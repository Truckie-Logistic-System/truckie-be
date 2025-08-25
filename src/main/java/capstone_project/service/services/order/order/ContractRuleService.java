package capstone_project.service.services.order.order;

import capstone_project.dtos.request.order.ContractRuleRequest;
import capstone_project.dtos.response.order.contract.ContractRuleResponse;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;

import java.util.List;
import java.util.UUID;

public interface ContractRuleService {
    List<ContractRuleResponse> getContracts();

    ContractRuleResponse getContractById(UUID id);

    ContractRuleResponse createContract(ContractRuleRequest contractRuleRequest);

    ListContractRuleAssignResult createListContractRules(List<ContractRuleRequest> contractRuleRequests);

    ContractRuleResponse updateContract(UUID id, ContractRuleRequest contractRuleRequest);

    void deleteContract(UUID id);


}
