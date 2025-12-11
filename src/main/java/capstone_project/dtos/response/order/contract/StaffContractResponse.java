package capstone_project.dtos.response.order.contract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for staff contract management with full details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffContractResponse {
    // Contract basic info
    private UUID id;
    private String contractName;
    private String status;
    private String description;
    private String attachFileUrl;
    
    // Dates
    private LocalDateTime createdAt;
    private LocalDateTime effectiveDate;
    private LocalDateTime expirationDate;
    private LocalDateTime signingDeadline;
    private LocalDateTime depositPaymentDeadline;
    private LocalDateTime fullPaymentDeadline;
    
    // Values
    private BigDecimal totalValue;
    private BigDecimal adjustedValue;
    private BigDecimal effectiveValue; // adjustedValue if > 0, else totalValue
    
    // Order info
    private OrderInfo order;
    
    // Staff info
    private StaffInfo staff;
    
    // Transactions
    private List<TransactionInfo> transactions;
    
    // Computed fields
    private BigDecimal paidAmount;
    private BigDecimal remainingAmount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderInfo {
        private UUID id;
        private String orderCode;
        private String status;
        private String senderName;
        private String senderPhone;
        private String receiverName;
        private String receiverPhone;
        private String pickupAddress;
        private String deliveryAddress;
        private LocalDateTime createdAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffInfo {
        private UUID id;
        private String fullName;
        private String email;
        private String phoneNumber;
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
        private String currencyCode;
        private LocalDateTime paymentDate;
        private LocalDateTime createdAt;
    }
}
