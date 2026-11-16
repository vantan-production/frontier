package jp.co.frontier.ems.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.common.PageParams;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.dto.ThanksMessageDtos.HistoryType;
import jp.co.frontier.ems.dto.ThanksMessageDtos.Response;
import jp.co.frontier.ems.dto.ThanksMessageDtos.SendRequest;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import jp.co.frontier.ems.service.ThanksMessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * サンクスメッセージ。API 8.1〜8.3 に対応する。
 *
 * <p>社員別の送受信履歴（API 8.3）は {@code /employees/{employee_id}/thanks-messages}
 * だが、サンクスメッセージの機能としてまとまっている方が読みやすいので
 * このコントローラに置いている。
 */
@RestController
@RequestMapping(ApiPaths.V1)
@RequiredArgsConstructor
public class ThanksMessageController {

    private final ThanksMessageService thanksMessageService;

    /**
     * POST /v1/thanks-messages — メッセージを送信する（API 8.1）。
     *
     * <p>送信者はトークンから判定するのでリクエストに含めない。
     * クライアントに送信者を指定させると他人になりすませてしまう。
     */
    @PostMapping("/thanks-messages")
    public ResponseEntity<ApiResponse<Response>> send(
            @AuthenticationPrincipal AuthenticatedEmployee sender,
            @Valid @RequestBody SendRequest request) {

        if (sender == null) {
            throw ApiException.unauthorized("ログインしていません");
        }
        Response created = thanksMessageService.send(sender, request);

        return ResponseEntity
                .created(URI.create(ApiPaths.V1 + "/thanks-messages/" + created.messageId()))
                .body(ApiResponse.of(created));
    }

    /** GET /v1/thanks-messages — タイムラインを取得する（API 8.2）。新着順固定。 */
    @GetMapping("/thanks-messages")
    public ApiResponse<List<Response>> timeline(@Valid @ModelAttribute PageParams pageParams) {
        var result = thanksMessageService.findTimeline(
                pageParams.pageOrDefault(), pageParams.perPageOrDefault());

        return ApiResponse.of(result.rows(), result.meta());
    }

    /**
     * GET /v1/employees/{employee_id}/thanks-messages — 社員別の送受信履歴（API 8.3）。
     *
     * @param type 取得範囲。{@code sent} / {@code received} / {@code all}（既定）
     */
    @GetMapping("/employees/{employee_id}/thanks-messages")
    public ApiResponse<List<Response>> history(
            @PathVariable("employee_id") Long employeeId,
            @RequestParam(required = false) HistoryType type,
            @Valid @ModelAttribute PageParams pageParams) {

        var result = thanksMessageService.findByEmployee(
                employeeId, type, pageParams.pageOrDefault(), pageParams.perPageOrDefault());

        return ApiResponse.of(result.rows(), result.meta());
    }
}
