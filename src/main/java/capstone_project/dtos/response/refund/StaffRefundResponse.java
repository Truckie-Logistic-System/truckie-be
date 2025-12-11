package capstone_project.dtos.response.refund;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffRefundResponse {
    private UUID id;
    private BigDecimal refundAmount;
    private String bankTransferImage;
    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private String transactionCode;
    private LocalDateTime refundDate;
    private String notes;
    private String sourceType;
    private LocalDateTime createdAt;
    
    // Staff who processed the refund
    private StaffInfo processedByStaff;
    
    // Issue information
    private IssueInfo issue;
    
    // Order information (from issue -> vehicleAssignment -> orderDetails -> order)
    private OrderInfo order;
    
    // Vehicle Assignment information
    private VehicleAssignmentInfo vehicleAssignment;
    
    // Transaction information (if any)
    private TransactionInfo transaction;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffInfo {
        private UUID id;
        private String fullName;
        private String email;
        private String phone;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueInfo {
        private UUID id;
        private String issueTypeName;
        private String issueCategory;
        private String description;
        private String status;
        private LocalDateTime reportedAt;
        private LocalDateTime resolvedAt;
        private BigDecimal damageFinalCompensation;
        private String damageCompensationStatus;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderInfo {
        private UUID id;
        private String orderCode;
        private String status;
        private String senderName;
        private String receiverName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VehicleAssignmentInfo {
        private UUID id;
        private String trackingCode;
        private String status;
        private String vehiclePlateNumber;
        private String driverName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransactionInfo {
        private UUID id;
        private String transactionType;
        private BigDecimal amount;
        private String status;
        private String paymentProvider;
        private String gatewayOrderCode;
        private LocalDateTime paymentDate;
    }
}
