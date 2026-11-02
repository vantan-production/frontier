package jp.co.frontier.ems.service;

import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.dto.LoginRequest;
import jp.co.frontier.ems.dto.LoginResponse;
import jp.co.frontier.ems.dto.RefreshResponse;
import jp.co.frontier.ems.entity.AuthToken;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeStatus;
import jp.co.frontier.ems.entity.TokenType;
import jp.co.frontier.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** ログイン・トークン再発行・ログアウト（API 1.1 / 1.2 / 1.3）。 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    /**
     * ログイン（API 1.1）。
     *
     * <p>失敗理由（存在しないメール／パスワード違い／退職済み／アカウント未作成）は
     * <b>区別せず一律 UNAUTHORIZED を返す</b>。どれで失敗したか分かると、
     * 在籍している社員のメールアドレスを総当たりで特定できてしまうため。
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByEmail(request.email())
                .orElseThrow(() -> ApiException.unauthorized(
                        "メールアドレスまたはパスワードが正しくありません"));

        if (employee.getStatus() != EmployeeStatus.ACTIVE
                || employee.getPasswordHash() == null
                || !passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw ApiException.unauthorized("メールアドレスまたはパスワードが正しくありません");
        }

        // リフレッシュを先に発行し、アクセスの発行元として紐付ける
        TokenService.IssuedToken refresh = tokenService.issueRefreshToken(employee);
        TokenService.IssuedToken access =
                tokenService.issueAccessToken(employee, refresh.entity());

        return new LoginResponse(
                access.rawToken(),
                refresh.rawToken(),
                TokenService.ACCESS_TOKEN_TTL.toSeconds(),
                LoginResponse.EmployeeSummary.from(employee));
    }

    /**
     * アクセストークンの再発行（API 1.2）。
     *
     * <p>リフレッシュトークンはローテーションせず、期限（14日）まで同じものを使う。
     */
    @Transactional
    public RefreshResponse refresh(String rawRefreshToken) {
        AuthToken refresh = tokenService.findValidToken(rawRefreshToken, TokenType.REFRESH)
                .orElseThrow(() -> ApiException.unauthorized(
                        "リフレッシュトークンが無効または失効しています。再度ログインしてください"));

        TokenService.IssuedToken access =
                tokenService.issueAccessToken(refresh.getEmployee(), refresh);

        return new RefreshResponse(
                access.rawToken(), TokenService.ACCESS_TOKEN_TTL.toSeconds());
    }

    /**
     * ログアウト（API 1.3）。
     *
     * <p>アクセストークンと、それに紐づくリフレッシュトークンの両方を失効させる。
     */
    @Transactional
    public void logout(Long accessTokenId) {
        tokenService.revokeChain(accessTokenId);
    }
}
