package jp.co.frontier.ems.service;

import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.dto.EmployeeDetailResponse;
import jp.co.frontier.ems.dto.ProfileUpdateRequest;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeStatus;
import jp.co.frontier.ems.repository.EmployeeCertificationRepository;
import jp.co.frontier.ems.repository.EmployeeProjectRepository;
import jp.co.frontier.ems.repository.EmployeeRepository;
import jp.co.frontier.ems.repository.EmployeeSkillRepository;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 社員の参照。API 3.1（社員詳細）と API 1.5（GET /me）。 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeSkillRepository employeeSkillRepository;
    private final EmployeeCertificationRepository employeeCertificationRepository;
    private final EmployeeProjectRepository employeeProjectRepository;

    /**
     * 社員詳細を取得する（API 3.1）。
     *
     * @param employeeId 対象の社員ID
     * @param viewer     閲覧者。未認証の場合は null
     */
    @Transactional(readOnly = true)
    public EmployeeDetailResponse findDetail(Long employeeId, AuthenticatedEmployee viewer) {
        Employee employee = employeeRepository.findWithMastersByEmployeeId(employeeId)
                .orElseThrow(() -> ApiException.notFound("指定された社員が見つかりません"));

        boolean isAdmin = viewer != null && viewer.isAdmin();

        // 退職者（status=0）を閲覧できるのは ADMIN のみ。
        // 403 ではなく 404 を返すのは、退職者の社員IDを総当たりで
        // 特定できないようにするため（存在自体を伏せる）
        if (employee.getStatus() != EmployeeStatus.ACTIVE && !isAdmin) {
            throw ApiException.notFound("指定された社員が見つかりません");
        }

        boolean isSelf = viewer != null && viewer.employeeId().equals(employeeId);

        return buildDetail(employee, isAdmin || isSelf);
    }

    /**
     * ログイン中の本人の詳細を取得する（API 1.5）。
     *
     * <p>本人なので機密項目（email / tel）は必ず含める。
     */
    @Transactional(readOnly = true)
    public EmployeeDetailResponse findMe(AuthenticatedEmployee viewer) {
        Employee employee = employeeRepository.findWithMastersByEmployeeId(viewer.employeeId())
                .orElseThrow(() -> ApiException.unauthorized(
                        "ログイン中の社員が見つかりません。再度ログインしてください"));

        return buildDetail(employee, true);
    }

    /**
     * 自己プロフィールの更新（API 5.1）。
     *
     * <p>指定された項目だけを更新する。JSON に項目が無い場合と null を明示した
     * 場合は Jackson 上で区別できないため、どちらも「更新しない」として扱う。
     * 値を消したい場合は空文字を送ってもらう。
     *
     * <p>編集できるのは趣味と自己PRのみ。それ以外の基本情報は変更申請
     * （API 9章）を通す必要があるため、ここでは触らない。
     */
    @Transactional
    public EmployeeDetailResponse updateMyProfile(
            AuthenticatedEmployee viewer, ProfileUpdateRequest request) {

        Employee employee = employeeRepository.findWithMastersByEmployeeId(viewer.employeeId())
                .orElseThrow(() -> ApiException.unauthorized(
                        "ログイン中の社員が見つかりません。再度ログインしてください"));

        if (request.hobby() != null) {
            employee.setHobby(request.hobby());
        }
        if (request.selfPr() != null) {
            employee.setSelfPr(request.selfPr());
        }

        // 本人の操作なので機密項目を含めて返す
        return buildDetail(employee, true);
    }

    /**
     * 紐付け3種を取得して詳細DTOに組み立てる。
     *
     * <p>1リクエストで発行されるクエリは計4本（社員＋マスタをまとめて1本、
     * スキル・資格・プロジェクトで各1本）。マスタ分は
     * {@code findWithMastersByEmployeeId} の {@code @EntityGraph} で、
     * 紐付け先マスタは各リポジトリの {@code @EntityGraph} でまとめている。
     *
     * <p>一覧（API 2.1）では紐付けの3本が件数分繰り返されるため、
     * そちらは社員IDをまとめて渡す形（{@code IN} 句）に変える必要がある。
     */
    private EmployeeDetailResponse buildDetail(Employee employee, boolean includeConfidential) {
        Long id = employee.getEmployeeId();
        return EmployeeDetailResponse.from(
                employee,
                employeeSkillRepository.findByEmployeeEmployeeId(id),
                employeeCertificationRepository.findByEmployeeEmployeeId(id),
                employeeProjectRepository.findByEmployeeEmployeeId(id),
                includeConfidential);
    }
}
