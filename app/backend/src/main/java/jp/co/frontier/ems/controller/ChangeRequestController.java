package jp.co.frontier.ems.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.common.PageParams;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.dto.ChangeRequestDtos.CreateRequest;
import jp.co.frontier.ems.dto.ChangeRequestDtos.Response;
import jp.co.frontier.ems.dto.ChangeRequestDtos.ReviewRequest;
import jp.co.frontier.ems.entity.ChangeRequestStatus;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import jp.co.frontier.ems.service.ChangeRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 基本情報の変更申請。API 9.1〜9.4 に対応する。
 *
 * <p>権限が「本人」「本人/ADMIN」「ADMIN」と操作ごとに違うため、
 * {@code @PreAuthorize} ではなくサービス側で申請者と照合して判定している。
 * 「自分の申請かどうか」はデータを見ないと分からないので、注釈では表現できない。
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/change-requests")
@RequiredArgsConstructor
public class ChangeRequestController {

    private final ChangeRequestService changeRequestService;

    /**
     * POST /v1/change-requests — 申請を作成する（API 9.1）。
     *
     * <p>申請対象はログイン中の本人に固定。変更前の値はサーバが記録する。
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Response>> create(
            @AuthenticationPrincipal AuthenticatedEmployee applicant,
            @Valid @RequestBody CreateRequest request) {

        Response created = changeRequestService.create(require(applicant), request);

        return ResponseEntity
                .created(URI.create(ApiPaths.V1 + "/change-requests/" + created.requestId()))
                .body(ApiResponse.of(created));
    }

    /**
     * GET /v1/change-requests — 申請一覧（API 9.2）。
     *
     * <p>ADMIN は全件、それ以外は自分の申請のみ。
     */
    @GetMapping
    public ApiResponse<List<Response>> list(
            @AuthenticationPrincipal AuthenticatedEmployee viewer,
            @RequestParam(required = false) ChangeRequestStatus status,
            @Valid @ModelAttribute PageParams pageParams) {

        var result = changeRequestService.search(
                require(viewer), status,
                pageParams.pageOrDefault(), pageParams.perPageOrDefault());

        return ApiResponse.of(result.rows(), result.meta());
    }

    /** GET /v1/change-requests/{request_id} — 申請詳細（API 9.2）。本人または ADMIN。 */
    @GetMapping("/{request_id}")
    public ApiResponse<Response> detail(
            @AuthenticationPrincipal AuthenticatedEmployee viewer,
            @PathVariable("request_id") Long requestId) {

        return ApiResponse.of(changeRequestService.findDetail(require(viewer), requestId));
    }

    /**
     * PATCH /v1/change-requests/{request_id} — 承認・却下（API 9.3）。ADMIN のみ。
     *
     * <p>承認時は対象社員の情報も同時に更新される。
     */
    @PatchMapping("/{request_id}")
    public ApiResponse<Response> review(
            @AuthenticationPrincipal AuthenticatedEmployee reviewer,
            @PathVariable("request_id") Long requestId,
            @Valid @RequestBody ReviewRequest request) {

        return ApiResponse.of(
                changeRequestService.review(require(reviewer), requestId, request));
    }

    /**
     * DELETE /v1/change-requests/{request_id} — 申請の取り下げ（API 9.4）。
     *
     * <p>申請者本人のみ、申請中のときだけ。
     */
    @DeleteMapping("/{request_id}")
    public ResponseEntity<Void> withdraw(
            @AuthenticationPrincipal AuthenticatedEmployee applicant,
            @PathVariable("request_id") Long requestId) {

        changeRequestService.withdraw(require(applicant), requestId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 認証済みであることを要求する。
     *
     * <p>このコントローラの全操作で「誰が操作しているか」が必要なため、
     * principal が無い場合は処理に入る前に弾く。
     */
    private AuthenticatedEmployee require(AuthenticatedEmployee principal) {
        if (principal == null) {
            throw ApiException.unauthorized("ログインしていません");
        }
        return principal;
    }
}
