package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import jp.co.frontier.ems.entity.ThanksMessage;

/** サンクスメッセージの入出力（API 8.1〜8.3）。 */
public final class ThanksMessageDtos {

    private ThanksMessageDtos() {
    }

    /**
     * 送信のリクエスト（API 8.1）。
     *
     * <p>{@code from_employee_id} は受け取らない。トークンから判定する。
     * クライアントに送信者を指定させると、他人になりすませてしまう。
     */
    public record SendRequest(
            @NotNull(message = "宛先を選択してください")
            Long toEmployeeId,

            @NotBlank(message = "本文を入力してください")
            @Size(max = 300, message = "本文は300文字以内で入力してください")
            String body) {
    }

    /**
     * メッセージ1件（API 8.1〜8.3 のレスポンス）。
     *
     * <p>送信者・受信者は要約（{@link EmployeeSummaryResponse}）で返す。
     * タイムラインに出すものなので機密項目は含まない。
     */
    public record Response(
            Long messageId,
            EmployeeSummaryResponse fromEmployee,
            EmployeeSummaryResponse toEmployee,
            String body,
            OffsetDateTime sentAt) {

        public static Response from(ThanksMessage message) {
            return new Response(
                    message.getMessageId(),
                    EmployeeSummaryResponse.from(message.getFromEmployee()),
                    EmployeeSummaryResponse.from(message.getToEmployee()),
                    message.getBody(),
                    message.getSentAt());
        }
    }

    /** 社員別履歴の取得範囲（API 8.3 の {@code type}）。 */
    public enum HistoryType {
        /** 送信分のみ */
        SENT,
        /** 受信分のみ */
        RECEIVED,
        /** 両方（既定） */
        ALL;

        public boolean includesSent() {
            return this == SENT || this == ALL;
        }

        public boolean includesReceived() {
            return this == RECEIVED || this == ALL;
        }
    }
}
