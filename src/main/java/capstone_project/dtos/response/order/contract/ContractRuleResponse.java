package capstone_project.dtos.response.order.contract;

public record ContractRuleResponse(
    String id,
    String numOfVehicles,
    String note,
    String info1,
    String info2,
    String vehicleRuleId,
    String contractEntityId
) {
}
