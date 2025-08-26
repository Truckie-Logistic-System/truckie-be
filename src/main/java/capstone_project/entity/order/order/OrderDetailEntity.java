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

@Entity
@Table(name = "order_details", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetailEntity extends BaseEntity {
    @Column(name = "weight")
    private BigDecimal weight;

    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Size(max = 100)
    @Column(name = "tracking_code", length = 100)
    private String trackingCode;

    @Column(name = "estimated_start_time")
    private LocalDateTime estimatedStartTime;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "estimated_end_time")
    private LocalDateTime estimatedEndTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "height")
    private BigDecimal height;

    @Column(name = "width")
    private BigDecimal width;

    @Column(name = "length")
    private BigDecimal length;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private OrderEntity orderEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_size_id")
    private OrderSizeEntity orderSizeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignmentEntity;

//    @ElementCollection
//    @CollectionTable(
//            name = "contract_rule_order_details",
//            joinColumns = @JoinColumn(name = "contract_rule_id")
//    )
//    @Column(name = "order_detail_id")
//    private Set<UUID> orderDetailIds = new HashSet<>();

}