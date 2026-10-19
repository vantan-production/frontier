package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

/** 拠点マスタ。API 4.1 / 7.3 */
public interface SiteRepository extends JpaRepository<Site, Long> {
}
