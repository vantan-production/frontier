package jp.co.frontier.ems.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import jp.co.frontier.ems.entity.Employee;
import org.springframework.data.jpa.repository.EntityGraph;
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

    /**
     * 社員と、画面表示に必要なマスタをまとめて取得する。
     *
     * <p>{@code @ManyToOne} はすべて LAZY なので、素の {@code findById} だと
     * 部署・役職・雇用形態・座席・フロア・拠点がそれぞれ別クエリになり、
     * 1件の表示で10クエリ発行される。一覧（API 2.1）で20件返すと
     * 同じ構造が件数分繰り返されるため、ここでまとめて取る。
     */
    @EntityGraph(attributePaths = {
            "department", "position", "employmentType", "seat", "seat.floor", "seat.floor.site"})
    Optional<Employee> findWithMastersByEmployeeId(Long employeeId);

    /**
     * 指定座席に在席している社員を取得する（API 4.3）。
     *
     * <p>在席の判定は employees.seat_id 側で行う（seats に在席フラグは持たない）。
     * 退職者は seat_id を NULL にするため、在籍者だけが引ける。
     *
     * <p>座席IDをまとめて渡すのは、フロアの座席ごとに問い合わせると
     * 座席数ぶんクエリが飛ぶため（N+1）。
     */
    @EntityGraph(attributePaths = {"department", "position"})
    List<Employee> findBySeatSeatIdIn(Collection<Long> seatIds);

    /** 社員番号の重複チェック（API 6.1）。 */
    boolean existsByEmployeeCode(String employeeCode);

    /** メールアドレスの重複チェック（API 6.1）。 */
    boolean existsByEmail(String email);

    /** 更新時の重複チェック（API 6.2）。自分自身は除外する。 */
    boolean existsByEmployeeCodeAndEmployeeIdNot(String employeeCode, Long employeeId);

    boolean existsByEmailAndEmployeeIdNot(String email, Long employeeId);

    /**
     * 指定座席に着席している社員を取得する。座席の二重割当の判定に使う。
     *
     * <p>DBにも UNIQUE 制約があるが、制約違反の例外をそのまま返すと
     * 500 になってしまうため、事前に確認して 409 を返す。
     */
    Optional<Employee> findBySeatSeatId(Long seatId);

    /** 指定部署に所属する社員がいるか（マスタ削除の可否判定）。 */
    boolean existsByDepartmentDepartmentId(Long departmentId);

    /** 指定役職の社員がいるか（マスタ削除の可否判定）。 */
    boolean existsByPositionPositionId(Long positionId);

    /** 指定雇用形態の社員がいるか（マスタ削除の可否判定）。 */
    boolean existsByEmploymentTypeEmploymentTypeId(Long employmentTypeId);
}
