package jp.co.frontier.ems.service;

import java.time.OffsetDateTime;
import java.util.List;
import jp.co.frontier.ems.dto.EmployeeUpsertRequest;
import jp.co.frontier.ems.entity.ImportJob;
import jp.co.frontier.ems.repository.ImportJobRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * CSV取込の行単位の書き込み（API 10.1）。
 *
 * <p>受付側（{@link EmployeeCsvService}）から分けているのは循環参照を避けるため。
 * 受付が取込ワーカーを呼び、ワーカーが行の書き込みを呼ぶので、書き込みが
 * 受付側に同居していると参照が一周してしまう。
 */
@Service
@RequiredArgsConstructor
public class EmployeeImportRowService {

    private static final Logger log = LoggerFactory.getLogger(EmployeeImportRowService.class);

    private final EmployeeAdminService employeeAdminService;
    private final ImportJobRepository importJobRepository;

    /**
     * 1行分を登録する。
     *
     * <p>{@code REQUIRES_NEW} にしているのは、1行の失敗で他の行まで巻き戻らない
     * ようにするため。仕様書 10.2 が成功件数とエラー件数を分けて返す形なので、
     * 行ごとに独立して確定する必要がある。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void 一行を取り込む(EmployeeUpsertRequest request) {
        employeeAdminService.create(request);
    }

    /**
     * 取り込み結果をジョブに記録する。
     *
     * <p>こちらも {@code REQUIRES_NEW}。行の取り込みとは独立に確定させて、
     * 途中で落ちても結果が残るようにする。
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void 完了を記録する(
            String jobId, int 成功, List<ImportJob.RowError> errors, boolean 失敗したか) {

        ImportJob job = importJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            log.warn("取込ジョブが見つかりません: {}", jobId);
            return;
        }
        job.setStatus(失敗したか ? ImportJob.Status.FAILED : ImportJob.Status.COMPLETED);
        job.setSuccessRows(成功);
        job.setErrorRows(errors.size());
        job.setErrors(errors);
        job.setFinishedAt(OffsetDateTime.now());
    }
}
