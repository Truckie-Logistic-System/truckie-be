package capstone_project.service.services.order.order;

import capstone_project.dtos.request.order.ContractRequest;
import capstone_project.dtos.request.order.CreateContractForCusRequest;
import capstone_project.dtos.request.order.contract.ContractFileUploadRequest;
import capstone_project.dtos.response.order.contract.BothOptimalAndRealisticAssignVehiclesResponse;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.order.contract.PriceCalculationResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.user.address.AddressEntity;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ContractService {
    List<ContractResponse> getAllContracts();

    ContractResponse getContractById(UUID id);

    ContractResponse getContractByOrderId(UUID orderId);

    ContractResponse createContract(ContractRequest contractRequest);

    ContractResponse createBothContractAndContractRule(ContractRequest contractRequest);

    ContractResponse createBothContractAndContractRuleForCus(CreateContractForCusRequest contractRequest);

    BothOptimalAndRealisticAssignVehiclesResponse getBothOptimalAndRealisticAssignVehiclesResponse(UUID orderId);

    ContractResponse updateContract(UUID id, ContractRequest contractRequest);

    List<ContractRuleAssignResponse> assignVehiclesOptimal(UUID orderId);

    PriceCalculationResponse calculateTotalPrice(ContractEntity contract,
                                                 BigDecimal distanceKm,
                                                 Map<UUID, Integer> vehicleCountMap);

    BigDecimal calculateDistanceKm(AddressEntity from, AddressEntity to);

    ContractResponse uploadContractFile(ContractFileUploadRequest contractFileUploadRequest) throws IOException;

    void deleteContractByOrderId(UUID orderId);

    List<ContractRuleAssignResponse> assignVehiclesWithAvailability(UUID orderId);
}
