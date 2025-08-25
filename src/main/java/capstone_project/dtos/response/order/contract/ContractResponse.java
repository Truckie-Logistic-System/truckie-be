package capstone_project.dtos.response.order.contract;

public record ContractResponse (
    String id,
    String contractName,
    String effectiveDate,
    String expirationDate,
    String totalValue,
    String description,
    String attachFileUrl,
    String status,
    String orderId
) {
}
