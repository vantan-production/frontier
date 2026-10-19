package jp.co.frontier.ems.entity;

/**
 * 権限。employees.role（VARCHAR）に対応する。
 *
 * <p>前期スコープではこの2値のみで権限制御を行う（DB設計書 決定事項3）。
 * {@link #ADMIN} は人事部の全員と社長に付与する。
 */
public enum Role {

    /** 一般社員 */
    EMPLOYEE,

    /** 人事・管理者。社員CRUD、マスタ管理、申請承認、機密項目の閲覧が可能 */
    ADMIN
}
