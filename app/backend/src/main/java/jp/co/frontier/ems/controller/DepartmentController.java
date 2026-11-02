package jp.co.frontier.ems.controller;

import java.util.List;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.dto.DepartmentResponse;
import jp.co.frontier.ems.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 部署マスタ。API 7.1 に対応する。
 *
 * <p>参照系（GET）は全社員が利用可、登録・更新・削除は ADMIN のみ。
 * 書込系はフェーズ1の範囲に含まれるが、本コミットでは参照のみ実装している。
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    /** GET /v1/departments — 部署の一覧を取得する。 */
    @GetMapping
    public ApiResponse<List<DepartmentResponse>> list() {
        return ApiResponse.of(departmentService.findAll());
    }
}
