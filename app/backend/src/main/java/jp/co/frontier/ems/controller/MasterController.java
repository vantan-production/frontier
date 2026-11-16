package jp.co.frontier.ems.controller;

import java.util.List;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.dto.CertificationResponse;
import jp.co.frontier.ems.dto.DepartmentResponse;
import jp.co.frontier.ems.dto.EmploymentTypeResponse;
import jp.co.frontier.ems.dto.PositionResponse;
import jp.co.frontier.ems.dto.ProjectResponse;
import jp.co.frontier.ems.dto.SkillResponse;
import jp.co.frontier.ems.service.MasterQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * マスタの参照。API 7.1〜7.9 の GET に対応する。
 *
 * <p>参照は全社員が利用できる。登録・更新・削除は ADMIN 限定で、
 * マスタごとに検証内容が変わるため別のコントローラに分ける。
 *
 * <p>マスタはいずれも件数が少なくプルダウンで使うため、ページネーションしない。
 */
@RestController
@RequestMapping(ApiPaths.V1)
@RequiredArgsConstructor
public class MasterController {

    private final MasterQueryService masterQueryService;

    /** GET /v1/departments — 部署一覧（API 7.1）。 */
    @GetMapping("/departments")
    public ApiResponse<List<DepartmentResponse>> departments() {
        return ApiResponse.of(masterQueryService.findDepartments());
    }

    /** GET /v1/positions — 役職一覧（API 7.2）。序列順。 */
    @GetMapping("/positions")
    public ApiResponse<List<PositionResponse>> positions() {
        return ApiResponse.of(masterQueryService.findPositions());
    }

    /** GET /v1/employment-types — 雇用形態一覧（API 7.6）。 */
    @GetMapping("/employment-types")
    public ApiResponse<List<EmploymentTypeResponse>> employmentTypes() {
        return ApiResponse.of(masterQueryService.findEmploymentTypes());
    }

    /** GET /v1/skills — スキルマスタ一覧（API 7.7）。分類→名称順。 */
    @GetMapping("/skills")
    public ApiResponse<List<SkillResponse>> skills() {
        return ApiResponse.of(masterQueryService.findSkills());
    }

    /** GET /v1/certifications — 資格マスタ一覧（API 7.8）。 */
    @GetMapping("/certifications")
    public ApiResponse<List<CertificationResponse>> certifications() {
        return ApiResponse.of(masterQueryService.findCertifications());
    }

    /** GET /v1/projects — プロジェクト一覧（API 7.9）。開始日の新しい順。 */
    @GetMapping("/projects")
    public ApiResponse<List<ProjectResponse>> projects() {
        return ApiResponse.of(masterQueryService.findProjects());
    }
}
