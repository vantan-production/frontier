package jp.co.frontier.ems.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security の設定。
 *
 * <p><b>現状はすべてのリクエストを許可している（認証なし）。</b>
 * 認証は「1. 認証」のAPI実装時に、ここへトークン検証フィルタを追加して差し替える。
 * それまでの間、エンドポイントは誰でも叩ける状態である点に注意。
 *
 * <p>この設定を置かないと Spring Security の既定動作により、全エンドポイントが
 * 起動ごとに変わるランダムパスワードの HTTP Basic 認証で保護され、
 * 何を作っても 401 になる。
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // トークン認証にするためCSRFトークンは使わない（Cookieセッションを使わないため）
                .csrf(csrf -> csrf.disable())
                // ブラウザのBasic認証ダイアログやログイン画面は使わない。
                // 認証は /auth/login がJSONで受け取る
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                // サーバ側にセッションを持たない。認証状態は auth_tokens テーブルで管理する
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // TODO: 認証API実装時に、認証不要なパス（/auth/login, /auth/refresh,
                //       /accounts）以外は authenticated() に変更する
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }
}
