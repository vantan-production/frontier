package jp.co.frontier.ems.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * エラーレスポンスの共通形式。
 *
 * <pre>
 * {
 *   "error": {
 *     "code": "VALIDATION_ERROR",
 *     "message": "入力値に誤りがあります",
 *     "details": [ { "field": "email", "reason": "メールアドレスの形式が正しくありません" } ]
 *   }
 * }
 * </pre>
 *
 * <p>{@code details} は VALIDATION_ERROR のときのみ入る。
 * それ以外では null にしてキーごと省略する。
 */
public record ErrorResponse(Body error) {

    public static ErrorResponse of(ErrorCode code, String message, List<ErrorDetail> details) {
        return new ErrorResponse(new Body(code.name(), message, details));
    }

    /**
     * 仕様書のコード表に無いプロトコルレベルのエラー用
     * （405 METHOD_NOT_ALLOWED、415 UNSUPPORTED_MEDIA_TYPE など）。
     *
     * <p>業務エラーは必ず {@link ErrorCode} を使う。こちらは
     * HTTPステータス名をそのまま code に入れるための逃げ道。
     */
    public static ErrorResponse ofRawCode(String code, String message) {
        return new ErrorResponse(new Body(code, message, null));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Body(String code, String message, List<ErrorDetail> details) {
    }

    /**
     * 入力値エラーの明細。
     *
     * @param field  対象の項目名
     * @param reason 理由
     */
    public record ErrorDetail(String field, String reason) {
    }
}
