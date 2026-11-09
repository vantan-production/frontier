package jp.co.frontier.ems.controller;

import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.common.error.ApiException;
import jakarta.validation.Valid;
import jp.co.frontier.ems.dto.EmployeeDetailResponse;
import jp.co.frontier.ems.dto.PasswordChangeRequest;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import jp.co.frontier.ems.service.AccountService;
import jp.co.frontier.ems.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ログイン中の本人を対象とする操作。API 1.5 / 5. 自己PR編集 に対応する。
 *
 * <p>エンドポイント設計書「{@code /me} パターン」のとおり、本人限定の操作は
 * {@code /employees/{employee_id}} ではなく {@code /me} 以下に置く。
 * URLの時点で他人を指定できないため、権限チェックの漏れが起きない。
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/me")
@RequiredArgsConstructor
public class MeController {

    private final EmployeeService employeeService;
    private final AccountService accountService;

    /** GET /v1/me — ログイン中の自分の情報を取得する（API 1.5）。 */
    @GetMapping
    public ApiResponse<EmployeeDetailResponse> me(
            @AuthenticationPrincipal AuthenticatedEmployee viewer) {

        if (viewer == null) {
            throw ApiException.unauthorized("ログインしていません");
        }
        return ApiResponse.of(employeeService.findMe(viewer));
    }

    /**
     * PATCH /v1/me/password — パスワードを変更する（API 1.6）。
     *
     * <p>成功すると全トークンが失効するため、クライアントは 204 を受けたら
     * ログイン画面へ戻す必要がある。現在使っているトークンもこの時点で無効。
     */
    @PatchMapping("/password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal AuthenticatedEmployee viewer,
            @Valid @RequestBody PasswordChangeRequest request) {

        if (viewer == null) {
            throw ApiException.unauthorized("ログインしていません");
        }
        accountService.changePassword(viewer.employeeId(), request);
        return ResponseEntity.noContent().build();
    }
}
