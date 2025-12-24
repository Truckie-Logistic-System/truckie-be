package capstone_project.dtos.response.order.contract;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SimpleContractResponse(
    String id,
    String contractName,
    String effectiveDate,
    String expirationDate,
    BigDecimal totalValue,
    BigDecimal adjustedValue,
    String description,
    String attachFileUrl,
    String status,
    String staffName,
    String paymentBreakdownSnapshot  // JSON string containing PaymentBreakdownSnapshot
) {}