package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.Floor;

/**
 * フロアの返却用DTO。API 4.2 / 7.4。
 *
 * @param floorNumber 階数。並べ替え用。地下は負数
 */
public record FloorResponse(
        Long floorId, String floorName, Short floorNumber, String mapImageUrl) {

    public static FloorResponse from(Floor floor) {
        if (floor == null) {
            return null;
        }
        return new FloorResponse(
                floor.getFloorId(),
                floor.getFloorName(),
                floor.getFloorNumber(),
                floor.getMapImageUrl());
    }
}
