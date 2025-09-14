package capstone_project.dtos.response.order.contract;

public record ContractResponse (
    String id,
    String contractName,
    String effectiveDate,
    String expirationDate,
    String totalValue,
    String supportedValue,
    String description,
    String attachFileUrl,
    String status,
    String orderId,
    String staffId
) {
}
