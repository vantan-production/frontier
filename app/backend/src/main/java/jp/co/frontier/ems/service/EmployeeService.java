package jp.co.frontier.ems.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import jp.co.frontier.ems.common.PageMeta;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.common.error.ErrorResponse;
import jp.co.frontier.ems.dto.EmployeeDetailResponse;
import jp.co.frontier.ems.dto.EmployeeListItemResponse;
import jp.co.frontier.ems.dto.EmployeeSearchCriteria;
import jp.co.frontier.ems.dto.EmployeeSkillResponse;
import jp.co.frontier.ems.dto.ProfileUpdateRequest;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.entity.EmployeeStatus;
import jp.co.frontier.ems.repository.EmployeeCertificationRepository;
import jp.co.frontier.ems.repository.EmployeeProjectRepository;
import jp.co.frontier.ems.repository.EmployeeRepository;
import jp.co.frontier.ems.repository.EmployeeSkillRepository;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
    private final TokenService tokenService;

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
     * 社員詳細を ADMIN として取得する。
     *
     * <p>登録・更新（API 6.1 / 6.2）のレスポンスに使う。実行者が ADMIN で
     * あることは呼び出し側（{@link EmployeeAdminService}）で担保されているため、
     * 機密項目を含めて返す。
     */
    @Transactional(readOnly = true)
    public EmployeeDetailResponse findDetailAsAdmin(Long employeeId) {
        Employee employee = employeeRepository.findWithMastersByEmployeeId(employeeId)
                .orElseThrow(() -> ApiException.notFound("指定された社員が見つかりません"));

        return buildDetail(employee, true);
    }

    /**
     * 指定社員のトークンをすべて失効させる。
     *
     * <p>社員の論理削除（API 6.3）から呼ぶ。退職者のトークンは検証時に
     * 在籍状態を見て弾かれるので穴は無いが、使えそうに見えるレコードを
     * 残さないため能動的に失効させる。
     */
    @Transactional
    public void revokeTokensOf(Long employeeId) {
        tokenService.revokeAllForEmployee(employeeId);
    }

    /**
     * 社員検索・一覧（API 2.1）。
     *
     * <p>任意指定のフィルタが10個近くあるため絞り込みは
     * {@link EmployeeSpecifications} が組む。ここでは並び順・ページング・
     * 権限の判定と、N+1 を避けたスキルの取得を担う。
     *
     * @param viewer 閲覧者。未認証の場合は null
     */
    @Transactional(readOnly = true)
    public PagedResult<EmployeeListItemResponse> search(
            EmployeeSearchCriteria criteria, AuthenticatedEmployee viewer) {

        // 退職者を含む検索は ADMIN のみ（仕様書 2.1 / 共通事項）。
        // 403 を返すのは、閲覧権限が無いことを伝えるだけで個々の社員の
        // 存在は漏れないため。詳細取得（3.1）で 404 にしているのとは事情が違う
        if (criteria.statusOrDefault() != EmployeeStatus.ACTIVE
                && (viewer == null || !viewer.isAdmin())) {
            throw ApiException.forbidden("退職者を含む検索は管理者のみ利用できます");
        }

        Pageable pageable = PageRequest.of(
                criteria.pageOrDefault() - 1,   // 仕様は1始まり、Spring Data は0始まり
                criteria.perPageOrDefault(),
                toSort(criteria.sort()));

        Page<Employee> page = employeeRepository.findAll(
                EmployeeSpecifications.from(criteria), pageable);

        Map<Long, List<EmployeeSkillResponse>> skills = findSkillsByEmployee(page.getContent());

        List<EmployeeListItemResponse> rows = page.getContent().stream()
                .map(e -> EmployeeListItemResponse.from(
                        e, skills.getOrDefault(e.getEmployeeId(), List.of())))
                .toList();

        return new PagedResult<>(rows, PageMeta.from(page));
    }

    /**
     * 一覧に出すスキルを社員IDごとにまとめて取得する。
     *
     * <p>社員ごとに問い合わせると1ページ20件で20クエリ飛ぶため、
     * IN 句で1クエリにしている。
     */
    private Map<Long, List<EmployeeSkillResponse>> findSkillsByEmployee(List<Employee> employees) {
        if (employees.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = employees.stream().map(Employee::getEmployeeId).toList();

        return employeeSkillRepository.findByEmployeeEmployeeIdIn(ids).stream()
                .collect(Collectors.groupingBy(
                        link -> link.getEmployee().getEmployeeId(),
                        Collectors.mapping(EmployeeSkillResponse::from, Collectors.toList())));
    }

    /**
     * {@code sort} クエリパラメータをエンティティの属性名に変換する。
     *
     * <p>クライアントが渡すのは JSON のキー名（{@code hire_date}）なので、
     * そのまま Spring Data に渡すと属性が見つからず 500 になる。
     * 許可した項目だけを明示的に対応づけ、それ以外は 422 で弾く。
     */
    private Sort toSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.by(Sort.Direction.ASC, "employeeCode");
        }

        boolean descending = sort.startsWith("-");
        String key = descending ? sort.substring(1) : sort;

        String property = switch (key) {
            case "employee_code" -> "employeeCode";
            case "name" -> "name";
            case "hire_date" -> "hireDate";
            case "department_id" -> "department.departmentId";
            default -> null;
        };

        if (property == null) {
            throw ApiException.validation(
                    "sortには employee_code / name / hire_date / department_id のいずれかを"
                            + "指定してください（降順は先頭に - を付ける）",
                    List.of(new ErrorResponse.ErrorDetail("sort", "指定できない並び順です")));
        }

        return Sort.by(descending ? Sort.Direction.DESC : Sort.Direction.ASC, property);
    }

    /**
     * 一覧とページ情報の組。
     *
     * <p>コントローラで {@code ApiResponse.of(data, meta)} に渡すために、
     * サービスからは両方を返す必要がある。
     */
    public record PagedResult<T>(List<T> rows, PageMeta meta) {
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
