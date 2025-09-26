package capstone_project.dtos.response.order.contract;

import java.util.List;

public record BothOptimalAndRealisticAssignVehiclesResponse(
        List<ContractRuleAssignResponse> optimal,
        List<ContractRuleAssignResponse> realistic
) {
}
