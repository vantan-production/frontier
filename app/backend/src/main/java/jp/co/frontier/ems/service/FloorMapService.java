package jp.co.frontier.ems.service;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.dto.EmployeeSummaryResponse;
import jp.co.frontier.ems.dto.FloorResponse;
import jp.co.frontier.ems.dto.FloorSeatsResponse;
import jp.co.frontier.ems.dto.SeatResponse;
import jp.co.frontier.ems.dto.SiteResponse;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.Floor;
import jp.co.frontier.ems.entity.Seat;
import jp.co.frontier.ems.repository.EmployeeRepository;
import jp.co.frontier.ems.repository.FloorRepository;
import jp.co.frontier.ems.repository.SeatRepository;
import jp.co.frontier.ems.repository.SiteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** フロアマップ・座席図の参照（API 4.1〜4.4）。 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FloorMapService {

    private final SiteRepository siteRepository;
    private final FloorRepository floorRepository;
    private final SeatRepository seatRepository;
    private final EmployeeRepository employeeRepository;

    /** 拠点一覧（API 4.1）。 */
    public List<SiteResponse> findSites() {
        return siteRepository.findAll(Sort.by("siteId")).stream()
                .map(SiteResponse::from).toList();
    }

    /** 拠点内のフロア一覧（API 4.2）。拠点が無ければ404。 */
    public List<FloorResponse> findFloorsBySite(Long siteId) {
        if (!siteRepository.existsById(siteId)) {
            throw ApiException.notFound("指定された拠点が見つかりません");
        }
        return floorRepository.findBySiteSiteIdOrderByFloorNumberAsc(siteId).stream()
                .map(FloorResponse::from).toList();
    }

    /**
     * フロア内の座席一覧（API 4.3）。空席は {@code employee} が null。
     *
     * <p>在席者は座席IDをまとめて1クエリで引く。座席ごとに問い合わせると
     * 座席数ぶんクエリが飛ぶため。
     */
    public FloorSeatsResponse findSeatsByFloor(Long floorId) {
        Floor floor = floorRepository.findById(floorId)
                .orElseThrow(() -> ApiException.notFound("指定されたフロアが見つかりません"));

        List<Seat> seats = seatRepository.findByFloorFloorIdOrderBySeatNumberAsc(floorId);

        Map<Long, Employee> 在席者 = seats.isEmpty()
                ? Map.of()
                : employeeRepository.findBySeatSeatIdIn(
                                seats.stream().map(Seat::getSeatId).toList())
                        .stream()
                        .collect(Collectors.toMap(e -> e.getSeat().getSeatId(), Function.identity()));

        List<FloorSeatsResponse.SeatOccupancy> 座席 = seats.stream()
                .map(seat -> new FloorSeatsResponse.SeatOccupancy(
                        seat.getSeatId(),
                        seat.getSeatNumber(),
                        seat.getPositionX(),
                        seat.getPositionY(),
                        EmployeeSummaryResponse.from(在席者.get(seat.getSeatId()))))
                .toList();

        return FloorSeatsResponse.of(floor, 座席);
    }

    /**
     * 社員の座席取得（API 4.4）。
     *
     * <p>座席未登録の社員は 200 で {@code null} を返す（仕様書 4.4）。
     * 404 を返すのは社員自体が存在しない場合のみ。
     */
    public SeatResponse findSeatByEmployee(Long employeeId) {
        Employee employee = employeeRepository.findWithMastersByEmployeeId(employeeId)
                .orElseThrow(() -> ApiException.notFound("指定された社員が見つかりません"));

        return SeatResponse.from(employee.getSeat());
    }
}
