package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.EmployeeCertification;
import jp.co.frontier.ems.entity.EmployeeCertificationId;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** 社員と資格の紐付け。API 6.6 / 6.7 */
public interface EmployeeCertificationRepository extends JpaRepository<EmployeeCertification, EmployeeCertificationId> {

    /**
     * 指定社員の資格紐付けを全件取得する。
     *
     * <p>{@code @EntityGraph} で資格マスタを同時に取得している。
     * 付けないと紐付け1件ごとにマスタへの問い合わせが飛ぶ（N+1）。
     */
    @EntityGraph(attributePaths = "certification")
    List<EmployeeCertification> findByEmployeeEmployeeId(Long employeeId);
}
