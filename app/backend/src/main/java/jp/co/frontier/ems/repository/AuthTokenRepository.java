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

    /**
     * 指定社員の有効なトークンをすべて失効させる。
     *
     * <p>用途は2つ。
     * <ul>
     *   <li>パスワード変更時（API 1.6）— 他端末に残ったセッションを無効化する</li>
     *   <li>社員の論理削除時（API 6.3）— 退職者のトークンを残さない</li>
     * </ul>
     *
     * <p>物理削除ではなく {@code revoked_at} を入れる。いつ失効したかを
     * 追えるようにするため。レコード自体は
     * {@link jp.co.frontier.ems.service.AuthTokenCleanupService} が
     * 期限到来後にまとめて消す。
     *
     * @return 失効させた件数
     */
    @Modifying(flushAutomatically = true)
    @Query("update AuthToken t set t.revokedAt = :now "
            + "where t.employee.employeeId = :employeeId and t.revokedAt is null")
    int revokeAllByEmployeeId(@Param("employeeId") Long employeeId,
                              @Param("now") OffsetDateTime now);
}
