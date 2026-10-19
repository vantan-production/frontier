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
 * 社員と担当プロジェクトの紐付け。
 *
 * <p>{@code endDate} が null なら継続中。
 */
@Entity
@Table(name = "employee_projects")
@Getter
@Setter
@NoArgsConstructor
public class EmployeeProject {

    @EmbeddedId
    private EmployeeProjectId id = new EmployeeProjectId();

    @MapsId("employeeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @MapsId("projectId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /** 担当役割（フロントエンド担当 など） */
    @Column(name = "role_in_project", length = 100)
    private String roleInProject;

    @Column(name = "start_date")
    private LocalDate startDate;

    /** 離任日。継続中は null */
    @Column(name = "end_date")
    private LocalDate endDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
