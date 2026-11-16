package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.Seat;

/**
 * 座席とその所在。社員詳細（API 3.1）と社員の座席取得（API 4.4）で共用する。
 *
 * <p>フロアと拠点を入れ子で持つ形は仕様書 4.4 に明示されているもの。
 * 社員詳細側でも同じ形を使い、「座席の所在」の表現を1つに揃えている。
 * 座席番号だけでは拠点が分からず、画面側がフロアマップAPIを追加で
 * 呼ぶことになるため、所在をここに含めている。
 *
 * @param positionX マップ画像上のX座標（px）
 * @param positionY マップ画像上のY座標（px）
 */
public record SeatResponse(
        Long seatId,
        String seatNumber,
        Integer positionX,
        Integer positionY,
        FloorResponse floor,
        SiteResponse site) {

    /** null 安全。未着席の社員は座席を持たないため。 */
    public static SeatResponse from(Seat seat) {
        if (seat == null) {
            return null;
        }
        var floor = seat.getFloor();
        return new SeatResponse(
                seat.getSeatId(),
                seat.getSeatNumber(),
                seat.getPositionX(),
                seat.getPositionY(),
                FloorResponse.from(floor),
                SiteResponse.from(floor.getSite()));
    }
}
