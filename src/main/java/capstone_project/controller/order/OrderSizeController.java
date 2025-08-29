package capstone_project.controller.order;

import capstone_project.dtos.request.order.CreateOrderSizeRequest;
import capstone_project.dtos.request.order.UpdateOrderSizeRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.GetOrderSizeResponse;
import capstone_project.service.services.order.order.OrderSizeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${order-size.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderSizeController {
    private final OrderSizeService orderSizeService;

    // Create OrderSize
    @PostMapping
    public ResponseEntity<ApiResponse<GetOrderSizeResponse>> createOrderSize(
            @RequestBody CreateOrderSizeRequest request) {
        final var result = orderSizeService.createOrderSize(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Update OrderSize
    @PutMapping()
    public ResponseEntity<ApiResponse<GetOrderSizeResponse>> updateOrderSize(
            @RequestBody UpdateOrderSizeRequest request) {
        final var result = orderSizeService.updateOrderSize(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Delete OrderSize
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> deleteOrderSize(
            @PathVariable UUID id) {
        final var result = orderSizeService.deleteOrderSize(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get OrderSize by Id
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetOrderSizeResponse>> getOrderSizeById(
            @PathVariable UUID id) {
        final var result = orderSizeService.getOrderSizeById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get all OrderSizes
    @GetMapping
    public ResponseEntity<ApiResponse<List<GetOrderSizeResponse>>> getAllOrderSizes() {
        final var result = orderSizeService.getAllOrderSizes();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Active OrderSize
    @PutMapping("/{id}/active")
    public ResponseEntity<ApiResponse<Boolean>> activeOrderSize(
            @PathVariable UUID id) {
        final var result = orderSizeService.activeOrderSize(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
