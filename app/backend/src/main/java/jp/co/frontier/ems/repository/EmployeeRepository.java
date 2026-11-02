package jp.co.frontier.ems.repository;

import java.util.Optional;
import jp.co.frontier.ems.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 社員。API 2. 社員検索 / 3. 社員詳細 / 5. 自己PR編集 / 6. 社員CRUD
 *
 * <p>{@code JpaSpecificationExecutor} を継承しているのは、API 2.1 の検索が
 * 部署・役職・拠点・フロア・スキル（複数＋and/or）・資格・プロジェクト・
 * 雇用形態・status と任意指定のフィルタを10個近く持つため。
 * メソッド名による導出では表現できないので {@code Specification} で動的に組む。
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long>,
        JpaSpecificationExecutor<Employee> {

    /** ログイン時にメールアドレスで引く（email はログインID）。 */
    Optional<Employee> findByEmail(String email);

    /** アカウント作成時の本人確認で社員番号から引く。 */
    Optional<Employee> findByEmployeeCode(String employeeCode);
}
