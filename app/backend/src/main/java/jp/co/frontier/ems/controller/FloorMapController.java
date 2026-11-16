package jp.co.frontier.ems.controller;

import java.util.List;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.dto.FloorResponse;
import jp.co.frontier.ems.dto.FloorSeatsResponse;
import jp.co.frontier.ems.dto.SiteResponse;
import jp.co.frontier.ems.service.FloorMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * フロアマップ・座席図の参照。API 4.1〜4.3 に対応する。
 *
 * <p>社員の座席取得（API 4.4）は {@code /employees/{employee_id}/seat} なので
 * {@link EmployeeController} 側に置いている。
 */
@RestController
@RequestMapping(ApiPaths.V1)
@RequiredArgsConstructor
public class FloorMapController {

    private final FloorMapService floorMapService;

    /** GET /v1/sites — 拠点一覧（API 4.1）。 */
    @GetMapping("/sites")
    public ApiResponse<List<SiteResponse>> sites() {
        return ApiResponse.of(floorMapService.findSites());
    }

    /** GET /v1/sites/{site_id}/floors — 拠点内のフロア一覧（API 4.2）。階数順。 */
    @GetMapping("/sites/{site_id}/floors")
    public ApiResponse<List<FloorResponse>> floors(@PathVariable("site_id") Long siteId) {
        return ApiResponse.of(floorMapService.findFloorsBySite(siteId));
    }

    /**
     * GET /v1/floors/{floor_id}/seats — フロア内の座席一覧（API 4.3）。
     *
     * <p>空席は {@code employee} が null になる。
     */
    @GetMapping("/floors/{floor_id}/seats")
    public ApiResponse<FloorSeatsResponse> seats(@PathVariable("floor_id") Long floorId) {
        return ApiResponse.of(floorMapService.findSeatsByFloor(floorId));
    }
}
