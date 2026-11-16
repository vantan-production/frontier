package jp.co.frontier.ems.service;

import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.common.error.ErrorCode;
import jp.co.frontier.ems.dto.EmployeeDetailResponse;
import jp.co.frontier.ems.dto.EmployeeUpsertRequest;
import jp.co.frontier.ems.entity.Department;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeStatus;
import jp.co.frontier.ems.entity.EmploymentType;
import jp.co.frontier.ems.entity.Position;
import jp.co.frontier.ems.entity.Seat;
import jp.co.frontier.ems.repository.DepartmentRepository;
import jp.co.frontier.ems.repository.EmployeeRepository;
import jp.co.frontier.ems.repository.EmploymentTypeRepository;
import jp.co.frontier.ems.repository.PositionRepository;
import jp.co.frontier.ems.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 社員情報の登録・更新・論理削除（API 6.1〜6.3）。ADMIN 専用。
 *
 * <p>権限は {@code @PreAuthorize} でメソッドに書く（DB設計書 決定事項3、
 * SecurityConfig の方針）。条件が処理のすぐ隣にあるので読み落としにくい。
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeAdminService {

    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final EmploymentTypeRepository employmentTypeRepository;
    private final SeatRepository seatRepository;
    private final EmployeeService employeeService;

    /**
     * 社員の新規登録（API 6.1）。
     *
     * <p>パスワードは設定しない。本人がアカウント作成（API 1.4）で設定するまで
     * {@code password_hash} は NULL のままで、ログインできない状態になる。
     */
    @Transactional
    public EmployeeDetailResponse create(EmployeeUpsertRequest request) {
        if (employeeRepository.existsByEmployeeCode(request.employeeCode())) {
            throw new ApiException(ErrorCode.DUPLICATE_EMPLOYEE_CODE,
                    "この社員番号は既に登録されています");
        }
        if (employeeRepository.existsByEmail(request.email())) {
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL,
                    "このメールアドレスは既に登録されています");
        }

        Employee employee = new Employee();
        apply(employee, request);

        // 採番された社員IDが必要なので、詳細を組む前に保存する
        Employee saved = employeeRepository.save(employee);
        return employeeService.findDetailAsAdmin(saved.getEmployeeId());
    }

    /**
     * 社員情報の更新（API 6.2）。
     *
     * <p>PUT なので全項目の置換。送らなかった任意項目は null で上書きされる。
     */
    @Transactional
    public EmployeeDetailResponse update(Long employeeId, EmployeeUpsertRequest request) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> ApiException.notFound("指定された社員が見つかりません"));

        if (employeeRepository.existsByEmployeeCodeAndEmployeeIdNot(
                request.employeeCode(), employeeId)) {
            throw new ApiException(ErrorCode.DUPLICATE_EMPLOYEE_CODE,
                    "この社員番号は既に他の社員で登録されています");
        }
        if (employeeRepository.existsByEmailAndEmployeeIdNot(request.email(), employeeId)) {
            throw new ApiException(ErrorCode.DUPLICATE_EMAIL,
                    "このメールアドレスは既に他の社員で登録されています");
        }

        apply(employee, request);
        return employeeService.findDetailAsAdmin(employeeId);
    }

    /**
     * 社員の論理削除（API 6.3）。
     *
     * <p>レコードは消さず {@code status} を 0 にし、あわせて座席の割当を解除する
     * （座席を空けるため。DB設計書 1章）。
     *
     * <p>トークンも失効させる。退職者のトークンは検証時に在籍状態を見て
     * 弾かれるので穴は無いが、使えそうに見えるレコードを残さないため。
     */
    @Transactional
    public void logicalDelete(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> ApiException.notFound("指定された社員が見つかりません"));

        // 既に退職済みなら 404（仕様書 6.3）
        if (employee.getStatus() != EmployeeStatus.ACTIVE) {
            throw ApiException.notFound("指定された社員が見つかりません");
        }

        employee.setStatus(EmployeeStatus.RETIRED);
        employee.setSeat(null);
        employeeService.revokeTokensOf(employeeId);
    }

    /** リクエストの内容をエンティティへ反映する。登録と更新で共通。 */
    private void apply(Employee employee, EmployeeUpsertRequest request) {
        employee.setEmployeeCode(request.employeeCode());
        employee.setName(request.name());
        employee.setNameKana(request.nameKana());
        employee.setEmail(request.email());
        employee.setTel(request.tel());
        employee.setHireDate(request.hireDate());
        employee.setDepartment(department(request.departmentId()));
        employee.setPosition(position(request.positionId()));
        employee.setEmploymentType(employmentType(request.employmentTypeId()));
        employee.setSeat(seat(request.seatId(), employee));
    }

    private Department department(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> ApiException.validation(
                        "指定された部署が存在しません", null));
    }

    private Position position(Long id) {
        if (id == null) {
            return null;
        }
        return positionRepository.findById(id)
                .orElseThrow(() -> ApiException.validation(
                        "指定された役職が存在しません", null));
    }

    private EmploymentType employmentType(Long id) {
        return employmentTypeRepository.findById(id)
                .orElseThrow(() -> ApiException.validation(
                        "指定された雇用形態が存在しません", null));
    }

    /**
     * 座席を解決する。他の社員が着席している座席は 409（仕様書 6.1 / 6.2）。
     *
     * <p>DBにも UNIQUE 制約があるが、制約違反をそのまま投げると 500 になるため
     * 事前に確認している。
     */
    private Seat seat(Long seatId, Employee target) {
        if (seatId == null) {
            return null;
        }
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> ApiException.validation(
                        "指定された座席が存在しません", null));

        employeeRepository.findBySeatSeatId(seatId).ifPresent(occupant -> {
            boolean 自分自身 = occupant.getEmployeeId() != null
                    && occupant.getEmployeeId().equals(target.getEmployeeId());
            if (!自分自身) {
                throw ApiException.conflict(
                        "指定された座席は既に他の社員に割り当てられています");
            }
        });

        return seat;
    }
}
