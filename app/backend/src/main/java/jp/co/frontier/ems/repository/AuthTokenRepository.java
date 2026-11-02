package jp.co.frontier.ems.repository;

import java.util.List;
import java.util.Optional;
import jp.co.frontier.ems.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;

/** 認証トークン。API 1. 認証 */
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    /**
     * ハッシュ値でトークンを引く。リクエストごとに呼ばれるため
     * token_hash にはインデックスを張っている。
     */
    Optional<AuthToken> findByTokenHash(String tokenHash);

    /**
     * 指定のリフレッシュトークンから発行されたアクセストークンを全件取得する。
     * ログアウト時の連鎖失効に使う。
     */
    List<AuthToken> findByParentTokenTokenId(Long parentTokenId);
}
