package capstone_project.dtos.response.order.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SimpleTransactionResponse(
    String id,
    String paymentProvider,
    String gatewayOrderCode,
    BigDecimal amount,
    String currencyCode,
    String status,
    LocalDateTime paymentDate,
    String transactionType // DEPOSIT, FULL_PAYMENT, RETURN_SHIPPING
) {}