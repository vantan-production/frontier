package jp.co.frontier.ems.common.error;

import org.springframework.http.HttpStatus;

/**
 * 共通エラーコード。API設計仕様書「共通エラーコード」の一覧に1対1で対応する。
 *
 * <p>HTTPステータスをここに持たせているのは、コードとステータスの対応を
 * 1箇所で管理するため。コントローラごとにステータスを書くとズレる。
 */
public enum ErrorCode {

    /** 入力値が不正。details に該当項目を入れる */
    VALIDATION_ERROR(HttpStatus.UNPROCESSABLE_ENTITY, "入力値に誤りがあります"),

    /** メールアドレス重複 */
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "このメールアドレスは既に登録されています"),

    /** 社員番号重複 */
    DUPLICATE_EMPLOYEE_CODE(HttpStatus.CONFLICT, "この社員番号は既に登録されています"),

    /** 状態の競合（参照中マスタの削除、スキル等の重複紐付け、座席の二重割当 など） */
    CONFLICT(HttpStatus.CONFLICT, "現在の状態では実行できません"),

    /** 未認証、またはトークン失効 */
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "認証が必要です"),

    /** 権限不足 */
    FORBIDDEN(HttpStatus.FORBIDDEN, "この操作を行う権限がありません"),

    /** 指定IDのリソースが存在しない */
    NOT_FOUND(HttpStatus.NOT_FOUND, "指定されたリソースが見つかりません"),

    /** インポートCSVの形式不正 */
    IMPORT_FILE_INVALID(HttpStatus.BAD_REQUEST, "CSVファイルの形式が正しくありません"),

    /** 一定時間内のリクエスト数が上限を超過 */
    RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "リクエストが多すぎます。時間をおいて再実行してください"),

    /** サーバ内部エラー */
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "サーバ内部エラーが発生しました");

    private final HttpStatus status;
    private final String defaultMessage;

    ErrorCode(HttpStatus status, String defaultMessage) {
        this.status = status;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getStatus() {
        return status;
    }

    /** 個別のメッセージを渡さなかった場合に使われる既定文 */
    public String getDefaultMessage() {
        return defaultMessage;
    }
}
