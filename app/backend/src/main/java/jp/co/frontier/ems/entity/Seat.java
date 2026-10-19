package jp.co.frontier.ems.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 座席。
 *
 * <p>空席かどうかは {@link Employee#getSeat()} 側を参照して判定する。
 * seats 側に在席フラグは持たない（二重管理を避けるため）。
 */
@Entity
@Table(name = "seats")
@Getter
@Setter
@NoArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seat_id")
    private Long seatId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "floor_id", nullable = false)
    private Floor floor;

    @Column(name = "seat_number", nullable = false, length = 50)
    private String seatNumber;

    /** マップ画像上のX座標（px） */
    @Column(name = "position_x", nullable = false)
    private Integer positionX;

    /** マップ画像上のY座標（px） */
    @Column(name = "position_y", nullable = false)
    private Integer positionY;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
