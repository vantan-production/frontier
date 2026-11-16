package jp.co.frontier.ems.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 基本情報の変更申請。
 *
 * <p>申請対象は常にログイン中の本人。{@code before_value} は改ざん防止のため
 * サーバ側で現在値を取得して記録する（DB設計書 6章）。
 *
 * <p>同じ社員・同じ項目で申請中のものは1件まで（部分UNIQUE制約）。
 * 処理済みは何件でも残せる。
 */
@Entity
@Table(name = "change_requests")
@Getter
@Setter
@NoArgsConstructor
public class ChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Long requestId;

    /** 申請対象（＝申請者本人） */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_employee_id", nullable = false)
    private Employee targetEmployee;

    /** 変更対象のカラム名（例 department_id） */
    @Column(name = "target_field", nullable = false, length = 50)
    private String targetField;

    /** 変更前の値。申請時にサーバが現在値を取得して記録する */
    @Column(name = "before_value", length = 255)
    private String beforeValue;

    @Column(name = "after_value", nullable = false, length = 255)
    private String afterValue;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "status", nullable = false)
    private ChangeRequestStatus status = ChangeRequestStatus.PENDING;

    @CreationTimestamp
    @Column(name = "requested_at", nullable = false, updatable = false)
    private OffsetDateTime requestedAt;

    /** 承認・却下したADMIN */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by")
    private Employee reviewedBy;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "review_comment", length = 500)
    private String reviewComment;
}
