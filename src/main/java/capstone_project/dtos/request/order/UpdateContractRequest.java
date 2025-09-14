package capstone_project.dtos.request.order;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;

public record UpdateContractRequest(
    String contractName,
    String effectiveDate,
    String expirationDate,
    String totalValue,
    String description,
    String attachFileUrl,
    @EnumValidator(enumClass = CommonStatusEnum.class, message = "Status must be one of: ACTIVE, INACTIVE, DELETED")
    String status,
    String orderId,
    String pricingRuleId
) {
}
