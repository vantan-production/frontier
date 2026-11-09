package jp.co.frontier.ems.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 定期実行を有効にする。
 *
 * <p>現在の用途は期限切れ認証トークンの掃除
 * （{@link jp.co.frontier.ems.service.AuthTokenCleanupService}）のみ。
 *
 * <p>{@code EmsApplication} にアノテーションを足す形でも動くが、
 * 「何のために定期実行を有効にしているか」を残したいので設定クラスに分けている。
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
