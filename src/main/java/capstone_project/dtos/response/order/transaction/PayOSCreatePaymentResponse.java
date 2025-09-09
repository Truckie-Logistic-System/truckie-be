package capstone_project.dtos.response.order.transaction;

import java.math.BigDecimal;

public record PayOSCreatePaymentResponse(
        String transactionId,
        String checkoutUrl,
        String status,
        BigDecimal amount,
        String currencyCode,
        String message
) {
}
