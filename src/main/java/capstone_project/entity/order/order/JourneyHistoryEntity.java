package capstone_project.entity.order.order;

import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "journey_history", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class JourneyHistoryEntity extends BaseEntity {
    @Column(name = "start_location", precision = 10, scale = 8)
    private BigDecimal startLocation;

    @Column(name = "end_location", precision = 10, scale = 8)
    private BigDecimal endLocation;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "total_distance")
    private BigDecimal totalDistance;

    @Column(name = "is_reported_incident")
    private Boolean isReportedIncident;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity orderEntity;

}