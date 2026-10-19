package jp.co.frontier.ems.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * フロア。
 *
 * <p>同一拠点に同じ階は作れない（UNIQUE(site_id, floor_number)）。
 */
@Entity
@Table(name = "floors")
@Getter
@Setter
@NoArgsConstructor
public class Floor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "floor_id")
    private Long floorId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    @Column(name = "floor_name", nullable = false, length = 50)
    private String floorName;

    /** 階数。並べ替え用。地下は負数 */
    @Column(name = "floor_number", nullable = false)
    private Short floorNumber;

    @Column(name = "map_image_url", length = 500)
    private String mapImageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
