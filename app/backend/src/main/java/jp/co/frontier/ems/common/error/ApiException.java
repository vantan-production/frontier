package jp.co.frontier.ems.common.error;

import java.util.List;

/**
 * API のエラーレスポンスに変換される例外。
 *
 * <p>サービス層からこれを投げると、{@link GlobalExceptionHandler} が
 * 共通形式のJSONと適切なHTTPステータスに変換する。
 * コントローラ側でステータスを組み立てる必要はない。
 *
 * <pre>
 * throw ApiException.notFound("指定された社員が見つかりません");
 * </pre>
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final List<ErrorResponse.ErrorDetail> details;

    public ApiException(ErrorCode errorCode, String message, List<ErrorResponse.ErrorDetail> details) {
        super(message != null ? message : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = details;
    }

    public ApiException(ErrorCode errorCode, String message) {
        this(errorCode, message, null);
    }

    public ApiException(ErrorCode errorCode) {
        this(errorCode, null, null);
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public List<ErrorResponse.ErrorDetail> getDetails() {
        return details;
    }

    // ---- よく使うものは生成メソッドを用意しておく ----

    public static ApiException notFound(String message) {
        return new ApiException(ErrorCode.NOT_FOUND, message);
    }

    public static ApiException conflict(String message) {
        return new ApiException(ErrorCode.CONFLICT, message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(ErrorCode.FORBIDDEN, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(ErrorCode.UNAUTHORIZED, message);
    }

    public static ApiException validation(String message, List<ErrorResponse.ErrorDetail> details) {
        return new ApiException(ErrorCode.VALIDATION_ERROR, message, details);
    }
}
