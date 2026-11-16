package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * 社員の登録・更新のリクエスト（API 6.1 / 6.2）。
 *
 * <p>登録（POST）と更新（PUT）で同じ項目を受け取る。PUT は全項目の置換なので、
 * <b>送らなかった項目は null で上書きされる</b>（エンドポイント設計書 2章）。
 *
 * <p>パスワードはここでは設定しない。本人がアカウント作成（API 1.4）で設定する。
 * {@code image_url} も含めない。顔写真はアップロードAPI（5.2）経由でのみ
 * サーバが払い出す。
 *
 * @param email  ログインIDになる。ADMIN が設定し、本人は変更できない（決定事項2）
 * @param seatId 座席。未着席なら null。他社員が使用中の座席は 409
 */
public record EmployeeUpsertRequest(
        @NotBlank(message = "社員番号を入力してください")
        @Size(max = 20, message = "社員番号は20文字以内で入力してください")
        String employeeCode,

        @NotBlank(message = "氏名を入力してください")
        @Size(max = 100, message = "氏名は100文字以内で入力してください")
        String name,

        @Size(max = 100, message = "氏名カナは100文字以内で入力してください")
        String nameKana,

        @NotBlank(message = "メールアドレスを入力してください")
        @Email(message = "メールアドレスの形式が正しくありません")
        @Size(max = 255, message = "メールアドレスは255文字以内で入力してください")
        String email,

        @Size(max = 20, message = "電話番号は20文字以内で入力してください")
        String tel,

        @NotNull(message = "入社日を入力してください")
        LocalDate hireDate,

        @NotNull(message = "部署を選択してください")
        Long departmentId,

        Long positionId,

        @NotNull(message = "雇用形態を選択してください")
        Long employmentTypeId,

        Long seatId) {
}
