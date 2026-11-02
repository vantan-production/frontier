package jp.co.frontier.ems.entity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * スキルの習熟度。employee_skills.proficiency（SMALLINT）に対応する。
 */
public enum Proficiency {

    /** 1:初級 */
    BEGINNER((short) 1),

    /** 2:中級 */
    INTERMEDIATE((short) 2),

    /** 3:上級 */
    ADVANCED((short) 3);

    private final short code;

    Proficiency(short code) {
        this.code = code;
    }

    /**
     * JSONでは区分値そのもの（数値）として表現する。
     * API設計仕様書が employee_skills.proficiency を数値で返す形になっているため。
     * 付けないと enum 名（"ACTIVE" など）で出力されてしまう。
     */
    @JsonValue
    public short getCode() {
        return code;
    }

    @JsonCreator
    public static Proficiency fromCode(short code) {
        for (Proficiency value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("employee_skills.proficiency に未定義の値: " + code);
    }
}
