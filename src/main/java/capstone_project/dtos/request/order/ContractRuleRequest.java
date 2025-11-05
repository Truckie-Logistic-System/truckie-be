package capstone_project.dtos.request.order;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ContractRuleRequest(

        @NotNull(message = "Num of vehicles cannot be null")
        @Min(value = 0L, message = "The num of vehicles must be positive")
        Integer numOfVehicles,
        String note,
        String info1,
        String info2,

        @NotNull(message = "Vehicle rule ID cannot be null")
        String sizeRuleId,

        @NotNull(message = "Contract entity ID cannot be null")
        String contractEntityId
) {
}
