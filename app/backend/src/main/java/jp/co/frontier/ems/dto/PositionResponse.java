package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.Position;

/**
 * 役職の返却用DTO。社員詳細・社員一覧の {@code position}、
 * およびマスタ参照（API 7.2）で共用する。
 *
 * @param rank 序列（小さいほど上位）。画面側の並べ替えに使う
 */
public record PositionResponse(Long positionId, String positionName, Short rank) {

    /** null 安全。役職は未設定（null）を許す項目のため。 */
    public static PositionResponse from(Position position) {
        if (position == null) {
            return null;
        }
        return new PositionResponse(
                position.getPositionId(), position.getPositionName(), position.getRank());
    }
}
