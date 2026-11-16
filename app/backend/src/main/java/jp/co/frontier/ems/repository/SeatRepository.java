package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.Seat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 座席マスタ。API 4.3 / 7.5 */
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /** 指定フロアの座席を座席番号順に取得する（API 4.3）。 */
    List<Seat> findByFloorFloorIdOrderBySeatNumberAsc(Long floorId);
}
