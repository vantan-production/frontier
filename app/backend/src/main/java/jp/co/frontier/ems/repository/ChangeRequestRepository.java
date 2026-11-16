package jp.co.frontier.ems.repository;

import java.util.Optional;
import jp.co.frontier.ems.entity.ChangeRequest;
import jp.co.frontier.ems.entity.ChangeRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 基本情報変更申請。API 9.1〜9.4 */
public interface ChangeRequestRepository extends JpaRepository<ChangeRequest, Long> {

    /**
     * 一覧（API 9.2）。
     *
     * <p>ADMIN は全件、EMPLOYEE は自分の分だけ取得できる。{@code employeeId} に
     * null を渡すと絞り込まない（ADMIN用）。{@code status} も null なら絞り込まない。
     * 条件の組み合わせごとにメソッドを分けると4つ必要になるため1つにまとめている。
     *
     * <p>{@code @EntityGraph} で申請者と承認者を同時に取得している。
     * 付けないと申請1件ごとに社員への問い合わせが飛ぶ（N+1）。
     */
    @EntityGraph(attributePaths = {
            "targetEmployee", "targetEmployee.department", "targetEmployee.position",
            "reviewedBy", "reviewedBy.department", "reviewedBy.position"})
    @Query("""
            select r from ChangeRequest r
            where (:employeeId is null or r.targetEmployee.employeeId = :employeeId)
              and (:status is null or r.status = :status)
            """)
    Page<ChangeRequest> search(
            @Param("employeeId") Long employeeId,
            @Param("status") ChangeRequestStatus status,
            Pageable pageable);

    @EntityGraph(attributePaths = {
            "targetEmployee", "targetEmployee.department", "targetEmployee.position",
            "reviewedBy", "reviewedBy.department", "reviewedBy.position"})
    Optional<ChangeRequest> findWithEmployeesByRequestId(Long requestId);

    /**
     * 同じ社員・同じ項目で申請中のものがあるか（API 9.1 の CONFLICT 判定）。
     *
     * <p>DBにも部分UNIQUE制約（{@code where status = 0}）があるが、
     * 制約違反をそのまま投げると500になるため事前に確認する。
     */
    boolean existsByTargetEmployeeEmployeeIdAndTargetFieldAndStatus(
            Long employeeId, String targetField, ChangeRequestStatus status);
}
