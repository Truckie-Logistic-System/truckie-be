package capstone_project.controller.seal;

import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.dtos.request.order.CreateOrderAndDetailRequest;
import capstone_project.dtos.request.order.seal.OrderDetailSealRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.CreateOrderResponse;
import capstone_project.dtos.response.order.seal.GetOrderDetailSealResponse;
import capstone_project.dtos.response.order.seal.GetSealFullResponse;
import capstone_project.service.services.order.seal.OrderDetailSealService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${order-detail-seal.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class OrderDetailSealController {
    private final OrderDetailSealService orderDetailSealService;

    @PostMapping()
    public ResponseEntity<ApiResponse<GetSealFullResponse>> assignAFirstSealForOrderDetail(@Valid @RequestBody OrderDetailSealRequest request) {
        final var result = orderDetailSealService.assignAFirstSealForOrderDetail(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{sealId}")
    public ResponseEntity<ApiResponse<GetSealFullResponse>> removeSealForDetailsBySealId(
            @PathVariable UUID sealId) {
        final var result = orderDetailSealService.removeSealForDetailsBySealId(sealId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-all/{sealId}")
    public ResponseEntity<ApiResponse<GetSealFullResponse>> getAllBySealId(
            @PathVariable UUID sealId) {
        final var result = orderDetailSealService.getAllBySealId(sealId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-active-by-detail-id/{orderDetailId}")
    public ResponseEntity<ApiResponse<GetOrderDetailSealResponse>> getActiveOrderSealByOrderDetailId(
            @PathVariable UUID orderDetailId) {
        final var result = orderDetailSealService.getActiveOrderSealByOrderDetailId(orderDetailId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
