package jp.co.frontier.ems.security;

import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import jp.co.frontier.ems.common.error.ErrorCode;
import jp.co.frontier.ems.common.error.ErrorResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 未認証のまま保護されたエンドポイントを叩いたときの応答。
 *
 * <p>既定のままだとSpring Securityが独自形式の401（空ボディやHTML）を返してしまい、
 * 他のエラーと形が揃わない。フロント側が常に同じ形でエラーを読めるよう、
 * 共通のエラーレスポンス形式に揃える。
 *
 * <p>フィルタ内で発生する401は {@link jp.co.frontier.ems.common.error.GlobalExceptionHandler}
 * では捕まえられない（例外ハンドラはコントローラに到達した後の層のため）ので、
 * ここで直接JSONを書き出している。
 *
 * <p><b>注意:</b> このプロジェクトの Jackson は 3.x で、{@code ObjectMapper} の
 * パッケージが {@code tools.jackson.databind} に変わっている
 * （2.x は {@code com.fasterxml.jackson.databind}）。
 * 一方 {@code @JsonInclude} などの注釈は {@code com.fasterxml.jackson.annotation} の
 * まま据え置かれているため、両方のパッケージが混在する。
 */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        writeUnauthorized(response, "認証が必要です");
    }

    /** 共通形式の401を書き出す。トークン検証フィルタからも使う。 */
    public void writeUnauthorized(HttpServletResponse response, String message)
            throws IOException {
        ErrorCode code = ErrorCode.UNAUTHORIZED;
        response.setStatus(code.getStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(), ErrorResponse.of(code, message, null));
    }
}
