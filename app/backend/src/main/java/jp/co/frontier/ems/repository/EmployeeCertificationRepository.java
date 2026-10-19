package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.EmployeeCertification;
import jp.co.frontier.ems.entity.EmployeeCertificationId;
import org.springframework.data.jpa.repository.JpaRepository;

/** 社員と資格の紐付け。API 6.6 / 6.7 */
public interface EmployeeCertificationRepository extends JpaRepository<EmployeeCertification, EmployeeCertificationId> {
}
