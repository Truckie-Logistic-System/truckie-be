package capstone_project.entity.order.order;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "journey_history", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class JourneyHistoryEntity extends BaseEntity {
    @Column(name = "journey_name")
    private String journeyName;

    @Column(name = "journey_type") // INITIAL, REROUTED
    private String journeyType;

    @Column(name = "status") // ACTIVE, COMPLETED, CANCELLED
    private String status;

    @Column(name = "reason_for_reroute")
    private String reasonForReroute;

    @Column(name = "total_toll_fee")
    private Long totalTollFee;

    @Column(name = "total_toll_count")
    private Integer totalTollCount;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignment;

    @OneToMany(mappedBy = "journeyHistory", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JourneySegmentEntity> journeySegments = new ArrayList<>();
}