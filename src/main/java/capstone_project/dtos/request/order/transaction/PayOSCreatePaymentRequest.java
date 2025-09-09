package capstone_project.dtos.request.order.transaction;

import java.math.BigDecimal;

public record PayOSCreatePaymentRequest(
        String orderCode,
        BigDecimal amount,
        String currencyCode,
        String description,
        String returnUrl,
        String cancelUrl
//        String notifyUrl
) {
}
