package jp.co.frontier.ems.entity;

/**
 * 社員の在籍状態。employees.status（SMALLINT）に対応する。
 *
 * <p>社員は物理削除せず、退職時は {@link #RETIRED} に更新する（論理削除）。
 * 検索の既定は {@link #ACTIVE} のみ。
 */
public enum EmployeeStatus {

    /** 0:退職・無効 */
    RETIRED((short) 0),

    /** 1:在籍 */
    ACTIVE((short) 1);

    private final short code;

    EmployeeStatus(short code) {
        this.code = code;
    }

    public short getCode() {
        return code;
    }

    public static EmployeeStatus fromCode(short code) {
        for (EmployeeStatus value : values()) {
            if (value.code == code) {
                return value;
            }
        }
        throw new IllegalArgumentException("employees.status に未定義の値: " + code);
    }
}
