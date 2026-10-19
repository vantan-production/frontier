package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.EmployeeProject;
import jp.co.frontier.ems.entity.EmployeeProjectId;
import org.springframework.data.jpa.repository.JpaRepository;

/** 社員とプロジェクトの紐付け。API 6.8 / 6.9 */
public interface EmployeeProjectRepository extends JpaRepository<EmployeeProject, EmployeeProjectId> {
}
