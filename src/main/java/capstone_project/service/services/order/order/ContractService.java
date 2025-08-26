package capstone_project.service.services.order.order;

import capstone_project.dtos.request.order.ContractRequest;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.user.address.AddressEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ContractService {
    List<ContractResponse> getAllContracts();

    ContractResponse getContractById(UUID id);

    ContractResponse createContract(ContractRequest contractRequest);

    ContractResponse createBothContractAndContractRule(ContractRequest contractRequest);

    ContractResponse updateContract(UUID id, ContractRequest contractRequest);

    List<ContractRuleAssignResponse> assignVehicles(UUID orderId);

    BigDecimal calculateTotalPrice(ContractEntity contract,
                                   BigDecimal distanceKm,
                                   Map<UUID, Integer> vehicleCountMap);

    BigDecimal calculateDistanceKm(AddressEntity from, AddressEntity to);

    void deleteContract(UUID id);
}
