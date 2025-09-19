package capstone_project.controller.order;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.BillOfLandingResponse;
import capstone_project.service.services.billOfLanding.BillOfLandingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("${bill-of-lading.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class BillOfLandingController {

    private final BillOfLandingService billOfLandingService;

    @GetMapping("/{contractId}")
    public ResponseEntity<ApiResponse<BillOfLandingResponse>> getAllInformationForBillOfLanding(@PathVariable UUID contractId) {
        final var result = billOfLandingService.getBillOfLandingById(contractId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
