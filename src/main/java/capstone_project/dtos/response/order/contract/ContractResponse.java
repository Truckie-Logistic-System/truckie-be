package capstone_project.dtos.response.order.contract;

import java.math.BigDecimal;

public record ContractResponse (
    String id,
    String contractName,
    String effectiveDate,
    String expirationDate,
    BigDecimal totalValue,
    BigDecimal adjustedValue,
    BigDecimal customDepositPercent,
    String description,
    String attachFileUrl,
    String status,
    String orderId,
    String staffId
) {
}
