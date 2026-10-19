package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.Certification;
import org.springframework.data.jpa.repository.JpaRepository;

/** 資格マスタ。API 7.8 */
public interface CertificationRepository extends JpaRepository<Certification, Long> {
}
