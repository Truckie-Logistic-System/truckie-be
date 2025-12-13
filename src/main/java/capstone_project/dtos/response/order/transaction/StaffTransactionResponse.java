package capstone_project.dtos.response.order.transaction;

import capstone_project.entity.order.transaction.TransactionEntity;
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
public class StaffTransactionResponse {
    private UUID id;
    private String transactionType;
    private BigDecimal amount;
    private String status;
    private String paymentProvider;
    private String currencyCode;
    private String gatewayOrderCode;
    private LocalDateTime paymentDate;
    private LocalDateTime createdAt;
    private String gatewayResponse;
    
    // Nested contract information for banner
    private ContractInfo contract;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractInfo {
        private UUID id;
        private String contractName;
        private String status;
        private String orderCode;
        private String orderStatus;
        private String customerName;
        private BigDecimal adjustedValue;
        private BigDecimal totalValue;
        private String attachFileUrl;
    }
    
    // For list view (without full contract details)
    public static StaffTransactionResponse fromList(TransactionEntity transaction) {
        return StaffTransactionResponse.builder()
                .id(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .paymentProvider(transaction.getPaymentProvider())
                .currencyCode(transaction.getCurrencyCode())
                .gatewayOrderCode(transaction.getGatewayOrderCode())
                .paymentDate(transaction.getPaymentDate())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}
