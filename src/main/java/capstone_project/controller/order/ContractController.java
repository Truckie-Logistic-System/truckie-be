package capstone_project.controller.order;

import capstone_project.dtos.request.order.ContractRequest;
import capstone_project.dtos.request.order.CreateContractForCusRequest;
import capstone_project.dtos.request.order.contract.ContractFileUploadRequest;
import capstone_project.dtos.request.order.contract.GenerateContractPdfRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.contract.BothOptimalAndRealisticAssignVehiclesResponse;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.service.services.order.order.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
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

    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<ContractResponse>> getContractByOrderId(@PathVariable UUID orderId) {
        final var result = contractService.getContractByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /*
    * API này chỉ tạo hợp đồng rỗng (trong đó chưa có rule nào được áp dụng) --> chưa tính được tiền
    * qua sử dụng API createListContractRules
    * */
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

    @PostMapping("/both/for-cus")
    public ResponseEntity<ApiResponse<ContractResponse>> createBothContractAndContractRuleForCus(@RequestBody @Valid CreateContractForCusRequest contractRequest) {
        final var result = contractService.createBothContractAndContractRuleForCus(contractRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractResponse>> updateContract(@PathVariable UUID id, @RequestBody @Valid ContractRequest contractRequest) {
        final var result = contractService.updateContract(id, contractRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/{orderId}")
    public void deleteContract(@PathVariable UUID orderId) {
        contractService.deleteContractByOrderId(orderId);
    }

    /*
    * API này dùng để suggest, nếu thấy suggest hợp lý, thì sử dụng createBothContractAndContractRule (/both) để tạo hợp đồng luôn
    * */
    @GetMapping("{orderId}/suggest-assign-vehicles")
    public ResponseEntity<ApiResponse<List<ContractRuleAssignResponse>>> assignVehicles(@PathVariable UUID orderId) {
        final var result = contractService.assignVehiclesOptimal(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("{orderId}/get-both-optimal-and-realistic-assign-vehicles")
    public ResponseEntity<ApiResponse<BothOptimalAndRealisticAssignVehiclesResponse>> getBothOptimalAndRealisticAssignVehiclesResponse(@PathVariable UUID orderId) {
        // DEBUG: Bypass logging config
        System.out.println("🚨 DEBUG: ContractController.getBothOptimalAndRealisticAssignVehiclesResponse() CALLED with orderId: " + orderId);
        System.out.println("🚨 DEBUG: Thread: " + Thread.currentThread().getName());
        System.out.println("🚨 DEBUG: Timestamp: " + java.time.LocalDateTime.now());
        
        final var result = contractService.getBothOptimalAndRealisticAssignVehiclesResponse(orderId);
        
        System.out.println("🚨 DEBUG: ContractController completed successfully");
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping(path = "/upload-contract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ContractResponse>>  uploadFile(ContractFileUploadRequest contractFileUploadRequest) throws IOException {
        final var result = contractService.uploadContractFile(contractFileUploadRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Generate contract PDF on server-side and upload to Cloudinary
     * This endpoint handles PDF generation with proper pagination (no content truncation)
     * Frontend should call this instead of generating PDF locally
     */
    @PostMapping("/generate-and-save-pdf")
    public ResponseEntity<ApiResponse<ContractResponse>> generateAndSavePdf(@RequestBody @Valid GenerateContractPdfRequest request) {
        final var result = contractService.generateAndSaveContractPdf(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

}
