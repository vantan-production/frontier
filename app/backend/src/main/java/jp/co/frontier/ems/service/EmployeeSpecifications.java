package jp.co.frontier.ems.service;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import jp.co.frontier.ems.dto.EmployeeSearchCriteria;
import jp.co.frontier.ems.dto.EmployeeSearchCriteria.MatchMode;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeCertification;
import jp.co.frontier.ems.entity.EmployeeProject;
import jp.co.frontier.ems.entity.EmployeeSkill;
import org.springframework.data.jpa.domain.Specification;

/**
 * 社員検索（API 2.1）の絞り込み条件を組み立てる。
 *
 * <p>任意指定のフィルタが10個近くあり、メソッド名による導出
 * （{@code findByDepartmentIdAndPositionId...}）では到達できないため
 * {@code Specification} で動的に組む。
 *
 * <p>スキル・資格・プロジェクトは中間テーブル経由の絞り込みで、
 * かつ AND / OR を切り替えられる必要がある。JOIN で書くと AND のときに
 * 同じ社員が複数行に増えて件数が狂うため、<b>いずれも EXISTS の副問い合わせ</b>
 * で表現している。
 */
final class EmployeeSpecifications {

    private EmployeeSpecifications() {
    }

    static Specification<Employee> from(EmployeeSearchCriteria criteria) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 在籍状態。既定は在籍のみ（仕様書 2.1）
            predicates.add(cb.equal(root.get("status"), criteria.statusOrDefault()));

            // 氏名・趣味・自己PRへの部分一致。
            // 800名規模なので LIKE で足りる見込み（DB設計書の未確定事項3）
            if (hasText(criteria.keyword())) {
                String pattern = "%" + criteria.keyword().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("nameKana")), pattern),
                        cb.like(cb.lower(root.get("hobby")), pattern),
                        cb.like(cb.lower(root.get("selfPr")), pattern)));
            }

            // 単純な外部キーでの絞り込み。複数指定は IN
            addIn(predicates, root.get("department").get("departmentId"), criteria.departmentId(), cb);
            addIn(predicates, root.get("position").get("positionId"), criteria.positionId(), cb);
            addIn(predicates, root.get("employmentType").get("employmentTypeId"),
                    criteria.employmentTypeId(), cb);

            // 勤務拠点・フロアは座席を経由する。未着席の社員は対象外になるので
            // INNER ではなく LEFT で結合し、条件側で絞る
            if (isNotEmpty(criteria.floorId())) {
                var seat = root.join("seat", JoinType.LEFT);
                predicates.add(seat.get("floor").get("floorId").in(criteria.floorId()));
            }
            if (isNotEmpty(criteria.siteId())) {
                var seat = root.join("seat", JoinType.LEFT);
                predicates.add(seat.get("floor").get("site").get("siteId").in(criteria.siteId()));
            }

            // 中間テーブル経由の絞り込み
            addLinkCondition(predicates, root, query, cb, criteria.skillId(),
                    criteria.skillMatchOrDefault(),
                    EmployeeSkill.class, "skill", "skillId");
            addLinkCondition(predicates, root, query, cb, criteria.certificationId(),
                    criteria.certificationMatchOrDefault(),
                    EmployeeCertification.class, "certification", "certificationId");
            // プロジェクトは仕様書に match の指定が無いため AND 固定
            addLinkCondition(predicates, root, query, cb, criteria.projectId(),
                    MatchMode.AND,
                    EmployeeProject.class, "project", "projectId");

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 中間テーブル経由の絞り込みを EXISTS で組む。
     *
     * <p>AND のときは指定IDごとに EXISTS を1つ作って全部を満たすことを要求する。
     * JOIN + IN では「いずれかを持つ」の意味になってしまい AND を表現できないため。
     *
     * <p>OR のときは IN を含む EXISTS 1つで足りる。
     */
    private static <L> void addLinkCondition(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Root<Employee> root,
            jakarta.persistence.criteria.CommonAbstractCriteria query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            List<Long> ids,
            MatchMode mode,
            Class<L> linkType,
            String masterAttribute,
            String masterIdAttribute) {

        if (!isNotEmpty(ids)) {
            return;
        }

        if (mode == MatchMode.OR) {
            predicates.add(cb.exists(
                    linkSubquery(root, query, cb, linkType, masterAttribute, masterIdAttribute, ids)));
            return;
        }

        for (Long id : ids) {
            predicates.add(cb.exists(
                    linkSubquery(root, query, cb, linkType, masterAttribute, masterIdAttribute,
                            List.of(id))));
        }
    }

    private static <L> Subquery<L> linkSubquery(
            jakarta.persistence.criteria.Root<Employee> root,
            jakarta.persistence.criteria.CommonAbstractCriteria query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Class<L> linkType,
            String masterAttribute,
            String masterIdAttribute,
            Collection<Long> ids) {

        Subquery<L> subquery = query.subquery(linkType);
        var link = subquery.from(linkType);
        return subquery.select(link)
                .where(cb.and(
                        cb.equal(link.get("employee").get("employeeId"), root.get("employeeId")),
                        link.get(masterAttribute).get(masterIdAttribute).in(ids)));
    }

    private static void addIn(
            List<Predicate> predicates,
            jakarta.persistence.criteria.Path<?> path,
            List<Long> ids,
            jakarta.persistence.criteria.CriteriaBuilder cb) {

        if (isNotEmpty(ids)) {
            predicates.add(path.in(ids));
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static boolean isNotEmpty(Collection<?> values) {
        return values != null && !values.isEmpty();
    }
}
