package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.EmployeeProject;
import jp.co.frontier.ems.entity.EmployeeProjectId;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 社員とプロジェクトの紐付け。API 6.8 / 6.9 */
public interface EmployeeProjectRepository extends JpaRepository<EmployeeProject, EmployeeProjectId> {

    /**
     * 指定社員のプロジェクト紐付けを全件取得する。
     *
     * <p>{@code @EntityGraph} でプロジェクトマスタを同時に取得している。
     * 付けないと紐付け1件ごとにマスタへの問い合わせが飛ぶ（N+1）。
     */
    @EntityGraph(attributePaths = "project")
    List<EmployeeProject> findByEmployeeEmployeeId(Long employeeId);
}
