package jp.co.frontier.ems.service;

import java.util.ArrayList;
import java.util.List;
import jp.co.frontier.ems.common.error.ApiException;
import jp.co.frontier.ems.dto.EmployeeUpsertRequest;
import jp.co.frontier.ems.entity.ImportJob;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * CSV一括取込の本体を別スレッドで進める（API 10.1）。
 *
 * <p>受付（{@code EmployeeCsvService#accept}）とは別クラスにしている。
 * {@code @Async} は Spring のプロキシ経由でのみ効くため、同じクラス内の
 * メソッドを呼んでも非同期にならない。
 *
 * <p>SecurityContext は {@code AsyncConfig} が引き継いでいる。
 * 取込は受付時に認可済みのADMINの代わりに動くため、
 * {@code @PreAuthorize} 付きの登録処理をそのまま呼べる。
 */
@Component
@RequiredArgsConstructor
public class EmployeeCsvImportWorker {

    private static final Logger log = LoggerFactory.getLogger(EmployeeCsvImportWorker.class);

    private final EmployeeImportRowService rowService;

    /**
     * 行ごとに取り込み、結果をジョブに記録する。
     *
     * <p>1行の失敗で全体を止めない。仕様書 10.2 が成功件数とエラー件数を
     * 分けて返す形なので、取り込めた行は確定させ、失敗した行だけを
     * 理由つきで残す。
     *
     * @param rows ヘッダーを除いたデータ行。受付時に読み切ったものを受け取る
     */
    @Async("importTaskExecutor")
    public void run(String jobId, List<String[]> rows) {
        List<ImportJob.RowError> errors = new ArrayList<>();
        int 成功 = 0;

        for (int i = 0; i < rows.size(); i++) {
            // ヘッダーを除いた1始まりの行番号。CSVを開いたときの行と対応づけやすい
            int 行番号 = i + 1;
            try {
                EmployeeUpsertRequest request = EmployeeCsvService.行を変換する(rows.get(i));
                rowService.一行を取り込む(request);
                成功++;
            } catch (ApiException e) {
                errors.add(new ImportJob.RowError(行番号, e.getMessage()));
            } catch (IllegalArgumentException e) {
                // 列数不足・数値や日付として読めない場合
                errors.add(new ImportJob.RowError(行番号, e.getMessage()));
            } catch (RuntimeException e) {
                log.warn("取込中の想定外のエラー jobId={} row={}", jobId, 行番号, e);
                errors.add(new ImportJob.RowError(行番号, "取り込めませんでした"));
            }
        }

        // 全行失敗した場合のみ failed。一部でも通れば completed として
        // 結果を見てもらう（仕様書 10.2 の status）
        boolean 全滅 = 成功 == 0 && !rows.isEmpty();
        rowService.完了を記録する(jobId, 成功, errors, 全滅);

        log.info("CSV取込完了 jobId={} 成功={} 失敗={}", jobId, 成功, errors.size());
    }
}
