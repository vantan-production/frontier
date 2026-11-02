package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * アカウント作成のリクエスト（API 1.4）。
 *
 * <p>社員番号とメールアドレスの<b>両方</b>を社員マスタと照合して本人確認を行う。
 * メールアドレスはADMINが社員登録時に設定済みの値と一致する必要があり、
 * 本人が新規に決めることはできない（決定事項2）。このAPIで設定するのはパスワードのみ。
 */
public record AccountCreateRequest(
        @NotBlank(message = "社員番号を入力してください")
        String employeeCode,

        @NotBlank(message = "メールアドレスを入力してください")
        @Email(message = "メールアドレスの形式が正しくありません")
        String email,

        /*
         * パスワード強度（仕様書 1.4 の「パスワード強度不足」に対応）。
         * 8文字以上で英字と数字を両方含むこと、とした。
         * 社内システムで800名が使うため、記号必須までは求めない。
         */
        @NotBlank(message = "パスワードを入力してください")
        @Size(min = 8, max = 72, message = "パスワードは8文字以上72文字以下で入力してください")
        @Pattern(regexp = ".*[A-Za-z].*", message = "パスワードには英字を含めてください")
        @Pattern(regexp = ".*[0-9].*", message = "パスワードには数字を含めてください")
        String password) {
}
