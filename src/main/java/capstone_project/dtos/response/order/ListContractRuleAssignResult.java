package capstone_project.dtos.response.order;

import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import lombok.Builder;

import java.util.List;
import java.util.UUID;

@Builder
public record ListContractRuleAssignResult(
        List<ContractRuleAssignResponse> vehicleAssignments,
        List<UUID> unassignedDetails,
        List<String> errors
) {}
