package jp.co.frontier.ems.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import jp.co.frontier.ems.entity.AuthToken;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeStatus;
import jp.co.frontier.ems.entity.TokenType;
import jp.co.frontier.ems.repository.AuthTokenRepository;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 認証トークンの発行・検証・失効。
 *
 * <p>API設計仕様書「認証トークンの仕様」に対応する opaque token 方式。
 * 乱数で生成した意味を持たない文字列をクライアントに渡し、サーバは
 * SHA-256 ハッシュのみを保存して照合する。平文は保存しない。
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    /** アクセストークンの有効期限（仕様書: 1時間 / expires_in 3600秒） */
    public static final Duration ACCESS_TOKEN_TTL = Duration.ofHours(1);

    /** リフレッシュトークンの有効期限（仕様書: 14日。ローテーションしない） */
    public static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(14);

    /** 乱数のバイト数（仕様書: 32バイト以上） */
    private static final int TOKEN_BYTES = 32;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AuthTokenRepository authTokenRepository;

    /**
     * 発行したトークンの平文と、保存されたレコード。
     *
     * <p>平文はこの瞬間だけ存在し、クライアントに返したあとは二度と取得できない。
     */
    public record IssuedToken(String rawToken, AuthToken entity) {
    }

    /** リフレッシュトークンを発行する。 */
    @Transactional
    public IssuedToken issueRefreshToken(Employee employee) {
        return issue(employee, TokenType.REFRESH, REFRESH_TOKEN_TTL, null);
    }

    /**
     * アクセストークンを発行する。
     *
     * @param parent 発行元のリフレッシュトークン。ログアウト時の連鎖失効に使う
     */
    @Transactional
    public IssuedToken issueAccessToken(Employee employee, AuthToken parent) {
        return issue(employee, TokenType.ACCESS, ACCESS_TOKEN_TTL, parent);
    }

    private IssuedToken issue(Employee employee, TokenType type, Duration ttl, AuthToken parent) {
        byte[] bytes = new byte[TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        AuthToken token = new AuthToken();
        token.setTokenHash(hash(raw));
        token.setEmployee(employee);
        token.setTokenType(type);
        token.setExpiresAt(OffsetDateTime.now().plus(ttl));
        token.setParentToken(parent);

        return new IssuedToken(raw, authTokenRepository.save(token));
    }

    /**
     * 平文トークンから有効なレコードを引く。
     *
     * <p>有効と見なす条件は、ハッシュ一致・種別一致・未失効・期限内・社員が在籍中。
     * 退職者（status=0）のトークンは残っていても無効扱いにする。
     */
    @Transactional(readOnly = true)
    public Optional<AuthToken> findValidToken(String rawToken, TokenType expectedType) {
        return authTokenRepository.findByTokenHash(hash(rawToken))
                .filter(t -> t.getTokenType() == expectedType)
                .filter(t -> t.getRevokedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(OffsetDateTime.now()))
                .filter(t -> t.getEmployee().getStatus() == EmployeeStatus.ACTIVE);
    }

    /**
     * アクセストークンを検証し、認証済み利用者を返す。
     *
     * <p>遅延ロードを発火させないよう、トランザクション内で必要な値だけ取り出して
     * 値オブジェクトに詰め替えて返す。
     */
    @Transactional(readOnly = true)
    public Optional<AuthenticatedEmployee> authenticate(String rawAccessToken) {
        return findValidToken(rawAccessToken, TokenType.ACCESS)
                .map(token -> {
                    Employee e = token.getEmployee();
                    return new AuthenticatedEmployee(
                            e.getEmployeeId(), e.getEmployeeCode(), e.getRole(), token.getTokenId());
                });
    }

    /**
     * アクセストークンと、それに紐づくリフレッシュトークンの両方を失効させる。
     *
     * <p>仕様書 1.3 のログアウト動作。同じリフレッシュトークンから発行された
     * 他のアクセストークンもまとめて失効させる（再発行で複数発行されている場合があるため）。
     */
    @Transactional
    public void revokeChain(Long accessTokenId) {
        OffsetDateTime now = OffsetDateTime.now();

        AuthToken access = authTokenRepository.findById(accessTokenId).orElse(null);
        if (access == null) {
            return;
        }
        access.setRevokedAt(now);

        AuthToken refresh = access.getParentToken();
        if (refresh != null) {
            refresh.setRevokedAt(now);
            // 同じリフレッシュから発行された兄弟のアクセストークンも失効させる
            List<AuthToken> siblings =
                    authTokenRepository.findByParentTokenTokenId(refresh.getTokenId());
            siblings.forEach(t -> t.setRevokedAt(now));
        }
    }

    /** SHA-256 ハッシュを16進64文字で返す（auth_tokens.token_hash は VARCHAR(64)）。 */
    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 は標準で必ず存在する
            throw new IllegalStateException("SHA-256 が利用できません", e);
        }
    }
}
