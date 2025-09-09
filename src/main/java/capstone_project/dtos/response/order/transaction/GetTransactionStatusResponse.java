package capstone_project.dtos.response.order.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GetTransactionStatusResponse(

        String id,
        String status,
        BigDecimal amount,
        String currencyCode,
        String paymentProvider,
        Long gatewayOrderCode,
        LocalDateTime modifiedAt
) {
}
