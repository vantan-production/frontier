package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 社員への紐付け（スキル・資格・プロジェクト）のリクエスト（API 6.4〜6.9）。
 *
 * <p>いずれも小さなレコードなので1ファイルにまとめている。
 */
public final class EmployeeLinkRequests {

    private EmployeeLinkRequests() {
    }

    /**
     * スキルの紐付け追加（API 6.4）。
     *
     * @param proficiency     1:初級 / 2:中級 / 3:上級
     * @param experienceYears 経験年数。DBが NUMERIC(4,1) なので最大 999.9
     */
    public record SkillLink(
            @NotNull(message = "スキルを選択してください")
            Long skillId,

            @NotNull(message = "習熟度を入力してください")
            @Min(value = 1, message = "習熟度は1〜3で指定してください")
            @Max(value = 3, message = "習熟度は1〜3で指定してください")
            Short proficiency,

            @DecimalMin(value = "0.0", message = "経験年数は0以上で入力してください")
            @DecimalMax(value = "999.9", message = "経験年数は999.9以下で入力してください")
            BigDecimal experienceYears) {
    }

    /** スキルの紐付け更新（API 6.5）。習熟度と経験年数のみ変更できる。 */
    public record SkillLinkUpdate(
            @NotNull(message = "習熟度を入力してください")
            @Min(value = 1, message = "習熟度は1〜3で指定してください")
            @Max(value = 3, message = "習熟度は1〜3で指定してください")
            Short proficiency,

            @DecimalMin(value = "0.0", message = "経験年数は0以上で入力してください")
            @DecimalMax(value = "999.9", message = "経験年数は999.9以下で入力してください")
            BigDecimal experienceYears) {
    }

    /**
     * 資格の紐付け追加（API 6.6）。
     *
     * <p>取得日に未来日は入れられない（仕様書 6.6 の VALIDATION_ERROR）。
     */
    public record CertificationLink(
            @NotNull(message = "資格を選択してください")
            Long certificationId,

            @NotNull(message = "取得日を入力してください")
            @PastOrPresent(message = "取得日に未来の日付は指定できません")
            LocalDate acquireDate) {
    }

    /**
     * 担当プロジェクトの紐付け追加（API 6.8）。
     *
     * @param endDate 離任日。継続中は null
     */
    public record ProjectLink(
            @NotNull(message = "プロジェクトを選択してください")
            Long projectId,

            @Size(max = 100, message = "担当役割は100文字以内で入力してください")
            String roleInProject,

            LocalDate startDate,

            LocalDate endDate) {
    }
}
