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
 * 社員。システムの中心となるエンティティ。
 *
 * <p>物理削除はせず、退職時は {@code status} を {@link EmployeeStatus#RETIRED} に更新し、
 * あわせて {@code seat} を null にして座席を空ける（DB設計書 1章）。
 *
 * <p>{@code name} / {@code nameKana} には DB 側で {@code COLLATE "ja-x-icu"} が
 * 指定されているため、ORDER BY で日本語の並び順になる（決定事項5）。
 * Java 側で指定するものは無い。
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@NoArgsConstructor
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "employee_id")
    private Long employeeId;

    /** 社員番号（例 EMP00123）。アカウント作成時の本人確認に使う */
    @Column(name = "employee_code", nullable = false, length = 20, unique = true)
    private String employeeCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    /** 氏名カナ。かな検索・五十音順の並べ替えに使う */
    @Column(name = "name_kana", length = 100)
    private String nameKana;

    /**
     * メールアドレス。ログインIDとして使う。
     *
     * <p>ADMIN が社員登録時に設定し、本人は変更できない（決定事項1, 2）。
     * 機密項目なので、本人または ADMIN 以外にはレスポンスに含めない。
     */
    @Column(name = "email", nullable = false, length = 255, unique = true)
    private String email;

    /** 電話番号。機密項目 */
    @Column(name = "tel", length = 20)
    private String tel;

    /** BCrypt ハッシュ。アカウント未作成の間は null */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "hobby", length = 255)
    private String hobby;

    @Column(name = "self_pr", columnDefinition = "text")
    private String selfPr;

    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "department_id", nullable = false)
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "position_id")
    private Position position;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employment_type_id", nullable = false)
    private EmploymentType employmentType;

    /**
     * 割り当てられた座席。未着席の場合は null。
     *
     * <p>DB 側で UNIQUE 制約があるため 1座席に1人までが保証される。
     * JPA 上も 1対1 として表現している。
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", unique = true)
    private Seat seat;

    /** 在籍状態。EmployeeStatusConverter で SMALLINT と相互変換される */
    @Column(name = "status", nullable = false)
    private EmployeeStatus status = EmployeeStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role = Role.EMPLOYEE;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
