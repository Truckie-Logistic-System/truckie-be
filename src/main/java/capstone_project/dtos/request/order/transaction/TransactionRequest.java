package capstone_project.dtos.request.order.transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionRequest(

        String paymentProvider,
        BigDecimal amount,
        String currencyCode,
        String gatewayResponse,
        LocalDateTime paymentDate,

        String contractId
) {
}
