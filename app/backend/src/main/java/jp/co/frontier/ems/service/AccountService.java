package jp.co.frontier.ems.service;

import java.util.List;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.common.error.ErrorCode;
import jp.co.frontier.ems.common.error.ErrorResponse;
import jp.co.frontier.ems.dto.AccountCreateRequest;
import jp.co.frontier.ems.dto.AccountCreateResponse;
import jp.co.frontier.ems.dto.PasswordChangeRequest;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeStatus;
import jp.co.frontier.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * アカウント（パスワード）の管理。API 1.4 アカウント作成 / 1.6 パスワード変更。
 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * アカウント作成（API 1.4）。
     *
     * <p>社員番号とメールアドレスの両方を照合する。社員の登録自体はADMINが
     * 先に済ませている前提で、ここで行うのはパスワードの設定のみ。
     */
    @Transactional
    public AccountCreateResponse create(AccountCreateRequest request) {
        Employee employee = employeeRepository.findByEmployeeCode(request.employeeCode())
                .orElseThrow(() -> ApiException.notFound(
                        "該当する社員番号が登録されていません。人事部にご確認ください"));

        // 退職者はアカウントを作れない。社員番号の存在有無を伏せるため 404 に揃える
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw ApiException.notFound(
                    "該当する社員番号が登録されていません。人事部にご確認ください");
        }

        // 本人確認を先に済ませる。メールアドレスはADMINが登録した値と一致する
        // 必要がある（決定事項2）。
        // 「作成済み」の判定より前に置いているのは、社員番号だけで
        // アカウントの有無を調べられないようにするため
        if (!employee.getEmail().equals(request.email())) {
            throw ApiException.validation(
                    "メールアドレスが登録内容と一致しません", null);
        }

        // 既にパスワードが設定済み = アカウント作成済み
        if (employee.getPasswordHash() != null) {
            throw new ApiException(ErrorCode.DUPLICATE_EMPLOYEE_CODE,
                    "この社員番号のアカウントは既に作成されています");
        }

        employee.setPasswordHash(passwordEncoder.encode(request.password()));

        return new AccountCreateResponse(employee.getEmployeeId());
    }

    /**
     * パスワード変更（API 1.6）。
     *
     * <p>現在のパスワードの照合を必須にしている。トークンを盗まれた場合に
     * 本人のパスワードを勝手に書き換えられないようにするため。
     *
     * <p>変更後は<b>その社員の全トークンを失効させる</b>。他端末に残った
     * セッションを無効化するためで、変更した端末も含めて再ログインが必要になる。
     * パスワードを変えた理由が「漏れたかもしれない」である可能性を考えると、
     * 現在の端末だけ残す作りにはしない方が安全。
     */
    @Transactional
    public void changePassword(Long employeeId, PasswordChangeRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> ApiException.unauthorized(
                        "ログイン中の社員が見つかりません。再度ログインしてください"));

        // アカウント未作成（パスワード未設定）の状態では変更できない。
        // この場合に使うのは API 1.4（アカウント作成）
        if (employee.getPasswordHash() == null) {
            throw ApiException.validation(
                    "パスワードが未設定です。アカウント作成からお試しください", null);
        }

        if (!passwordEncoder.matches(request.currentPassword(), employee.getPasswordHash())) {
            throw ApiException.validation("現在のパスワードが正しくありません",
                    List.of(new ErrorResponse.ErrorDetail(
                            "current_password", "現在のパスワードが正しくありません")));
        }

        if (passwordEncoder.matches(request.newPassword(), employee.getPasswordHash())) {
            throw ApiException.validation("現在と異なるパスワードを指定してください",
                    List.of(new ErrorResponse.ErrorDetail(
                            "new_password", "現在と同じパスワードは使用できません")));
        }

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        tokenService.revokeAllForEmployee(employeeId);
    }
}
