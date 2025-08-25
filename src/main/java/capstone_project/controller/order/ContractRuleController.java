package capstone_project.controller.order;

import capstone_project.dtos.request.order.ContractRuleRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.contract.ContractRuleResponse;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.service.services.order.order.ContractRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${contract-rule.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ContractRuleController {

    private final ContractRuleService contractRuleService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<ContractRuleResponse>>> getAllContractRules() {
        final var result = contractRuleService.getContracts();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractRuleResponse>> getContractRuleById(@PathVariable UUID id) {
        final var result = contractRuleService.getContractById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<ContractRuleResponse>> createContractRule(@Valid @RequestBody ContractRuleRequest contractRuleRequest) {
        final var result = contractRuleService.createContract(contractRuleRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/list")
    public ResponseEntity<ApiResponse<ListContractRuleAssignResult>> createListContractRules(@RequestBody
                                                                                           @Valid List<ContractRuleRequest> contractRuleRequests) {
        final var result = contractRuleService.createListContractRules(contractRuleRequests);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractRuleResponse>> updateContractRule(@PathVariable UUID id, @RequestBody @Valid ContractRuleRequest contractRuleRequest) {
        final var result = contractRuleService.updateContract(id, contractRuleRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

}
