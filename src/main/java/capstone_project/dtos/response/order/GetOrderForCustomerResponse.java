package capstone_project.dtos.response.order;

import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import capstone_project.dtos.response.issue.GetIssueImageResponse;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleResponse;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record GetOrderForCustomerResponse(
        GetOrderResponse getOrderResponse,
        List<GetIssueImageResponse> getIssueImageResponse,
        Map<UUID,List<PhotoCompletionResponse>> photoCompletionResponse,
        ContractResponse contractResponse,
        List<TransactionResponse> transactionResponse
) {
}
