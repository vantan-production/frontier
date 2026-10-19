package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.EmploymentType;
import org.springframework.data.jpa.repository.JpaRepository;

/** 雇用形態マスタ。API 7.6 */
public interface EmploymentTypeRepository extends JpaRepository<EmploymentType, Long> {
}
