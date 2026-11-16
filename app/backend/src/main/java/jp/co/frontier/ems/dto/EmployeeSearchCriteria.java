package jp.co.frontier.ems.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import jp.co.frontier.ems.entity.EmployeeStatus;
import org.springframework.web.bind.annotation.BindParam;

/**
 * 社員検索の条件（API 2.1 のクエリパラメータ）。
 *
 * <p>複数指定できる項目はカンマ区切りで受け取る（例 {@code skill_id=12,15,20}）。
 * Spring が {@code List<Long>} へ分解するので、こちら側では何もしなくてよい。
 *
 * <p><b>{@code @BindParam} でクエリ名を明示している。</b>
 * {@code spring.jackson.property-naming-strategy} はリクエストボディ（JSON）の
 * 変換にしか効かず、クエリパラメータの束縛は Jackson を通らないため、
 * 付けないと {@code department_id} が {@code departmentId} に対応づかず
 * <b>条件が黙って無視される</b>。
 *
 * <p>すべて任意。未指定の条件は絞り込みに使わない。
 *
 * @param keyword          氏名・氏名カナ・趣味・自己PRへの部分一致
 * @param skillMatch       skill_id を複数指定したときの結合方法
 * @param status           既定は在籍のみ。0（退職者）の指定は ADMIN のみ
 * @param sort             並び順。先頭に {@code -} を付けると降順（例 {@code -hire_date}）
 */
public record EmployeeSearchCriteria(
        String keyword,

        @BindParam("department_id")
        List<Long> departmentId,

        @BindParam("position_id")
        List<Long> positionId,

        @BindParam("site_id")
        List<Long> siteId,

        @BindParam("floor_id")
        List<Long> floorId,

        @BindParam("skill_id")
        List<Long> skillId,

        @BindParam("skill_match")
        MatchMode skillMatch,

        @BindParam("certification_id")
        List<Long> certificationId,

        @BindParam("certification_match")
        MatchMode certificationMatch,

        @BindParam("project_id")
        List<Long> projectId,

        @BindParam("employment_type_id")
        List<Long> employmentTypeId,

        EmployeeStatus status,

        @Min(value = 1, message = "pageは1以上で指定してください")
        Integer page,

        @BindParam("per_page")
        @Min(value = 1, message = "per_pageは1以上で指定してください")
        @Max(value = 100, message = "per_pageは100以下で指定してください")
        Integer perPage,

        String sort) {

    /** 複数指定した条件の結合方法。 */
    public enum MatchMode {
        /** すべて満たす（既定） */
        AND,
        /** いずれかを満たす */
        OR
    }

    /** ページネーションの既定値（仕様書「ページネーション共通仕様」）。 */
    public static final int DEFAULT_PAGE = 1;
    public static final int DEFAULT_PER_PAGE = 20;

    public int pageOrDefault() {
        return page != null ? page : DEFAULT_PAGE;
    }

    public int perPageOrDefault() {
        return perPage != null ? perPage : DEFAULT_PER_PAGE;
    }

    public MatchMode skillMatchOrDefault() {
        return skillMatch != null ? skillMatch : MatchMode.AND;
    }

    public MatchMode certificationMatchOrDefault() {
        return certificationMatch != null ? certificationMatch : MatchMode.AND;
    }

    /** 既定は在籍のみ（仕様書 2.1）。 */
    public EmployeeStatus statusOrDefault() {
        return status != null ? status : EmployeeStatus.ACTIVE;
    }
}
