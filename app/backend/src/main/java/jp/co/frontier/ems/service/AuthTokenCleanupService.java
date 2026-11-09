package jp.co.frontier.ems.service;

import java.time.Duration;
import java.time.OffsetDateTime;
import jp.co.frontier.ems.repository.AuthTokenRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 期限切れになった認証トークンを定期的に物理削除する。
 *
 * <p>DB設計書 4章の「期限切れレコードは定期的に物理削除する」に対応する。
 * ログアウトしても {@code revoked_at} が入るだけでレコードは残るため、
 * 掃除しないと {@code auth_tokens} が増え続ける
 * （ログイン1回で2行、トークン再発行ごとに1行）。
 */
@Service
@RequiredArgsConstructor
public class AuthTokenCleanupService {

    /**
     * 期限切れ後も残しておく猶予期間。
     *
     * <p>猶予を置いているのは2つの理由から。
     *
     * <p>1つは、{@code parent_token_id} の外部キーが {@code ON DELETE CASCADE} の
     * ため、リフレッシュトークンを消すと子のアクセストークンも道連れになる。
     * アクセス（1時間）はリフレッシュ（14日）より後に発行されるので、
     * リフレッシュの期限直前に発行されたアクセスは、リフレッシュより
     * わずかに後まで有効でありうる。猶予を1日置けばその重なりは解消される。
     *
     * <p>もう1つは、障害調査で直近の認証履歴を見たいことがあるため。
     */
    static final Duration RETENTION_AFTER_EXPIRY = Duration.ofDays(1);

    private static final Logger log = LoggerFactory.getLogger(AuthTokenCleanupService.class);

    private final AuthTokenRepository authTokenRepository;

    /**
     * 既定で毎日 JST 4時に実行する。
     *
     * <p>利用者が最も少ない時間帯を選んでいる。削除対象が多いと
     * テーブルロックの時間が伸びるため、日中は避ける。
     *
     * <p>cron は {@code ems.auth-token-cleanup.cron} で上書きできる。
     * 動作確認のとき、4時まで待たずに実行させるため。
     */
    @Scheduled(cron = "${ems.auth-token-cleanup.cron:0 0 4 * * *}", zone = "Asia/Tokyo")
    @Transactional
    public void deleteExpiredTokens() {
        OffsetDateTime threshold = OffsetDateTime.now().minus(RETENTION_AFTER_EXPIRY);
        int deleted = authTokenRepository.deleteExpiredBefore(threshold);

        if (deleted > 0) {
            log.info("期限切れの認証トークンを削除しました: {} 件（{} より前に失効）", deleted, threshold);
        } else {
            log.debug("削除対象の期限切れトークンはありませんでした");
        }
    }
}
