package capstone_project.controller.seal;

import capstone_project.dtos.request.order.seal.OrderSealRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.seal.GetOrderSealResponse;
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

    @PostMapping(value = "/confirm-seal-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GetOrderSealResponse>> confirmSealAttachment(
            @RequestParam("vehicleAssignmentId") UUID vehicleAssignmentId,
            @RequestParam("sealImage") MultipartFile sealImage,
            @RequestParam("sealCode") String sealCode) {
        OrderSealRequest request = new OrderSealRequest(vehicleAssignmentId, sealImage, sealCode);
        final var result = orderSealService.confirmSealAttachment(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/{sealId}")
    public ResponseEntity<ApiResponse<GetOrderSealResponse>> removeSealBySealId(
            @PathVariable UUID sealId) {
        final var result = orderSealService.removeSealBySealId(sealId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-all/{sealId}")
    public ResponseEntity<ApiResponse<GetOrderSealResponse>> getAllBySealId(
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
