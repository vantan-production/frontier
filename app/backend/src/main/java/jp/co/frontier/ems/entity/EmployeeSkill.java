package jp.co.frontier.ems.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 社員とスキルの紐付け。
 *
 * <p>複合主キーのため、同一社員に同じスキルを二重登録できない
 * （API 6.4 の CONFLICT(409) は DB 制約で担保される）。
 *
 * <p>{@code @MapsId} により、ID の各フィールドは関連から自動で設定される。
 * employee / skill をセットすれば id は書かなくてよい。
 */
@Entity
@Table(name = "employee_skills")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeSkill {

    @EmbeddedId
    private EmployeeSkillId id = new EmployeeSkillId();

    @MapsId("employeeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @MapsId("skillId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    /** 習熟度。ProficiencyConverter で SMALLINT と相互変換される */
    @Column(name = "proficiency", nullable = false)
    private Proficiency proficiency;

    /** 経験年数（例 5.5）。最大 999.9 */
    @Column(name = "experience_years", precision = 4, scale = 1)
    private BigDecimal experienceYears;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
