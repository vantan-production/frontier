package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import jp.co.frontier.ems.entity.ChangeRequest;
import jp.co.frontier.ems.entity.ChangeRequestStatus;
import org.springframework.web.bind.annotation.BindParam;

/** 基本情報変更申請の入出力（API 9.1〜9.4）。 */
public final class ChangeRequestDtos {

    private ChangeRequestDtos() {
    }

    /**
     * 申請の作成（API 9.1）。
     *
     * <p>申請対象はログイン中の本人に固定するため受け取らない。
     * {@code before_value} も受け取らない。改ざん防止のためサーバが現在値を記録する。
     *
     * @param targetField 変更したい項目。変更申請を通せる項目のみ指定できる
     */
    public record CreateRequest(
            @NotBlank(message = "変更したい項目を指定してください")
            @Size(max = 50, message = "項目名は50文字以内で指定してください")
            String targetField,

            @NotBlank(message = "変更後の値を入力してください")
            @Size(max = 255, message = "変更後の値は255文字以内で入力してください")
            String afterValue,

            @Size(max = 500, message = "申請理由は500文字以内で入力してください")
            String reason) {
    }

    /**
     * 承認・却下（API 9.3）。
     *
     * @param status 1:承認 / 2:却下。0（申請中）に戻すことはできない
     */
    public record ReviewRequest(
            @NotNull(message = "承認または却下を指定してください")
            ChangeRequestStatus status,

            @Size(max = 500, message = "コメントは500文字以内で入力してください")
            String comment) {
    }

    /** 一覧取得時の絞り込み（API 9.2）。 */
    public record SearchParams(
            ChangeRequestStatus status,

            @BindParam("page")
            Integer page,

            @BindParam("per_page")
            Integer perPage) {
    }

    /**
     * 申請1件（API 9.1〜9.3 のレスポンス）。
     *
     * @param status 0:申請中 / 1:承認 / 2:却下。JSONでは数値になる
     */
    public record Response(
            Long requestId,
            EmployeeSummaryResponse targetEmployee,
            String targetField,
            String beforeValue,
            String afterValue,
            String reason,
            ChangeRequestStatus status,
            OffsetDateTime requestedAt,
            EmployeeSummaryResponse reviewedBy,
            OffsetDateTime reviewedAt,
            String reviewComment) {

        public static Response from(ChangeRequest request) {
            return new Response(
                    request.getRequestId(),
                    EmployeeSummaryResponse.from(request.getTargetEmployee()),
                    request.getTargetField(),
                    request.getBeforeValue(),
                    request.getAfterValue(),
                    request.getReason(),
                    request.getStatus(),
                    request.getRequestedAt(),
                    EmployeeSummaryResponse.from(request.getReviewedBy()),
                    request.getReviewedAt(),
                    request.getReviewComment());
        }
    }
}
