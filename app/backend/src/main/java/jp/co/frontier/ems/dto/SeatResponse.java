package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.Seat;

/**
 * 座席の返却用DTO。
 *
 * <p>フロア名と拠点名も含めている。社員詳細で「本社 3階 3F-A01」と
 * 表示したいとき、座席番号だけでは拠点が分からず、画面側から
 * フロアマップAPIを追加で呼ぶことになるため。
 *
 * @param positionX マップ画像上のX座標（px）
 * @param positionY マップ画像上のY座標（px）
 */
public record SeatResponse(
        Long seatId,
        String seatNumber,
        Long floorId,
        String floorName,
        Long siteId,
        String siteName,
        Integer positionX,
        Integer positionY) {

    /** null 安全。未着席の社員は座席を持たないため。 */
    public static SeatResponse from(Seat seat) {
        if (seat == null) {
            return null;
        }
        var floor = seat.getFloor();
        var site = floor.getSite();
        return new SeatResponse(
                seat.getSeatId(),
                seat.getSeatNumber(),
                floor.getFloorId(),
                floor.getFloorName(),
                site.getSiteId(),
                site.getSiteName(),
                seat.getPositionX(),
                seat.getPositionY());
    }
}
