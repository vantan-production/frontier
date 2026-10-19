package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

/** スキルマスタ。API 7.7 */
public interface SkillRepository extends JpaRepository<Skill, Long> {
}
