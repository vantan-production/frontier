package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * マスタの登録・更新のリクエスト（API 7.1〜7.9）。
 *
 * <p>マスタごとに1つずつファイルを作ると9ファイル並ぶため、まとめている。
 * 文字数の上限はいずれもDBの定義に合わせている。
 *
 * <p>登録（POST）と更新（PUT）で同じ形を使う。PUT は全項目の置換なので、
 * 送らなかった任意項目は null で上書きされる。
 */
public final class MasterUpsertRequests {

    private MasterUpsertRequests() {
    }

    /** 部署（API 7.1）。 */
    public record DepartmentUpsert(
            @NotBlank(message = "部署名を入力してください")
            @Size(max = 100, message = "部署名は100文字以内で入力してください")
            String departmentName) {
    }

    /**
     * 役職（API 7.2）。
     *
     * @param rank 序列。小さいほど上位。並べ替え用なので任意
     */
    public record PositionUpsert(
            @NotBlank(message = "役職名を入力してください")
            @Size(max = 100, message = "役職名は100文字以内で入力してください")
            String positionName,

            Short rank) {
    }

    /** 拠点（API 7.3）。 */
    public record SiteUpsert(
            @NotBlank(message = "拠点名を入力してください")
            @Size(max = 100, message = "拠点名は100文字以内で入力してください")
            String siteName,

            @Size(max = 255, message = "住所は255文字以内で入力してください")
            String address) {
    }

    /**
     * フロア（API 7.4）。
     *
     * @param floorNumber 階数。並べ替え用。地下は負数
     */
    public record FloorUpsert(
            @NotNull(message = "拠点を選択してください")
            Long siteId,

            @NotBlank(message = "フロア名を入力してください")
            @Size(max = 50, message = "フロア名は50文字以内で入力してください")
            String floorName,

            @NotNull(message = "階数を入力してください")
            Short floorNumber,

            @Size(max = 500, message = "マップ画像URLは500文字以内で入力してください")
            String mapImageUrl) {
    }

    /**
     * 座席（API 7.5）。
     *
     * @param positionX マップ画像上のX座標（px）
     * @param positionY マップ画像上のY座標（px）
     */
    public record SeatUpsert(
            @NotNull(message = "フロアを選択してください")
            Long floorId,

            @NotBlank(message = "座席番号を入力してください")
            @Size(max = 50, message = "座席番号は50文字以内で入力してください")
            String seatNumber,

            @NotNull(message = "X座標を入力してください")
            Integer positionX,

            @NotNull(message = "Y座標を入力してください")
            Integer positionY) {
    }

    /** 雇用形態（API 7.6）。 */
    public record EmploymentTypeUpsert(
            @NotBlank(message = "雇用形態名を入力してください")
            @Size(max = 50, message = "雇用形態名は50文字以内で入力してください")
            String employmentTypeName) {
    }

    /** スキル（API 7.7）。 */
    public record SkillUpsert(
            @NotBlank(message = "スキル名を入力してください")
            @Size(max = 100, message = "スキル名は100文字以内で入力してください")
            String skillName,

            @Size(max = 50, message = "分類は50文字以内で入力してください")
            String category) {
    }

    /** 資格（API 7.8）。 */
    public record CertificationUpsert(
            @NotBlank(message = "資格名を入力してください")
            @Size(max = 150, message = "資格名は150文字以内で入力してください")
            String certificationName) {
    }

    /**
     * プロジェクト（API 7.9）。
     *
     * @param endDate 終了日。継続中は null
     */
    public record ProjectUpsert(
            @NotBlank(message = "プロジェクト名を入力してください")
            @Size(max = 200, message = "プロジェクト名は200文字以内で入力してください")
            String projectName,

            LocalDate startDate,

            LocalDate endDate,

            String description) {
    }
}
