package jp.co.frontier.ems.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import jp.co.frontier.ems.entity.ImportJob;

/**
 * CSV取込ジョブの返却用DTO（API 10.1 / 10.2）。
 *
 * <p>受付直後（10.1）は件数がまだ分からないので {@code job_id} / {@code status} /
 * {@code total_rows} だけを返し、結果（10.2）では成功・失敗件数と
 * エラー詳細も返す。仕様書の例もその形になっている。
 *
 * <p>未確定の項目はキーごと省略する。処理中に {@code success_rows: 0} が
 * 返ると「0件成功で終わった」と読めてしまうため。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ImportJobResponse(
        String jobId,
        ImportJob.Status status,
        Integer totalRows,
        Integer successRows,
        Integer errorRows,
        List<ImportJob.RowError> errors) {

    /** 受付直後（API 10.1）。まだ処理していないので件数は総数のみ。 */
    public static ImportJobResponse accepted(ImportJob job) {
        return new ImportJobResponse(
                job.getJobId(), job.getStatus(), job.getTotalRows(), null, null, null);
    }

    /** 結果の取得（API 10.2）。 */
    public static ImportJobResponse from(ImportJob job) {
        return new ImportJobResponse(
                job.getJobId(),
                job.getStatus(),
                job.getTotalRows(),
                job.getSuccessRows(),
                job.getErrorRows(),
                job.getErrors());
    }
}
