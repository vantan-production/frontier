package jp.co.frontier.ems.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 変更申請の状態。change_requests.status（SMALLINT）に対応する。
 */
public enum ChangeRequestStatus {

    /** 0:申請中 */
    PENDING((short) 0),

    /** 1:承認済み */
    APPROVED((short) 1),

    /** 2:却下 */
    REJECTED((short) 2);

    private final short code;

    ChangeRequestStatus(short code) {
        this.code = code;
    }

    /**
     * JSONでは区分値そのもの（数値）として表現する。
     * API設計仕様書が change_requests.status を数値で返す形になっているため。
     */
    @JsonValue
    public short getCode() {
        return code;
    }

    @JsonCreator
    public static ChangeRequestStatus fromCode(short code) {
        for (ChangeRequestStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("change_requests.status に未定義の値: " + code);
    }

    /** 処理済み（承認または却下）か。 */
    public boolean isProcessed() {
        return this != PENDING;
    }
}
