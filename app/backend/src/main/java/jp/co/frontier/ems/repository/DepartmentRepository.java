package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

/** 部署マスタ。API 7.1 */
public interface DepartmentRepository extends JpaRepository<Department, Long> {
}
