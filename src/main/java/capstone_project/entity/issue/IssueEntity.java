package capstone_project.entity.issue;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.order.order.SealEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.entity.order.transaction.TransactionEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "issues", schema = "public", catalog = "capstone-project")
@Data
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class    IssueEntity extends BaseEntity {
    @Size(max = 200)
    @Column(name = "description", length = 200)
    private String description;

    @Column(name = "location_latitude", precision = 11, scale = 8)
    private BigDecimal locationLatitude;

    @Column(name = "location_longitude", precision = 11, scale = 8)
    private BigDecimal locationLongitude;

    @Size(max = 20)
    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    // ✅ CRITICAL: Changed from VARCHAR(20) to VARCHAR(500) to support JSON format
    // Format: {"orderDetailId1":"STATUS1","orderDetailId2":"STATUS2"}
    // This is needed for combined issue reports where different packages have different statuses
    @Size(max = 500)
    @Column(name = "trip_status_at_report", length = 500)
    private String tripStatusAtReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_assignment_id")
    private VehicleAssignmentEntity vehicleAssignmentEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_type_id")
    private IssueTypeEntity issueTypeEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private UserEntity staff;

    // Seal replacement fields
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_seal_id")
    private SealEntity oldSeal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_seal_id")
    private SealEntity newSeal;

    @Size(max = 500)
    @Column(name = "seal_removal_image", length = 500)
    private String sealRemovalImage;

    @Size(max = 500)
    @Column(name = "new_seal_attached_image", length = 500)
    private String newSealAttachedImage;

    @Column(name = "new_seal_confirmed_at")
    private LocalDateTime newSealConfirmedAt;

    @OneToMany(mappedBy = "issueEntity", fetch = FetchType.LAZY)
    private List<OrderDetailEntity> orderDetails;

    @OneToMany(mappedBy = "issueEntity", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private List<IssueImageEntity> issueImages;

    // ===== ORDER_REJECTION specific fields =====
    
    @Column(name = "return_shipping_fee", precision = 19, scale = 2)
    private BigDecimal returnShippingFee; 
    
    @Column(name = "adjusted_return_fee", precision = 19, scale = 2)
    private BigDecimal adjustedReturnFee; 
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "return_journey_id")
    private JourneyHistoryEntity returnJourney; 
    
    @Column(name = "payment_deadline")
    private LocalDateTime paymentDeadline;
    
    // Refund relationship for ORDER_REJECTION return payment
    @OneToOne(mappedBy = "issueEntity", fetch = FetchType.LAZY)
    private capstone_project.entity.order.order.RefundEntity refund;
    
    // Transactions for ORDER_REJECTION return payment (can have multiple: PENDING, FAILED, PAID)
    // Note: TransactionEntity uses issueId field, not @ManyToOne, so this is a helper method relationship
    @Transient
    private List<TransactionEntity> returnTransactions;
    
    // ===== REROUTE specific fields =====
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "affected_segment_id")
    private capstone_project.entity.order.order.JourneySegmentEntity affectedSegment; // Segment gặp sự cố
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rerouted_journey_id")
    private JourneyHistoryEntity reroutedJourney; // Journey mới sau khi tái định tuyến

    // ===== DAMAGE compensation specific fields =====
    
    // Tỷ lệ hư hỏng (%) - do staff nhập sau khi thẩm định
    @Column(name = "damage_assessment_percent", precision = 5, scale = 2)
    private BigDecimal damageAssessmentPercent;
    
    // Khách hàng có cung cấp chứng từ hợp lệ không (hóa đơn VAT, chứng từ mua bán)
    // Lưu ý: Chứng từ được cung cấp bên ngoài hệ thống, chỉ ghi nhận có/không
    @Column(name = "damage_has_documents")
    private Boolean damageHasDocuments;
    
    // Giá trị khai báo theo chứng từ (VNĐ) - dùng khi có chứng từ
    @Column(name = "damage_declared_value", precision = 19, scale = 2)
    private BigDecimal damageDeclaredValue;
    
    // Giá trị ước tính theo thị trường (VNĐ) - dùng khi không có chứng từ
    @Column(name = "damage_estimated_market_value", precision = 19, scale = 2)
    private BigDecimal damageEstimatedMarketValue;
    
    // Cước phí vận chuyển (VNĐ) - lấy từ Contract (adjustedValue hoặc totalValue)
    @Column(name = "damage_freight_fee", precision = 19, scale = 2)
    private BigDecimal damageFreightFee;
    
    // Giới hạn pháp lý = 10 × cước phí (VNĐ)
    @Column(name = "damage_legal_limit", precision = 19, scale = 2)
    private BigDecimal damageLegalLimit;
    
    // Thiệt hại ước tính = giá trị × tỷ lệ hư hỏng (VNĐ)
    @Column(name = "damage_estimated_loss", precision = 19, scale = 2)
    private BigDecimal damageEstimatedLoss;
    
    // Mức bồi thường theo đúng policy (VNĐ)
    @Column(name = "damage_policy_compensation", precision = 19, scale = 2)
    private BigDecimal damagePolicyCompensation;
    
    // Mức bồi thường cuối cùng (VNĐ) - có thể khác policy nếu có điều chỉnh
    @Column(name = "damage_final_compensation", precision = 19, scale = 2)
    private BigDecimal damageFinalCompensation;
    
    // Kịch bản bồi thường: CASE1_HAS_INS_HAS_DOC, CASE2_HAS_INS_NO_DOC, CASE3_NO_INS_HAS_DOC, CASE4_NO_INS_NO_DOC
    @Size(max = 50)
    @Column(name = "damage_compensation_case", length = 50)
    private String damageCompensationCase;
    
    // Lý do điều chỉnh nếu final != policy
    @Size(max = 500)
    @Column(name = "damage_adjust_reason", length = 500)
    private String damageAdjustReason;
    
    // Ghi chú xử lý nội bộ
    @Size(max = 1000)
    @Column(name = "damage_handler_note", length = 1000)
    private String damageHandlerNote;
    
    // Trạng thái xử lý bồi thường: PENDING_ASSESSMENT, PROPOSED, APPROVED, REJECTED
    @Size(max = 30)
    @Column(name = "damage_compensation_status", length = 30)
    private String damageCompensationStatus;

}