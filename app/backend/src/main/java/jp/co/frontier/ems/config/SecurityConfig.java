package jp.co.frontier.ems.config;

import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.security.RestAuthenticationEntryPoint;
import jp.co.frontier.ems.security.TokenAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security の設定。
 *
 * <p><b>認証の適用範囲は段階的に広げている。</b> 現在トークンが必須なのは
 * {@code POST /v1/auth/logout} のみで、それ以外は認証なしでも通る。
 * 仕様書では「認証不要なのは login / refresh / accounts の3本のみ」だが、
 * 一気に切り替えるとフロント側のログイン実装が終わるまで画面が何も表示できなくなるため、
 * 並行して作業できるようこの形にしている。
 *
 * <p><b>TODO: フロント側のログイン実装が完了したら、{@code anyRequest()} を
 * {@code permitAll()} から {@code authenticated()} に変更する。</b>
 * 切り替え後は認証不要の3本以外すべてにトークンが必要になる。
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TokenAuthFilter tokenAuthFilter;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // トークン認証にするためCSRFトークンは使わない（Cookieセッションを使わないため）
                .csrf(csrf -> csrf.disable())
                // ブラウザのBasic認証ダイアログやログイン画面は使わない。
                // 認証は POST /v1/auth/login がJSONで受け取る
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                // サーバ側にセッションを持たない。認証状態は auth_tokens テーブルで管理する
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 未認証時の401を共通のエラー形式で返す
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .authorizeHttpRequests(auth -> auth
                        // 認証不要（ログイン前に叩く必要がある）
                        .requestMatchers(HttpMethod.POST,
                                ApiPaths.V1 + "/auth/login",
                                ApiPaths.V1 + "/auth/refresh",
                                ApiPaths.V1 + "/accounts").permitAll()
                        // 失効対象のトークンを特定する必要があるので認証必須
                        .requestMatchers(HttpMethod.POST,
                                ApiPaths.V1 + "/auth/logout").authenticated()
                        // TODO: フロントのログイン実装後 authenticated() に変更する
                        .anyRequest().permitAll())
                // 認可の判定より前にトークンを検証する必要があるため、
                // 標準のユーザー名/パスワード認証フィルタの手前に差し込む
                .addFilterBefore(tokenAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * パスワードのハッシュ化。
     *
     * <p>BCrypt の既定強度（cost 10）。開発用シードデータ
     * （V3__dev_employees.sql）のハッシュも同じ強度で生成している。
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
