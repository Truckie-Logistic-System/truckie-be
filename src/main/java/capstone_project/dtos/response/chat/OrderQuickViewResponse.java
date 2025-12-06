package capstone_project.dtos.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for quick view of order details in chat
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderQuickViewResponse {
    
    // Order info
    private UUID orderId;
    private String orderCode;
    private String status;
    private String notes;
    private Integer totalQuantity;
    private LocalDateTime createdAt;
    
    // Customer info
    private String customerName;
    private String customerPhone;
    private String companyName;
    
    // Receiver info
    private String receiverName;
    private String receiverPhone;
    private String receiverIdentity;
    
    // Address info
    private String pickupAddress;
    private String deliveryAddress;
    
    // Package info
    private String packageDescription;
    private String categoryName;
    private String categoryDescription;
    private Boolean hasInsurance;
    private BigDecimal totalDeclaredValue;
    
    // Contract info
    private ContractInfo contract;
    
    // Order details (packages)
    private List<OrderDetailInfo> orderDetails;
    
    // Vehicle assignment info
    private List<VehicleAssignmentInfo> vehicleAssignments;
    
    // Issues
    private List<IssueInfo> issues;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractInfo {
        private UUID contractId;
        private String contractCode;
        private String status;
        private BigDecimal totalAmount;
        private BigDecimal depositAmount;
        private BigDecimal paidAmount;
        private LocalDateTime signedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetailInfo {
        private UUID id;
        private String trackingCode;
        private String status;
        private String description;
        private BigDecimal weight;
        private BigDecimal weightBaseUnit;
        private String unit;
        private BigDecimal length;
        private BigDecimal width;
        private BigDecimal height;
        private BigDecimal declaredValue;
        private Boolean isFragile;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleAssignmentInfo {
        private UUID id;
        private String trackingCode;
        private String status;
        private String vehicleType;
        private String driverName;
        private String driverPhone;
        private String licensePlate;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueInfo {
        private UUID id;
        private String issueType;
        private String description;
        private String status;
        private String resolution;
        private LocalDateTime createdAt;
        private LocalDateTime resolvedAt;
    }
}
