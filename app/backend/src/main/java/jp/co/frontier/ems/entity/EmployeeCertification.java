package jp.co.frontier.ems.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 社員と資格の紐付け。
 *
 * <p>複合主キーのため、同一資格の二重登録はできない（API 6.6 の CONFLICT(409)）。
 */
@Entity
@Table(name = "employee_certifications")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeCertification {

    @EmbeddedId
    private EmployeeCertificationId id = new EmployeeCertificationId();

    @MapsId("employeeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @MapsId("certificationId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "certification_id", nullable = false)
    private Certification certification;

    @Column(name = "acquire_date", nullable = false)
    private LocalDate acquireDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
