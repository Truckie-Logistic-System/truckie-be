package capstone_project.dtos.request.order;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;

public record UpdateContractRuleRequest(
    String numOfVehicles,
    String note,
    String info1,
    String info2,
    @EnumValidator(enumClass = CommonStatusEnum.class, message = "Status must be one of: ACTIVE, INACTIVE, DELETED")
    String status,
    String sizeRuleId,
    String contractEntityId
) {
}
