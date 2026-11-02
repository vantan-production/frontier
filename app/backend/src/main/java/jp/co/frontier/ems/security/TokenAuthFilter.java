package jp.co.frontier.ems.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import jp.co.frontier.ems.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code Authorization: Bearer} のアクセストークンを検証し、
 * 認証済みなら {@code SecurityContextHolder} に利用者を設定するフィルタ。
 *
 * <p>トークン方式が自前（SHA-256ハッシュを auth_tokens で管理）なので、
 * Spring Security の認証機構に寄せるより、素直にフィルタで書く方が
 * 処理の流れがそのまま読める。
 *
 * <p>ヘッダーの扱いは2通りに分けている。
 * <ul>
 *   <li><b>ヘッダーが無い</b> → 認証せずに次へ進める。許可設定（permitAll など）側で
 *       通すか弾くかが決まる</li>
 *   <li><b>ヘッダーはあるが無効</b> → その場で401を返す。失効済み・期限切れトークンでの
 *       アクセスは UNAUTHORIZED とする仕様のため、permitAll のパスでも黙って
 *       通してはいけない</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class TokenAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final TokenService tokenService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        Optional<String> rawToken = extractBearerToken(request);

        if (rawToken.isEmpty()) {
            chain.doFilter(request, response);
            return;
        }

        Optional<AuthenticatedEmployee> authenticated = tokenService.authenticate(rawToken.get());

        if (authenticated.isEmpty()) {
            authenticationEntryPoint.writeUnauthorized(
                    response, "アクセストークンが無効または期限切れです");
            return;
        }

        AuthenticatedEmployee employee = authenticated.get();
        var authentication = new UsernamePasswordAuthenticationToken(
                employee,
                null,
                // 役職ではなく role（EMPLOYEE / ADMIN）を権限として扱う。
                // hasRole("ADMIN") で参照できるよう ROLE_ 接頭辞を付ける
                List.of(new SimpleGrantedAuthority("ROLE_" + employee.role().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            chain.doFilter(request, response);
        } finally {
            // スレッドが再利用されるため、リクエスト終了時に必ず消す
            SecurityContextHolder.clearContext();
        }
    }

    private Optional<String> extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
