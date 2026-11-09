package jp.co.frontier.ems.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * テスト用の PostgreSQL を使い捨てコンテナで立てる。
 *
 * <p>開発用のDB（docker-compose の db）に対してテストを実行すると、
 * パスワード変更などでシードデータが書き換わってしまうため分離している。
 *
 * <p>H2 などの組み込みDBを使わない理由は、マイグレーションが
 * {@code COLLATE "ja-x-icu"} / {@code JSONB} / 部分ユニークインデックスを
 * 使っており、PostgreSQL 以外では流れないため。
 * 結果としてマイグレーション自体もテストで毎回検証されることになる。
 *
 * <p>{@code @ServiceConnection} が付いているので、接続情報は Spring Boot が
 * 自動でコンテナのものに差し替える。application.properties の設定は使われない。
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfig {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        // 開発環境の docker-compose と同じイメージに合わせる。
        // Testcontainers 2.0 で PostgreSQLContainer は非ジェネリックになっている
        return new PostgreSQLContainer("postgres:16-alpine");
    }
}
