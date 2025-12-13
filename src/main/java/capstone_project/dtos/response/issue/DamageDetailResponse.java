package capstone_project.dtos.response.issue;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DamageDetailResponse {
    
    // Issue basic info
    @JsonProperty("issueId")
    private UUID issueId;
    
    @JsonProperty("issueType")
    private String issueType;
    
    @JsonProperty("issueStatus")
    private String issueStatus;
    
    @JsonProperty("description")
    private String description;
    
    @JsonProperty("evidenceImages")
    private List<String> evidenceImages;
    
    @JsonProperty("reportedAt")
    private LocalDateTime reportedAt;
    
    @JsonProperty("reportedBy")
    private String reportedBy;
    
    // Order context (readonly for staff)
    @JsonProperty("orderContext")
    private OrderContextInfo orderContext;
    
    // Policy info (readonly)
    @JsonProperty("policyInfo")
    private PolicyInfo policyInfo;
    
    // Damage assessment (if exists)
    @JsonProperty("damageAssessment")
    private DamageAssessmentInfo damageAssessment;
    
    // Refund info (if exists)
    @JsonProperty("refundInfo")
    private RefundInfo refundInfo;
    
    // Compensation breakdown (calculated)
    @JsonProperty("compensationBreakdown")
    private CompensationBreakdown compensationBreakdown;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderContextInfo {
        @JsonProperty("orderId")
        private UUID orderId;
        
        @JsonProperty("orderCode")
        private String orderCode;
        
        @JsonProperty("orderDetailId")
        private UUID orderDetailId;
        
        @JsonProperty("declaredValue")
        private BigDecimal declaredValue;
        
        @JsonProperty("hasInsurance")
        private Boolean hasInsurance;
        
        @JsonProperty("category")
        private String category;
        
        @JsonProperty("categoryDescription")
        private String categoryDescription;
        
        @JsonProperty("transportFee")
        private BigDecimal transportFee;
        
        @JsonProperty("customerName")
        private String customerName;
        
        @JsonProperty("customerPhone")
        private String customerPhone;
        
        // Weight info for Pro-rata calculation
        @JsonProperty("weight")
        private BigDecimal weight; // Trọng lượng kiện hư (tấn)
        
        @JsonProperty("totalWeight")
        private BigDecimal totalWeight; // Tổng trọng lượng đơn hàng (tấn)
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PolicyInfo {
        @JsonProperty("maxCompensationWithoutDocs")
        private BigDecimal maxCompensationWithoutDocs; // 10 * transportFee
        
        @JsonProperty("insuranceRate")
        private BigDecimal insuranceRate; // 0.0008 or 0.0015
        
        @JsonProperty("insuranceRatePercent")
        private String insuranceRatePercent; // "0.08%" or "0.15%"
        
        @JsonProperty("categoryMultiplier")
        private BigDecimal categoryMultiplier;
        
        @JsonProperty("policyDescription")
        private String policyDescription;
    }
    
    /**
     * Compensation breakdown theo công thức chuẩn:
     * B_tổng = B_hàng + C_hư
     * 
     * Trong đó:
     * - C_hư = C_total × (W_kiện / W_total) × T_hư (Pro-rata freight refund)
     * - B_hàng = min(V_lỗ, 10 × C_hư) nếu không bảo hiểm/không chứng từ
     * - B_hàng = min(V_lỗ, V_khai_báo) nếu có bảo hiểm + chứng từ
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CompensationBreakdown {
        @JsonProperty("goodsCompensation")
        private BigDecimal goodsCompensation; // B_hàng - Bồi thường hàng hóa
        
        @JsonProperty("freightRefund")
        private BigDecimal freightRefund; // C_hư - Hoàn cước Pro-rata
        
        @JsonProperty("totalCompensation")
        private BigDecimal totalCompensation; // B_tổng = B_hàng + C_hư
        
        @JsonProperty("legalLimit")
        private BigDecimal legalLimit; // 10 × C_hư (giới hạn trách nhiệm)
        
        @JsonProperty("compensationCase")
        private String compensationCase; // CASE1, CASE2, CASE3, CASE4
        
        @JsonProperty("explanation")
        private String explanation; // Giải thích chi tiết
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DamageAssessmentInfo {
        @JsonProperty("assessmentId")
        private UUID assessmentId;
        
        @JsonProperty("hasDocuments")
        private Boolean hasDocuments;
        
        @JsonProperty("documentValue")
        private BigDecimal documentValue;
        
        @JsonProperty("damageRate")
        private BigDecimal damageRate;
        
        @JsonProperty("damageRatePercent")
        private String damageRatePercent;
        
        @JsonProperty("compensationByPolicy")
        private BigDecimal compensationByPolicy;
        
        @JsonProperty("finalCompensation")
        private BigDecimal finalCompensation;
        
        @JsonProperty("staffNotes")
        private String staffNotes;
        
        @JsonProperty("createdAt")
        private LocalDateTime createdAt;
        
        @JsonProperty("updatedAt")
        private LocalDateTime updatedAt;
        
        @JsonProperty("createdByName")
        private String createdByName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundInfo {
        @JsonProperty("refundId")
        private UUID refundId;
        
        @JsonProperty("refundAmount")
        private BigDecimal refundAmount;
        
        @JsonProperty("bankName")
        private String bankName;
        
        @JsonProperty("accountNumber")
        private String accountNumber;
        
        @JsonProperty("accountHolderName")
        private String accountHolderName;
        
        @JsonProperty("transactionCode")
        private String transactionCode;
        
        @JsonProperty("bankTransferImage")
        private String bankTransferImage;
        
        @JsonProperty("refundDate")
        private LocalDateTime refundDate;
        
        @JsonProperty("notes")
        private String notes;
        
        @JsonProperty("processedByStaffName")
        private String processedByStaffName;
    }
}
