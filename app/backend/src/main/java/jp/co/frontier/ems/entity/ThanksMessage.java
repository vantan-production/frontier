package jp.co.frontier.ems.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * サンクスメッセージ。
 *
 * <p>編集・削除しない想定のため {@code updated_at} を持たない（DB設計書 5章）。
 *
 * <p>自分宛ての送信はDB側の CHECK 制約でも防がれている。
 */
@Entity
@Table(name = "thanks_messages")
@Getter
@Setter
@NoArgsConstructor
public class ThanksMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_employee_id", nullable = false)
    private Employee fromEmployee;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_employee_id", nullable = false)
    private Employee toEmployee;

    /** 本文（1〜300文字） */
    @Column(name = "body", nullable = false, length = 300)
    private String body;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private OffsetDateTime sentAt;
}
