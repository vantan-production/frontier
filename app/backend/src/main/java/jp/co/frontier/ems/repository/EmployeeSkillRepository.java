package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.EmployeeSkill;
import jp.co.frontier.ems.entity.EmployeeSkillId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 社員とスキルの紐付け。API 6.4 / 6.5 */
public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, EmployeeSkillId> {

    /**
     * 指定社員のスキル紐付けを全件取得する。
     *
     * <p>{@code @EntityGraph} でスキルマスタを同時に取得している。
     * 付けないと紐付け1件ごとにマスタへの問い合わせが飛ぶ（N+1）。
     */
    @EntityGraph(attributePaths = "skill")
    List<EmployeeSkill> findByEmployeeEmployeeId(Long employeeId);

    /**
     * 複数社員のスキル紐付けをまとめて取得する（API 2.1 の一覧用）。
     *
     * <p>社員ごとに {@code findByEmployeeEmployeeId} を呼ぶと件数ぶんクエリが
     * 飛ぶため（1ページ20件なら20回）、社員IDをまとめて渡して1クエリにする。
     */
    @EntityGraph(attributePaths = "skill")
    List<EmployeeSkill> findByEmployeeEmployeeIdIn(Collection<Long> employeeIds);
}
