package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.Floor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** フロアマスタ。API 4.2 / 7.4 */
public interface FloorRepository extends JpaRepository<Floor, Long> {

    /**
     * 指定拠点のフロアを階数順に取得する（API 4.2）。
     * 地下が負数なので昇順で地下→低層→高層の並びになる。
     */
    List<Floor> findBySiteSiteIdOrderByFloorNumberAsc(Long siteId);
}
