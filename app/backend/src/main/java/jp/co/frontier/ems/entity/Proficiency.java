package jp.co.frontier.ems.entity;

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

    public short getCode() {
        return code;
    }

    public static Proficiency fromCode(short code) {
        for (Proficiency value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("employee_skills.proficiency に未定義の値: " + code);
    }
}
