package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.Floor;
import org.springframework.data.jpa.repository.JpaRepository;

/** フロアマスタ。API 4.2 / 7.4 */
public interface FloorRepository extends JpaRepository<Floor, Long> {
}
