package capstone_project.controller.order;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.ContractPdfResponse;
import capstone_project.service.services.order.order.impl.OrderPdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${pdf.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderPdfController {

    private final OrderPdfService orderPdfService;

    @PostMapping("/{contractId}/generate-pdf")
    public ResponseEntity<ApiResponse<ContractPdfResponse>> generateOrderPdf(@PathVariable UUID contractId) {
        ContractPdfResponse response = orderPdfService.generateAndUploadContractPdf(contractId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}