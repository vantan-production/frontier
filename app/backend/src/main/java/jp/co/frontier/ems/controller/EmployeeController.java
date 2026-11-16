package jp.co.frontier.ems.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.dto.EmployeeCertificationResponse;
import jp.co.frontier.ems.dto.EmployeeDetailResponse;
import jp.co.frontier.ems.dto.EmployeeLinkRequests;
import jp.co.frontier.ems.dto.EmployeeListItemResponse;
import jp.co.frontier.ems.dto.EmployeeProjectResponse;
import jp.co.frontier.ems.dto.EmployeeSearchCriteria;
import jp.co.frontier.ems.dto.EmployeeSkillResponse;
import jp.co.frontier.ems.dto.EmployeeUpsertRequest;
import jp.co.frontier.ems.dto.SeatResponse;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import jp.co.frontier.ems.service.EmployeeAdminService;
import jp.co.frontier.ems.service.EmployeeLinkService;
import jp.co.frontier.ems.service.EmployeeService;
import jp.co.frontier.ems.service.FloorMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 社員。API 2. 社員検索 / 3. 社員詳細 / 4.4 座席取得 / 6. 社員CRUD に対応する。 */
@RestController
@RequestMapping(ApiPaths.V1 + "/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final FloorMapService floorMapService;
    private final EmployeeAdminService employeeAdminService;
    private final EmployeeLinkService employeeLinkService;

    /**
     * GET /v1/employees — 社員検索・一覧（API 2.1）。
     *
     * <p>複数指定できる条件はカンマ区切りで受け取る（例 {@code skill_id=12,15,20}）。
     * 条件はすべて任意で、未指定のものは絞り込みに使わない。
     *
     * <p>{@code email} / {@code tel} は閲覧者によらず含めない（仕様書 2.1）。
     * 必要な場合は詳細取得（3.1）を使う。
     *
     * <p>{@code status=0}（退職者を含む）の指定は ADMIN のみ。判定に認証情報が
     * 必要なため principal を受け取る。
     */
    @GetMapping
    public ApiResponse<List<EmployeeListItemResponse>> search(
            @Valid @ModelAttribute EmployeeSearchCriteria criteria,
            @AuthenticationPrincipal AuthenticatedEmployee viewer) {

        var result = employeeService.search(criteria, viewer);
        return ApiResponse.of(result.rows(), result.meta());
    }

    /**
     * GET /v1/employees/{employee_id} — 社員詳細を取得する（API 3.1）。
     *
     * <p>詳細取得そのものは全社員に許可されている。閲覧者によって
     * {@code email} / {@code tel} を含めるか判断するため、認証情報を受け取る。
     */
    @GetMapping("/{employee_id}")
    public ApiResponse<EmployeeDetailResponse> detail(
            @PathVariable("employee_id") Long employeeId,
            @AuthenticationPrincipal AuthenticatedEmployee viewer) {

        return ApiResponse.of(employeeService.findDetail(employeeId, viewer));
    }

    /**
     * GET /v1/employees/{employee_id}/seat — 社員の座席を取得する（API 4.4）。
     *
     * <p>座席未登録の社員は 200 で {@code "data": null} を返す。
     * 404 になるのは社員自体が存在しない場合のみ（仕様書 4.4）。
     */
    @GetMapping("/{employee_id}/seat")
    public ApiResponse<SeatResponse> seat(@PathVariable("employee_id") Long employeeId) {
        return ApiResponse.of(floorMapService.findSeatByEmployee(employeeId));
    }

    // ------------------------------------------------------------------
    // 以下は ADMIN 専用（API 6章）。権限はサービス側の @PreAuthorize で判定する
    // ------------------------------------------------------------------

    /** POST /v1/employees — 社員の新規登録（API 6.1）。 */
    @PostMapping
    public ResponseEntity<ApiResponse<EmployeeDetailResponse>> create(
            @Valid @RequestBody EmployeeUpsertRequest request) {

        EmployeeDetailResponse created = employeeAdminService.create(request);
        return ResponseEntity
                .created(URI.create(ApiPaths.V1 + "/employees/" + created.employeeId()))
                .body(ApiResponse.of(created));
    }

    /**
     * PUT /v1/employees/{employee_id} — 社員情報の更新（API 6.2）。
     *
     * <p>PUT なので全項目の置換。送らなかった任意項目は null で上書きされる。
     */
    @PutMapping("/{employee_id}")
    public ApiResponse<EmployeeDetailResponse> update(
            @PathVariable("employee_id") Long employeeId,
            @Valid @RequestBody EmployeeUpsertRequest request) {

        return ApiResponse.of(employeeAdminService.update(employeeId, request));
    }

    /**
     * DELETE /v1/employees/{employee_id} — 社員の論理削除（API 6.3）。
     *
     * <p>レコードは消さず status を 0 にし、座席の割当を解除する。
     */
    @DeleteMapping("/{employee_id}")
    public ResponseEntity<Void> delete(@PathVariable("employee_id") Long employeeId) {
        employeeAdminService.logicalDelete(employeeId);
        return ResponseEntity.noContent().build();
    }

    /** POST /v1/employees/{employee_id}/skills — スキルの紐付け追加（API 6.4）。 */
    @PostMapping("/{employee_id}/skills")
    public ResponseEntity<ApiResponse<EmployeeSkillResponse>> addSkill(
            @PathVariable("employee_id") Long employeeId,
            @Valid @RequestBody EmployeeLinkRequests.SkillLink request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(employeeLinkService.addSkill(employeeId, request)));
    }

    /** PATCH /v1/employees/{employee_id}/skills/{skill_id} — 習熟度・経験年数の更新（API 6.5）。 */
    @PatchMapping("/{employee_id}/skills/{skill_id}")
    public ApiResponse<EmployeeSkillResponse> updateSkill(
            @PathVariable("employee_id") Long employeeId,
            @PathVariable("skill_id") Long skillId,
            @Valid @RequestBody EmployeeLinkRequests.SkillLinkUpdate request) {

        return ApiResponse.of(employeeLinkService.updateSkill(employeeId, skillId, request));
    }

    /** DELETE /v1/employees/{employee_id}/skills/{skill_id} — スキルの解除（API 6.5）。 */
    @DeleteMapping("/{employee_id}/skills/{skill_id}")
    public ResponseEntity<Void> removeSkill(
            @PathVariable("employee_id") Long employeeId,
            @PathVariable("skill_id") Long skillId) {

        employeeLinkService.removeSkill(employeeId, skillId);
        return ResponseEntity.noContent().build();
    }

    /** POST /v1/employees/{employee_id}/certifications — 資格の紐付け追加（API 6.6）。 */
    @PostMapping("/{employee_id}/certifications")
    public ResponseEntity<ApiResponse<EmployeeCertificationResponse>> addCertification(
            @PathVariable("employee_id") Long employeeId,
            @Valid @RequestBody EmployeeLinkRequests.CertificationLink request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(employeeLinkService.addCertification(employeeId, request)));
    }

    /** DELETE /v1/employees/{employee_id}/certifications/{certification_id} — 資格の解除（API 6.7）。 */
    @DeleteMapping("/{employee_id}/certifications/{certification_id}")
    public ResponseEntity<Void> removeCertification(
            @PathVariable("employee_id") Long employeeId,
            @PathVariable("certification_id") Long certificationId) {

        employeeLinkService.removeCertification(employeeId, certificationId);
        return ResponseEntity.noContent().build();
    }

    /** POST /v1/employees/{employee_id}/projects — 担当プロジェクトの追加（API 6.8）。 */
    @PostMapping("/{employee_id}/projects")
    public ResponseEntity<ApiResponse<EmployeeProjectResponse>> addProject(
            @PathVariable("employee_id") Long employeeId,
            @Valid @RequestBody EmployeeLinkRequests.ProjectLink request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.of(employeeLinkService.addProject(employeeId, request)));
    }

    /** DELETE /v1/employees/{employee_id}/projects/{project_id} — 担当プロジェクトの解除（API 6.9）。 */
    @DeleteMapping("/{employee_id}/projects/{project_id}")
    public ResponseEntity<Void> removeProject(
            @PathVariable("employee_id") Long employeeId,
            @PathVariable("project_id") Long projectId) {

        employeeLinkService.removeProject(employeeId, projectId);
        return ResponseEntity.noContent().build();
    }
}
