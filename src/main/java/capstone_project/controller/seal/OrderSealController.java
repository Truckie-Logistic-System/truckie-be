package capstone_project.controller.seal;

import capstone_project.dtos.request.order.seal.OrderSealRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.seal.GetOrderSealResponse;
import capstone_project.dtos.response.order.seal.GetSealFullResponse;
import capstone_project.service.services.order.seal.OrderSealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("${order-seal.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class OrderSealController {
    private final OrderSealService orderSealService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GetSealFullResponse>> assignSealForVehicleAssignment(
            @RequestParam("vehicleAssignmentId") UUID vehicleAssignmentId,
            @RequestParam("sealImage") MultipartFile sealImage) {
        OrderSealRequest request = new OrderSealRequest(vehicleAssignmentId, sealImage);
        final var result = orderSealService.assignSealForVehicleAssignment(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{sealId}")
    public ResponseEntity<ApiResponse<GetSealFullResponse>> removeSealBySealId(
            @PathVariable UUID sealId) {
        final var result = orderSealService.removeSealBySealId(sealId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-all/{sealId}")
    public ResponseEntity<ApiResponse<GetSealFullResponse>> getAllBySealId(
            @PathVariable UUID sealId) {
        final var result = orderSealService.getAllBySealId(sealId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-active-by-assignment-id/{vehicleAssignmentId}")
    public ResponseEntity<ApiResponse<GetOrderSealResponse>> getActiveOrderSealByVehicleAssignmentId(
            @PathVariable UUID vehicleAssignmentId) {
        final var result = orderSealService.getActiveOrderSealByVehicleAssignmentId(vehicleAssignmentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
