package jp.co.frontier.ems.common.error;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 例外を共通のエラーレスポンス形式に変換する。
 *
 * <p>すべてのエラーを一箇所に集めているので、コントローラ側では
 * ステータスやJSONの組み立てを書かなくてよい。
 *
 * <p>{@link ResponseEntityExceptionHandler} を継承しているのは、Spring MVC が
 * 投げるHTTPレベルの例外（405 メソッド不許可、415 未対応の Content-Type、
 * 400 不正なリクエストなど）を取りこぼさないため。これらを自分で1つずつ
 * {@code @ExceptionHandler} に列挙すると、書き忘れた型が最後の
 * {@code Exception} ハンドラに流れて <b>本来4xxのものが500として報告される</b>。
 * 基底クラスが対象の例外を網羅しており、変換は
 * {@link #handleExceptionInternal} の1箇所で行える。
 *
 * <p><b>注意:</b> 基底クラスが扱う例外（{@code NoResourceFoundException}、
 * {@code HttpRequestMethodNotSupportedException} など）に対して、このクラスで
 * {@code @ExceptionHandler} を重ねて書くと「Ambiguous @ExceptionHandler method」
 * で<b>起動に失敗する</b>。個別に扱いたい場合は基底クラスの
 * {@code handleXxx} メソッドを override すること。
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** 業務上の想定内エラー（サービス層が投げる）。 */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException e) {
        ErrorCode code = e.getErrorCode();
        // 想定内なのでスタックトレースは出さない。ログが埋まるのを避ける
        log.info("業務エラー: code={} message={}", code.name(), e.getMessage());
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, e.getMessage(), e.getDetails()));
    }

    /**
     * リクエストボディのバリデーション違反（{@code @Valid} が検出）。
     *
     * <p>項目ごとの理由を details に詰めて返すため、基底クラスの実装を上書きする。
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {

        List<ErrorResponse.ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ErrorCode code = ErrorCode.VALIDATION_ERROR;
        log.info("入力値エラー: {}", details);
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, code.getDefaultMessage(), details));
    }

    /**
     * Spring MVC の標準例外すべてが最後に通る変換点。
     *
     * <p>ステータスは基底クラスが決めたものを尊重し、本文だけ共通形式に差し替える。
     * 仕様書のコード表に対応するコードがあればそれを使い、無ければ
     * HTTPステータス名を code として返す（405、415 など）。
     */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode statusCode, WebRequest request) {

        ErrorCode mapped = switch (statusCode.value()) {
            case 400 -> ErrorCode.VALIDATION_ERROR;
            case 401 -> ErrorCode.UNAUTHORIZED;
            case 403 -> ErrorCode.FORBIDDEN;
            case 404 -> ErrorCode.NOT_FOUND;
            case 409 -> ErrorCode.CONFLICT;
            case 422 -> ErrorCode.VALIDATION_ERROR;
            case 500 -> ErrorCode.INTERNAL_SERVER_ERROR;
            default -> null;
        };

        Object newBody;
        if (mapped != null) {
            log.info("MVCエラー: status={} -> code={}", statusCode.value(), mapped.name());
            newBody = ErrorResponse.of(mapped, mapped.getDefaultMessage(), null);
        } else {
            String rawCode = HttpStatus.valueOf(statusCode.value()).name();
            log.info("MVCエラー: status={} code={}", statusCode.value(), rawCode);
            newBody = ErrorResponse.ofRawCode(rawCode, ex.getMessage());
        }

        return super.handleExceptionInternal(ex, newBody, headers, statusCode, request);
    }

    /**
     * 想定外の例外。
     *
     * <p>内部の詳細はクライアントに返さず、ログにのみ残す
     * （例外メッセージにテーブル名やSQLが載ることがあるため）。
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        ErrorCode code = ErrorCode.INTERNAL_SERVER_ERROR;
        log.error("想定外のエラー", e);
        return ResponseEntity.status(code.getStatus())
                .body(ErrorResponse.of(code, code.getDefaultMessage(), null));
    }
}
