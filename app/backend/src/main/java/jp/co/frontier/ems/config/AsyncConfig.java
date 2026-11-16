package jp.co.frontier.ems.config;

import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.task.DelegatingSecurityContextAsyncTaskExecutor;

/**
 * 非同期処理の設定。
 *
 * <p>現在の用途はCSV一括取込（API 10.1）のみ。仕様書が「取り込みは非同期。
 * 結果は 10.2 で取得する」としているため、202 を返してから別スレッドで進める。
 *
 * <p><b>SecurityContext を非同期スレッドへ引き継いでいる。</b>
 * 既定では引き継がれないため、取込処理が呼ぶ
 * {@code @PreAuthorize("hasRole('ADMIN')")} 付きのメソッドが
 * <b>権限不足で拒否される</b>。取込は受付時に認可済みのADMINの代わりに
 * 動いているので、その文脈を引き継ぐのが正しい。
 */
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    @Override
    @Bean("importTaskExecutor")
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 一括取込は同時に何本も走る想定がない（4月・10月の異動時に数回）
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("csv-import-");
        executor.initialize();

        return new DelegatingSecurityContextAsyncTaskExecutor(executor);
    }
}
