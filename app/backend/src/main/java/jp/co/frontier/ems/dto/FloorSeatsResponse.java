package jp.co.frontier.ems.dto;

import java.util.List;
import jp.co.frontier.ems.entity.Floor;

/**
 * フロア内の座席一覧（API 4.3）。
 *
 * <p>フロアマップ画像の上に座席を並べるためのデータ。
 * 空席は {@code employee} が null になる。
 */
public record FloorSeatsResponse(FloorResponse floor, List<SeatOccupancy> seats) {

    /**
     * 座席と、そこに座っている社員。
     *
     * @param employee 在席者。空席なら null
     */
    public record SeatOccupancy(
            Long seatId,
            String seatNumber,
            Integer positionX,
            Integer positionY,
            EmployeeSummaryResponse employee) {
    }

    public static FloorSeatsResponse of(Floor floor, List<SeatOccupancy> seats) {
        return new FloorSeatsResponse(FloorResponse.from(floor), seats);
    }
}
