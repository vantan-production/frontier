package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.NotBlank;

/** アクセストークン再発行のリクエスト（API 1.2）。 */
public record RefreshRequest(
        @NotBlank(message = "リフレッシュトークンを指定してください")
        String refreshToken) {
}
