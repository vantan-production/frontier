package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.EmployeeSkill;
import jp.co.frontier.ems.entity.EmployeeSkillId;
import org.springframework.data.jpa.repository.JpaRepository;

/** 社員とスキルの紐付け。API 6.4 / 6.5 */
public interface EmployeeSkillRepository extends JpaRepository<EmployeeSkill, EmployeeSkillId> {
}
