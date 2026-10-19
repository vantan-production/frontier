package jp.co.frontier.ems.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** {@link EmployeeCertification} の複合主キー。 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EmployeeCertificationId implements Serializable {

    @Column(name = "employee_id")
    private Long employeeId;

    @Column(name = "certification_id")
    private Long certificationId;
}
