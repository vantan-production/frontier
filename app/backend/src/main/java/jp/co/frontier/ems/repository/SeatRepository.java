package jp.co.frontier.ems.repository;

import jp.co.frontier.ems.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

/** 座席マスタ。API 4.3 / 7.5 */
public interface SeatRepository extends JpaRepository<Seat, Long> {
}
