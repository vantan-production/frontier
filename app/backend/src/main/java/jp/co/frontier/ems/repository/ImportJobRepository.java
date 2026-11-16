package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.ImportJob;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * CSV一括インポートのジョブ。API 10.1 / 10.2
 *
 * <p>主キーは採番ではなくアプリが組み立てる文字列（{@code imp_20260727_001}）。
 */
public interface ImportJobRepository extends JpaRepository<ImportJob, String> {

    /** 同じ日付の採番済み件数。job_id の連番部分を決めるのに使う。 */
    long countByJobIdStartingWith(String prefix);
}
