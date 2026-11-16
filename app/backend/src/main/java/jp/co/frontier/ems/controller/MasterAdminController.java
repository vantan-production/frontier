package jp.co.frontier.ems.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.function.Function;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.dto.CertificationResponse;
import jp.co.frontier.ems.dto.DepartmentResponse;
import jp.co.frontier.ems.dto.EmploymentTypeResponse;
import jp.co.frontier.ems.dto.FloorResponse;
import jp.co.frontier.ems.dto.MasterUpsertRequests.CertificationUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.DepartmentUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.EmploymentTypeUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.FloorUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.PositionUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.ProjectUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.SeatUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.SiteUpsert;
import jp.co.frontier.ems.dto.MasterUpsertRequests.SkillUpsert;
import jp.co.frontier.ems.dto.PositionResponse;
import jp.co.frontier.ems.dto.ProjectResponse;
import jp.co.frontier.ems.dto.SeatResponse;
import jp.co.frontier.ems.dto.SiteResponse;
import jp.co.frontier.ems.dto.SkillResponse;
import jp.co.frontier.ems.service.MasterAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * マスタの登録・更新・削除。API 7.1〜7.9 の書込系に対応する。ADMIN 専用。
 *
 * <p>参照（GET）は {@link MasterController} 側。全社員が利用できるものと
 * ADMIN 限定のものが混在すると権限の範囲が読みにくいため、分けている。
 *
 * <p>権限はサービス側の {@code @PreAuthorize} で判定する。
 *
 * <p>権限マスタ（API 7.10 / 7.11）は実装していない。前期スコープでは
 * 権限テーブルを使わない判断のため（DB設計書 決定事項3）。
 *
 * <p>レスポンスは仕様書どおり、POST が 201 + Location、PUT が 200、
 * DELETE が 204。
 */
@RestController
@RequestMapping(ApiPaths.V1)
@RequiredArgsConstructor
public class MasterAdminController {

    private final MasterAdminService masterAdminService;

    // ---------- 7.1 部署 ----------

    @PostMapping("/departments")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody DepartmentUpsert request) {
        return created("/departments", masterAdminService.createDepartment(request),
                DepartmentResponse::departmentId);
    }

    @PutMapping("/departments/{department_id}")
    public ApiResponse<DepartmentResponse> updateDepartment(
            @PathVariable("department_id") Long id,
            @Valid @RequestBody DepartmentUpsert request) {
        return ApiResponse.of(masterAdminService.updateDepartment(id, request));
    }

    @DeleteMapping("/departments/{department_id}")
    public ResponseEntity<Void> deleteDepartment(@PathVariable("department_id") Long id) {
        masterAdminService.deleteDepartment(id);
        return noContent();
    }

    // ---------- 7.2 役職 ----------

    @PostMapping("/positions")
    public ResponseEntity<ApiResponse<PositionResponse>> createPosition(
            @Valid @RequestBody PositionUpsert request) {
        return created("/positions", masterAdminService.createPosition(request),
                PositionResponse::positionId);
    }

    @PutMapping("/positions/{position_id}")
    public ApiResponse<PositionResponse> updatePosition(
            @PathVariable("position_id") Long id,
            @Valid @RequestBody PositionUpsert request) {
        return ApiResponse.of(masterAdminService.updatePosition(id, request));
    }

    @DeleteMapping("/positions/{position_id}")
    public ResponseEntity<Void> deletePosition(@PathVariable("position_id") Long id) {
        masterAdminService.deletePosition(id);
        return noContent();
    }

    // ---------- 7.3 拠点 ----------

    @PostMapping("/sites")
    public ResponseEntity<ApiResponse<SiteResponse>> createSite(
            @Valid @RequestBody SiteUpsert request) {
        return created("/sites", masterAdminService.createSite(request), SiteResponse::siteId);
    }

    @PutMapping("/sites/{site_id}")
    public ApiResponse<SiteResponse> updateSite(
            @PathVariable("site_id") Long id, @Valid @RequestBody SiteUpsert request) {
        return ApiResponse.of(masterAdminService.updateSite(id, request));
    }

    @DeleteMapping("/sites/{site_id}")
    public ResponseEntity<Void> deleteSite(@PathVariable("site_id") Long id) {
        masterAdminService.deleteSite(id);
        return noContent();
    }

    // ---------- 7.4 フロア ----------

    @PostMapping("/floors")
    public ResponseEntity<ApiResponse<FloorResponse>> createFloor(
            @Valid @RequestBody FloorUpsert request) {
        return created("/floors", masterAdminService.createFloor(request), FloorResponse::floorId);
    }

    @PutMapping("/floors/{floor_id}")
    public ApiResponse<FloorResponse> updateFloor(
            @PathVariable("floor_id") Long id, @Valid @RequestBody FloorUpsert request) {
        return ApiResponse.of(masterAdminService.updateFloor(id, request));
    }

    @DeleteMapping("/floors/{floor_id}")
    public ResponseEntity<Void> deleteFloor(@PathVariable("floor_id") Long id) {
        masterAdminService.deleteFloor(id);
        return noContent();
    }

    // ---------- 7.5 座席 ----------

    @PostMapping("/seats")
    public ResponseEntity<ApiResponse<SeatResponse>> createSeat(
            @Valid @RequestBody SeatUpsert request) {
        return created("/seats", masterAdminService.createSeat(request), SeatResponse::seatId);
    }

    @PutMapping("/seats/{seat_id}")
    public ApiResponse<SeatResponse> updateSeat(
            @PathVariable("seat_id") Long id, @Valid @RequestBody SeatUpsert request) {
        return ApiResponse.of(masterAdminService.updateSeat(id, request));
    }

    @DeleteMapping("/seats/{seat_id}")
    public ResponseEntity<Void> deleteSeat(@PathVariable("seat_id") Long id) {
        masterAdminService.deleteSeat(id);
        return noContent();
    }

    // ---------- 7.6 雇用形態 ----------

    @PostMapping("/employment-types")
    public ResponseEntity<ApiResponse<EmploymentTypeResponse>> createEmploymentType(
            @Valid @RequestBody EmploymentTypeUpsert request) {
        return created("/employment-types", masterAdminService.createEmploymentType(request),
                EmploymentTypeResponse::employmentTypeId);
    }

    @PutMapping("/employment-types/{employment_type_id}")
    public ApiResponse<EmploymentTypeResponse> updateEmploymentType(
            @PathVariable("employment_type_id") Long id,
            @Valid @RequestBody EmploymentTypeUpsert request) {
        return ApiResponse.of(masterAdminService.updateEmploymentType(id, request));
    }

    @DeleteMapping("/employment-types/{employment_type_id}")
    public ResponseEntity<Void> deleteEmploymentType(
            @PathVariable("employment_type_id") Long id) {
        masterAdminService.deleteEmploymentType(id);
        return noContent();
    }

    // ---------- 7.7 スキル ----------

    @PostMapping("/skills")
    public ResponseEntity<ApiResponse<SkillResponse>> createSkill(
            @Valid @RequestBody SkillUpsert request) {
        return created("/skills", masterAdminService.createSkill(request), SkillResponse::skillId);
    }

    @PutMapping("/skills/{skill_id}")
    public ApiResponse<SkillResponse> updateSkill(
            @PathVariable("skill_id") Long id, @Valid @RequestBody SkillUpsert request) {
        return ApiResponse.of(masterAdminService.updateSkill(id, request));
    }

    @DeleteMapping("/skills/{skill_id}")
    public ResponseEntity<Void> deleteSkill(@PathVariable("skill_id") Long id) {
        masterAdminService.deleteSkill(id);
        return noContent();
    }

    // ---------- 7.8 資格 ----------

    @PostMapping("/certifications")
    public ResponseEntity<ApiResponse<CertificationResponse>> createCertification(
            @Valid @RequestBody CertificationUpsert request) {
        return created("/certifications", masterAdminService.createCertification(request),
                CertificationResponse::certificationId);
    }

    @PutMapping("/certifications/{certification_id}")
    public ApiResponse<CertificationResponse> updateCertification(
            @PathVariable("certification_id") Long id,
            @Valid @RequestBody CertificationUpsert request) {
        return ApiResponse.of(masterAdminService.updateCertification(id, request));
    }

    @DeleteMapping("/certifications/{certification_id}")
    public ResponseEntity<Void> deleteCertification(
            @PathVariable("certification_id") Long id) {
        masterAdminService.deleteCertification(id);
        return noContent();
    }

    // ---------- 7.9 プロジェクト ----------

    @PostMapping("/projects")
    public ResponseEntity<ApiResponse<ProjectResponse>> createProject(
            @Valid @RequestBody ProjectUpsert request) {
        return created("/projects", masterAdminService.createProject(request),
                ProjectResponse::projectId);
    }

    @PutMapping("/projects/{project_id}")
    public ApiResponse<ProjectResponse> updateProject(
            @PathVariable("project_id") Long id, @Valid @RequestBody ProjectUpsert request) {
        return ApiResponse.of(masterAdminService.updateProject(id, request));
    }

    @DeleteMapping("/projects/{project_id}")
    public ResponseEntity<Void> deleteProject(@PathVariable("project_id") Long id) {
        masterAdminService.deleteProject(id);
        return noContent();
    }

    // ---------- 共通 ----------

    /**
     * 201 + Location を返す。
     *
     * <p>Location はベースURLからの絶対パス（仕様書「共通事項」）。
     * マスタ9種で同じ形なので共通化している。
     */
    private <T> ResponseEntity<ApiResponse<T>> created(
            String basePath, T body, Function<T, Long> idExtractor) {

        return ResponseEntity
                .created(URI.create(ApiPaths.V1 + basePath + "/" + idExtractor.apply(body)))
                .body(ApiResponse.of(body));
    }

    private ResponseEntity<Void> noContent() {
        return ResponseEntity.noContent().build();
    }
}
