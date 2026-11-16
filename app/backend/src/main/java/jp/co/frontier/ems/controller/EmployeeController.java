package jp.co.frontier.ems.controller;

import jakarta.validation.Valid;
import java.util.List;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.dto.EmployeeDetailResponse;
import jp.co.frontier.ems.dto.EmployeeListItemResponse;
import jp.co.frontier.ems.dto.EmployeeSearchCriteria;
import jp.co.frontier.ems.dto.SeatResponse;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import jp.co.frontier.ems.service.EmployeeService;
import jp.co.frontier.ems.service.FloorMapService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 社員。API 2. 社員検索 / 3. 社員詳細 / 4.4 座席取得 / 6. 社員CRUD に対応する。 */
@RestController
@RequestMapping(ApiPaths.V1 + "/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final FloorMapService floorMapService;

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
}
