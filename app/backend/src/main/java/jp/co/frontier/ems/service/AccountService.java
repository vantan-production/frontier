package jp.co.frontier.ems.service;

import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.common.error.ErrorCode;
import jp.co.frontier.ems.dto.AccountCreateRequest;
import jp.co.frontier.ems.dto.AccountCreateResponse;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeStatus;
import jp.co.frontier.ems.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** アカウント作成（API 1.4）。社員マスタに登録済みの本人がパスワードを設定する。 */
@Service
@RequiredArgsConstructor
public class AccountService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

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
}
