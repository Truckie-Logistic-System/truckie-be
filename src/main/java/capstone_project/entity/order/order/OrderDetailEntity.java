package capstone_project.entity.order.order;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.issue.IssueEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
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
    @DecimalMin(value = "0.01", message = "Trọng lượng kiện hàng phải tối thiểu 0.01 tấn")
    @DecimalMax(value = "10.0", message = "Trọng lượng kiện hàng không được vượt quá 10 tấn")
    @Column(name = "weight_tons", precision = 19, scale = 6)
    private BigDecimal weightTons;

    @Column(name = "weight_base_unit")
    private BigDecimal weightBaseUnit;

    @Column(name = "unit")
    private String unit;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private OrderEntity orderEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_size_id")
    private OrderSizeEntity orderSizeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignmentEntity;

    // Link to issue if this order detail is damaged/has problem
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private IssueEntity issueEntity;

    // Giá trị khai báo của kiện hàng (VNĐ) - dùng để tính phí bảo hiểm
    @DecimalMin(value = "0", message = "Giá trị khai báo không được âm")
    @Column(name = "declared_value", precision = 19, scale = 2)
    private BigDecimal declaredValue;

//    @ElementCollection
//    @CollectionTable(
//            name = "contract_rule_order_details",
//            joinColumns = @JoinColumn(name = "contract_rule_id")
//    )
//    @Column(name = "order_detail_id")
//    private Set<UUID> orderDetailIds = new HashSet<>();

}