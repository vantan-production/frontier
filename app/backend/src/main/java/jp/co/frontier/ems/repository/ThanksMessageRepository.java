package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.ThanksMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** サンクスメッセージ。API 8.1〜8.3 */
public interface ThanksMessageRepository extends JpaRepository<ThanksMessage, Long> {

    /**
     * タイムライン（API 8.2）。新着順はサービス側の Pageable で指定する。
     *
     * <p>{@code @EntityGraph} で送信者・受信者とその部署を同時に取得している。
     * 付けないとメッセージ1件ごとに社員と部署への問い合わせが飛ぶ（N+1）。
     */
    @EntityGraph(attributePaths = {
            "fromEmployee", "fromEmployee.department", "fromEmployee.position",
            "toEmployee", "toEmployee.department", "toEmployee.position"})
    Page<ThanksMessage> findAll(Pageable pageable);

    /**
     * 社員別の送受信履歴（API 8.3）。
     *
     * <p>送信・受信・両方を1つのクエリで切り替えられるようにしている。
     * {@code type} ごとにメソッドを分けると3つ必要になるため。
     *
     * @param includeSent     送信分を含めるか
     * @param includeReceived 受信分を含めるか
     */
    @EntityGraph(attributePaths = {
            "fromEmployee", "fromEmployee.department", "fromEmployee.position",
            "toEmployee", "toEmployee.department", "toEmployee.position"})
    @Query("""
            select m from ThanksMessage m
            where (:includeSent = true and m.fromEmployee.employeeId = :employeeId)
               or (:includeReceived = true and m.toEmployee.employeeId = :employeeId)
            """)
    Page<ThanksMessage> findByEmployee(
            @Param("employeeId") Long employeeId,
            @Param("includeSent") boolean includeSent,
            @Param("includeReceived") boolean includeReceived,
            Pageable pageable);
}
