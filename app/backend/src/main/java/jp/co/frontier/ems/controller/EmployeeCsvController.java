package jp.co.frontier.ems.controller;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import jp.co.frontier.ems.common.ApiPaths;
import jp.co.frontier.ems.common.ApiResponse;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.dto.EmployeeSearchCriteria;
import jp.co.frontier.ems.dto.ImportJobResponse;
import jp.co.frontier.ems.security.AuthenticatedEmployee;
import jp.co.frontier.ems.service.EmployeeCsvService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 社員情報のCSV一括取込・出力。API 10.1〜10.3 に対応する。ADMIN 専用。
 *
 * <p>権限はサービス側の {@code @PreAuthorize} で判定する。
 */
@RestController
@RequestMapping(ApiPaths.V1 + "/employees")
@RequiredArgsConstructor
public class EmployeeCsvController {

    private static final DateTimeFormatter FILE_NAME_DATE =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    private final EmployeeCsvService employeeCsvService;

    /**
     * POST /v1/employees/import — CSV一括取込（API 10.1）。
     *
     * <p>取り込みは非同期。受付時点で 202 を返し、結果は 10.2 で取得する。
     */
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<ImportJobResponse>> importCsv(
            @AuthenticationPrincipal AuthenticatedEmployee executor,
            @RequestParam("file") MultipartFile file) {

        if (executor == null) {
            throw ApiException.unauthorized("ログインしていません");
        }
        var job = employeeCsvService.accept(executor, file);

        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.of(ImportJobResponse.accepted(job)));
    }

    /** GET /v1/employees/import/{job_id} — 取込結果の取得（API 10.2）。 */
    @GetMapping("/import/{job_id}")
    public ApiResponse<ImportJobResponse> importResult(@PathVariable("job_id") String jobId) {
        return ApiResponse.of(ImportJobResponse.from(employeeCsvService.findJob(jobId)));
    }

    /**
     * GET /v1/employees/export — CSV出力（API 10.3）。
     *
     * <p>検索条件は 2.1 と同じものを指定できる。{@code page} / {@code per_page} は
     * 無効で全件出力する。
     *
     * <p>Excel対応のため UTF-8 BOM付きで出力する。
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportCsv(
            @Valid @ModelAttribute EmployeeSearchCriteria criteria) {

        String csv = employeeCsvService.エクスポートする(criteria);
        String fileName = "employees_%s.csv".formatted(LocalDate.now().format(FILE_NAME_DATE));

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"%s\"".formatted(fileName))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }
}
