package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * ログインのリクエスト（API 1.1）。
 *
 * <p>ログインIDはメールアドレス（決定事項1）。
 */
public record LoginRequest(
        @NotBlank(message = "メールアドレスを入力してください")
        @Email(message = "メールアドレスの形式が正しくありません")
        String email,

        @NotBlank(message = "パスワードを入力してください")
        String password) {
}
