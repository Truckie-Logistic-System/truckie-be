package capstone_project.entity.order.order;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

@Entity
@Table(name = "journey_segments", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class JourneySegmentEntity extends BaseEntity {
    @Column(name = "segment_order")
    private Integer segmentOrder;

    @Column(name = "start_point_name")
    private String startPointName;

    @Column(name = "end_point_name")
    private String endPointName;

    @Column(name = "start_latitude", precision = 11, scale = 8)
    private BigDecimal startLatitude;

    @Column(name = "start_longitude", precision = 11, scale = 8)
    private BigDecimal startLongitude;

    @Column(name = "end_latitude", precision = 11, scale = 8)
    private BigDecimal endLatitude;

    @Column(name = "end_longitude", precision = 11, scale = 8)
    private BigDecimal endLongitude;

    @Column(name = "distance_kilometers")
    private Integer distanceKilometers;

    @Column(name = "status") // PENDING, COMPLETED, ACTIVE
    private String status;

    @Column(name = "estimated_toll_fee")
    private Long estimatedTollFee;

    @Column(name = "path_coordinates_json", columnDefinition = "TEXT")
    private String pathCoordinatesJson;

    @Column(name = "toll_details_json", columnDefinition = "TEXT")
    private String tollDetailsJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journey_history_id", nullable = false)
    private JourneyHistoryEntity journeyHistory;
}
