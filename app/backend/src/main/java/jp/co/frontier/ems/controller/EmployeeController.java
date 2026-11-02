package jp.co.frontier.ems.controller;

import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.dto.EmployeeDetailResponse;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import jp.co.frontier.ems.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 社員。API 2. 社員検索 / 3. 社員詳細 / 6. 社員CRUD に対応する。 */
@RestController
@RequestMapping(ApiPaths.V1 + "/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

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
}
