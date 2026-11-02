package jp.co.frontier.ems.dto;

import java.math.BigDecimal;
import jp.co.frontier.ems.entity.EmployeeSkill;
import jp.co.frontier.ems.entity.Proficiency;

/**
 * 社員が保有するスキル。
 *
 * <p>形は API 6.4 のレスポンス例に合わせている
 * （{@code {"skill_id":12,"skill_name":"Java","proficiency":3,"experience_years":5.5}}）。
 *
 * @param proficiency 1:初級 / 2:中級 / 3:上級。JSONでは数値になる
 */
public record EmployeeSkillResponse(
        Long skillId,
        String skillName,
        Proficiency proficiency,
        BigDecimal experienceYears) {

    public static EmployeeSkillResponse from(EmployeeSkill employeeSkill) {
        var skill = employeeSkill.getSkill();
        return new EmployeeSkillResponse(
                skill.getSkillId(),
                skill.getSkillName(),
                employeeSkill.getProficiency(),
                employeeSkill.getExperienceYears());
    }
}
