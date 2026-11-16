package jp.co.frontier.ems.dto;

import jp.co.frontier.ems.entity.Skill;

/** スキルマスタの返却用DTO。API 7.7。 */
public record SkillResponse(Long skillId, String skillName, String category) {

    public static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.getSkillId(), skill.getSkillName(), skill.getCategory());
    }
}
