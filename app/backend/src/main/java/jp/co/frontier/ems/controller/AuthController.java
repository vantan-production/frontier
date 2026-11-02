package jp.co.frontier.ems.controller;

import jakarta.validation.Valid;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.dto.LoginRequest;
import jp.co.frontier.ems.dto.LoginResponse;
import jp.co.frontier.ems.dto.RefreshRequest;
import jp.co.frontier.ems.dto.RefreshResponse;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import jp.co.frontier.ems.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 認証。API 1.1 / 1.2 / 1.3 に対応する。 */
@RestController
@RequestMapping(ApiPaths.V1 + "/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /v1/auth/login — ログイン（認証不要）。
     *
     * <p>仕様書の例に合わせ、トークン発行のレスポンスは {@code data} で包まない。
     */
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /** POST /v1/auth/refresh — アクセストークンの再発行（認証不要）。 */
    @PostMapping("/refresh")
    public RefreshResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request.refreshToken());
    }

    /**
     * POST /v1/auth/logout — ログアウト。
     *
     * <p>失効させるトークンは、リクエストの {@code Authorization} ヘッダーを
     * フィルタが検証した結果から特定する。クライアントは何も送らなくてよい。
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal AuthenticatedEmployee employee) {
        if (employee == null) {
            throw ApiException.unauthorized("ログインしていません");
        }
        authService.logout(employee.tokenId());
        return ResponseEntity.noContent().build();
    }
}
