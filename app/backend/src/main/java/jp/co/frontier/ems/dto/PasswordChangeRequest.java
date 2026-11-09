package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * パスワード変更のリクエスト（API 1.6）。
 *
 * <p>現在のパスワードの照合を必須にしている。トークンを盗まれた場合に、
 * 本人のパスワードを勝手に書き換えられないようにするため。
 *
 * <p>{@code newPassword} の強度条件は {@link AccountCreateRequest} と同じ。
 * <b>片方を変更したらもう片方も合わせること。</b>
 */
public record PasswordChangeRequest(
        @NotBlank(message = "現在のパスワードを入力してください")
        String currentPassword,

        @NotBlank(message = "新しいパスワードを入力してください")
        @Size(min = 8, max = 72, message = "パスワードは8文字以上72文字以下で入力してください")
        @Pattern(regexp = ".*[A-Za-z].*", message = "パスワードには英字を含めてください")
        @Pattern(regexp = ".*[0-9].*", message = "パスワードには数字を含めてください")
        String newPassword) {
}
