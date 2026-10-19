package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;

/** 役職マスタ。API 7.2 */
public interface PositionRepository extends JpaRepository<Position, Long> {
}
