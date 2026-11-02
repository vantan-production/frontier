package jp.co.frontier.ems.controller;

import jakarta.validation.Valid;
import java.net.URI;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.dto.AccountCreateRequest;
import jp.co.frontier.ems.dto.AccountCreateResponse;
import jp.co.frontier.ems.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** アカウント作成。API 1.4 に対応する。 */
@RestController
@RequestMapping(ApiPaths.V1 + "/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    /**
     * POST /v1/accounts — アカウント作成（認証不要）。
     *
     * <p>201 と、作成された社員への {@code Location} ヘッダーを返す。
     * Location はベースURLからの絶対パス（例 {@code /v1/employees/456}）。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<AccountCreateResponse>> create(
            @Valid @RequestBody AccountCreateRequest request) {

        AccountCreateResponse created = accountService.create(request);
        URI location = URI.create(ApiPaths.V1 + "/employees/" + created.employeeId());

        return ResponseEntity.created(location).body(ApiResponse.of(created));
    }
}
