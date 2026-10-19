package jp.co.frontier.ems.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@link EmployeeSkill} の複合主キー。
 *
 * <p>ID クラスは値オブジェクトなので equals / hashCode が必要。
 * 遅延ロードされる関連を持たないため、ここでは Lombok の
 * {@code @EqualsAndHashCode} を使って問題ない。
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EmployeeSkillId implements Serializable {

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "skill_id")
    private Long skillId;
}
