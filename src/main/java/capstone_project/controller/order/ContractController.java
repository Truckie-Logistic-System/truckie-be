package capstone_project.controller.order;

import capstone_project.dtos.request.order.ContractRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.service.services.order.order.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${contract.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ContractController {

    private final ContractService contractService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<ContractResponse>>> getAllContracts() {
        final var result = contractService.getAllContracts();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractResponse>> getContractById(@PathVariable UUID id) {
        final var result = contractService.getContractById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<ContractResponse>> createContract(@RequestBody @Valid ContractRequest contractRequest) {
        final var result = contractService.createContract(contractRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/both")
    public ResponseEntity<ApiResponse<ContractResponse>> createBothContractAndContractRule(@RequestBody @Valid ContractRequest contractRequest) {
        final var result = contractService.createBothContractAndContractRule(contractRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractResponse>> updateContract(@PathVariable UUID id, @RequestBody @Valid ContractRequest contractRequest) {
        final var result = contractService.updateContract(id, contractRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("{orderId}/assign-vehicles")
    public ResponseEntity<ApiResponse<List<ContractRuleAssignResponse>>> assignVehicles(@PathVariable UUID orderId) {
        final var result = contractService.assignVehicles(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

}
