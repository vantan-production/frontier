package jp.co.frontier.ems.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

/**
 * 認証トークン。opaque token 方式のため、発行したトークンをここで管理する。
 *
 * <p>有効性の判定条件は {@code revokedAt == null && expiresAt > now()}。
 *
 * <p>平文のトークンは保存せず、SHA-256 ハッシュのみを保存する。
 * 照合時はリクエストのトークンをハッシュ化して検索する（DBが漏れても
 * なりすまされないようにするため）。
 */
@Entity
@Table(name = "auth_tokens")
@Getter
@Setter
@NoArgsConstructor
public class AuthToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_id")
    private Long tokenId;

    /** トークンの SHA-256 ハッシュ（16進64文字） */
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_type", nullable = false, length = 10)
    private TokenType tokenType;

    /** 有効期限（ACCESS:1時間後 / REFRESH:14日後） */
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** 失効日時。null なら有効 */
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    /**
     * ACCESS の場合、発行元の REFRESH トークン。
     * ログアウト時の連鎖失効に使う。
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_token_id")
    private AuthToken parentToken;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
