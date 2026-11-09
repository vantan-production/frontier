package jp.co.frontier.ems.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import jp.co.frontier.ems.entity.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /**
     * 期限切れのトークンを物理削除する（DB設計書 4章の運用に対応）。
     *
     * <p>失効（{@code revoked_at}）はレコードを残すため、掃除しないと
     * ログインのたびに2行、トークン再発行ごとに1行ずつ増え続ける。
     *
     * <p>1件ずつ削除する派生クエリではなく一括の DELETE にしている。
     * 件数が多くなる処理なので、1行ごとにエンティティを読み込ませたくないため。
     *
     * @param threshold この時刻より前に期限切れになったものを削除する
     * @return 削除件数
     */
    @Modifying
    @Query("delete from AuthToken t where t.expiresAt < :threshold")
    int deleteExpiredBefore(@Param("threshold") OffsetDateTime threshold);
}
