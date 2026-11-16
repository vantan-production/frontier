package jp.co.frontier.ems.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import jp.co.frontier.ems.common.PageMeta;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.common.error.ErrorResponse;
import jp.co.frontier.ems.dto.ChangeRequestDtos.CreateRequest;
import jp.co.frontier.ems.dto.ChangeRequestDtos.Response;
import jp.co.frontier.ems.dto.ChangeRequestDtos.ReviewRequest;
import jp.co.frontier.ems.entity.ChangeRequest;
import jp.co.frontier.ems.entity.ChangeRequestStatus;
import jp.co.frontier.ems.entity.Employee;
import jp.co.frontier.ems.repository.ChangeRequestRepository;
import jp.co.frontier.ems.repository.DepartmentRepository;
import jp.co.frontier.ems.repository.EmployeeRepository;
import jp.co.frontier.ems.repository.EmploymentTypeRepository;
import jp.co.frontier.ems.repository.PositionRepository;
import jp.co.frontier.ems.repository.SeatRepository;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import jp.co.frontier.ems.service.EmployeeService.PagedResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 基本情報の変更申請（API 9.1〜9.4）。
 *
 * <p>申請できる項目は下記の {@link #申請可能な項目} に限る。仕様書 9.1 は
 * 「編集不可項目（self_pr / hobby / 顔写真は5章で直接編集）」としか書いて
 * いないため、<b>許可する項目を明示的に列挙する方式にした。</b>
 * 任意のカラム名を受け付けると、{@code password_hash} や {@code role} を
 * 書き換える申請を作れてしまう。
 */
@Service
@RequiredArgsConstructor
public class ChangeRequestService {

    /**
     * 変更申請を通せる項目。
     *
     * <p>含めていないものとその理由。
     * <ul>
     *   <li>{@code hobby} / {@code self_pr} / {@code image_url} … 本人が直接編集できる（API 5章）</li>
     *   <li>{@code employee_code} / {@code email} … 本人確認とログインIDに使う。
     *       ADMIN が社員登録時に設定する（決定事項1, 2）</li>
     *   <li>{@code password_hash} / {@code status} / {@code role} … 申請で変えるものではない</li>
     * </ul>
     */
    private static final Set<String> 申請可能な項目 = Set.of(
            "name", "name_kana", "tel", "hire_date",
            "department_id", "position_id", "employment_type_id", "seat_id");

    private final ChangeRequestRepository changeRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PositionRepository positionRepository;
    private final EmploymentTypeRepository employmentTypeRepository;
    private final SeatRepository seatRepository;

    /**
     * 申請の作成（API 9.1）。
     *
     * <p>申請対象はログイン中の本人に固定する。{@code before_value} は
     * 改ざん防止のためサーバ側で現在値を取得して記録する。
     */
    @Transactional
    public Response create(AuthenticatedEmployee applicant, CreateRequest request) {
        Employee employee = employeeRepository.findWithMastersByEmployeeId(applicant.employeeId())
                .orElseThrow(() -> ApiException.unauthorized(
                        "ログイン中の社員が見つかりません。再度ログインしてください"));

        String field = request.targetField();
        if (!申請可能な項目.contains(field)) {
            throw ApiException.validation(
                    "この項目は変更申請の対象外です。趣味・自己PR・顔写真は自分で直接編集できます",
                    List.of(new ErrorResponse.ErrorDetail("target_field",
                            "指定できる項目: " + String.join(", ", 申請可能な項目.stream().sorted().toList()))));
        }

        // 同じ項目で申請中のものは1件まで（仕様書 9.1 の CONFLICT）。
        // DBにも部分UNIQUE制約があるが、違反をそのまま投げると500になる
        if (changeRequestRepository.existsByTargetEmployeeEmployeeIdAndTargetFieldAndStatus(
                employee.getEmployeeId(), field, ChangeRequestStatus.PENDING)) {
            throw ApiException.conflict("この項目は既に申請中です");
        }

        // 変更後の値が項目として妥当かを申請時点で確認する。
        // 承認時まで気づけないと、承認したのに反映できないという状態になる
        検証(field, request.afterValue());

        ChangeRequest entity = new ChangeRequest();
        entity.setTargetEmployee(employee);
        entity.setTargetField(field);
        entity.setBeforeValue(現在値(employee, field));
        entity.setAfterValue(request.afterValue());
        entity.setReason(request.reason());
        entity.setStatus(ChangeRequestStatus.PENDING);

        return Response.from(changeRequestRepository.save(entity));
    }

    /**
     * 一覧（API 9.2）。ADMIN は全件、EMPLOYEE は自分の分だけ。
     */
    @Transactional(readOnly = true)
    public PagedResult<Response> search(
            AuthenticatedEmployee viewer, ChangeRequestStatus status, int page, int perPage) {

        // ADMIN 以外は自分の分に限定する。クエリで絞るので他人の分は見えない
        Long 絞り込む社員 = viewer.isAdmin() ? null : viewer.employeeId();

        Page<ChangeRequest> result = changeRequestRepository.search(
                絞り込む社員, status,
                PageRequest.of(page - 1, perPage,
                        Sort.by(Sort.Direction.DESC, "requestedAt")));

        return new PagedResult<>(
                result.getContent().stream().map(Response::from).toList(),
                PageMeta.from(result));
    }

    /** 詳細（API 9.2）。本人または ADMIN のみ。 */
    @Transactional(readOnly = true)
    public Response findDetail(AuthenticatedEmployee viewer, Long requestId) {
        ChangeRequest entity = find(requestId);

        boolean 本人 = entity.getTargetEmployee().getEmployeeId().equals(viewer.employeeId());
        if (!本人 && !viewer.isAdmin()) {
            throw ApiException.forbidden("他の社員の申請は閲覧できません");
        }
        return Response.from(entity);
    }

    /**
     * 承認・却下（API 9.3）。ADMIN のみ。
     *
     * <p>承認時は対象社員の情報も同時に更新する。
     */
    @Transactional
    public Response review(AuthenticatedEmployee reviewer, Long requestId, ReviewRequest request) {
        if (!reviewer.isAdmin()) {
            throw ApiException.forbidden("申請の承認・却下は管理者のみ行えます");
        }

        ChangeRequest entity = find(requestId);

        if (entity.getStatus().isProcessed()) {
            throw ApiException.validation("この申請は既に処理済みです", null);
        }
        if (request.status() == ChangeRequestStatus.PENDING) {
            throw ApiException.validation("承認（1）または却下（2）を指定してください",
                    List.of(new ErrorResponse.ErrorDetail("status", "1 または 2 を指定してください")));
        }

        if (request.status() == ChangeRequestStatus.APPROVED) {
            適用(entity.getTargetEmployee(), entity.getTargetField(), entity.getAfterValue());
        }

        Employee 承認者 = employeeRepository.findById(reviewer.employeeId())
                .orElseThrow(() -> ApiException.unauthorized("ログイン中の社員が見つかりません"));

        entity.setStatus(request.status());
        entity.setReviewedBy(承認者);
        entity.setReviewedAt(OffsetDateTime.now());
        entity.setReviewComment(request.comment());

        return Response.from(entity);
    }

    /**
     * 申請の取り下げ（API 9.4）。申請者本人のみ、申請中のときだけ。
     */
    @Transactional
    public void withdraw(AuthenticatedEmployee applicant, Long requestId) {
        ChangeRequest entity = find(requestId);

        if (!entity.getTargetEmployee().getEmployeeId().equals(applicant.employeeId())) {
            throw ApiException.forbidden("自分の申請以外は取り下げられません");
        }
        if (entity.getStatus().isProcessed()) {
            throw ApiException.validation("処理済みの申請は取り下げられません", null);
        }

        changeRequestRepository.delete(entity);
    }

    // ---------- 項目ごとの読み書き ----------

    /**
     * 現在値を文字列で取り出す。
     *
     * <p>項目によって型が違うので、文字列に正規化して {@code before_value} に入れる
     * （DBは VARCHAR(255)）。
     */
    private String 現在値(Employee employee, String field) {
        return switch (field) {
            case "name" -> employee.getName();
            case "name_kana" -> employee.getNameKana();
            case "tel" -> employee.getTel();
            case "hire_date" -> String.valueOf(employee.getHireDate());
            case "department_id" -> String.valueOf(employee.getDepartment().getDepartmentId());
            case "position_id" -> employee.getPosition() == null
                    ? null : String.valueOf(employee.getPosition().getPositionId());
            case "employment_type_id" ->
                    String.valueOf(employee.getEmploymentType().getEmploymentTypeId());
            case "seat_id" -> employee.getSeat() == null
                    ? null : String.valueOf(employee.getSeat().getSeatId());
            // 申請可能な項目は上で絞っているのでここには来ない
            default -> throw new IllegalStateException("未対応の項目: " + field);
        };
    }

    /** 変更後の値が項目として妥当かを確認する（申請時点のチェック）。 */
    private void 検証(String field, String value) {
        switch (field) {
            case "hire_date" -> 日付として解釈(value);
            case "department_id" -> {
                if (!departmentRepository.existsById(数値として解釈(field, value))) {
                    throw 不正な値(field, "指定された部署が存在しません");
                }
            }
            case "position_id" -> {
                if (!positionRepository.existsById(数値として解釈(field, value))) {
                    throw 不正な値(field, "指定された役職が存在しません");
                }
            }
            case "employment_type_id" -> {
                if (!employmentTypeRepository.existsById(数値として解釈(field, value))) {
                    throw 不正な値(field, "指定された雇用形態が存在しません");
                }
            }
            case "seat_id" -> {
                Long seatId = 数値として解釈(field, value);
                if (!seatRepository.existsById(seatId)) {
                    throw 不正な値(field, "指定された座席が存在しません");
                }
                if (employeeRepository.findBySeatSeatId(seatId).isPresent()) {
                    throw ApiException.conflict("指定された座席は既に他の社員が使用しています");
                }
            }
            default -> {
                // 文字列項目は Bean Validation の文字数制限で足りる
            }
        }
    }

    /**
     * 承認時に対象社員へ反映する。
     *
     * <p>申請から承認までの間に状況が変わっていることがあるため、ここでも
     * 存在確認と座席の重複確認をやり直す。
     */
    private void 適用(Employee employee, String field, String value) {
        検証(field, value);

        switch (field) {
            case "name" -> employee.setName(value);
            case "name_kana" -> employee.setNameKana(value);
            case "tel" -> employee.setTel(value);
            case "hire_date" -> employee.setHireDate(日付として解釈(value));
            case "department_id" -> employee.setDepartment(
                    departmentRepository.findById(数値として解釈(field, value)).orElseThrow());
            case "position_id" -> employee.setPosition(
                    positionRepository.findById(数値として解釈(field, value)).orElseThrow());
            case "employment_type_id" -> employee.setEmploymentType(
                    employmentTypeRepository.findById(数値として解釈(field, value)).orElseThrow());
            case "seat_id" -> employee.setSeat(
                    seatRepository.findById(数値として解釈(field, value)).orElseThrow());
            default -> throw new IllegalStateException("未対応の項目: " + field);
        }
    }

    private LocalDate 日付として解釈(String value) {
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw 不正な値("after_value", "日付は YYYY-MM-DD の形式で指定してください");
        }
    }

    private Long 数値として解釈(String field, String value) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw 不正な値("after_value", "%s にはIDを数値で指定してください".formatted(field));
        }
    }

    private ApiException 不正な値(String field, String reason) {
        return ApiException.validation(reason,
                List.of(new ErrorResponse.ErrorDetail(field, reason)));
    }

    private ChangeRequest find(Long requestId) {
        return changeRequestRepository.findWithEmployeesByRequestId(requestId)
                .orElseThrow(() -> ApiException.notFound("指定された申請が見つかりません"));
    }
}
