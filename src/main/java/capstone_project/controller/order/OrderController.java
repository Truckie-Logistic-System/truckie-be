package capstone_project.controller.order;

import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.dtos.request.order.CreateOrderAndDetailRequest;
import capstone_project.dtos.request.order.UpdateOrderRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.CreateOrderResponse;
import capstone_project.service.services.order.order.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${order.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService orderService;

    @PostMapping()
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrderAndOrderDetail(@Valid @RequestBody CreateOrderAndDetailRequest request) {
        final var result = orderService.createOrder(request.orderRequest(), request.orderDetails());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Change status cho Order (một status)
    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> changeAStatusOrder(
            @PathVariable UUID orderId,
            @RequestParam OrderStatusEnum status) {
        final var result = orderService.changeAStatusOrder(orderId, status);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Change status cho Order và toàn bộ OrderDetail
    @PutMapping("/{orderId}/status/all")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> changeStatusOrderWithAllOrderDetail(
            @PathVariable UUID orderId,
            @RequestParam OrderStatusEnum status) {
        final var result = orderService.changeStatusOrderWithAllOrderDetail(orderId, status);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Lấy order theo senderId
    @GetMapping("/sender/{senderId}")
    public ResponseEntity<ApiResponse<List<CreateOrderResponse>>> getOrdersBySenderId(
            @PathVariable UUID senderId) {
        final var result = orderService.getCreateOrderRequestsBySenderId(senderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Lấy order theo deliveryAddressId
    @GetMapping("/delivery-address/{deliveryAddressId}")
    public ResponseEntity<ApiResponse<List<CreateOrderResponse>>> getOrdersByDeliveryAddressId(
            @PathVariable UUID deliveryAddressId) {
        final var result = orderService.getCreateOrderRequestsByDeliveryAddressId(deliveryAddressId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping()
    public ResponseEntity<ApiResponse<CreateOrderResponse>> updateOrderBasicInPendingOrProcessing(
            @RequestBody UpdateOrderRequest request) {
        final var result = orderService.updateOrderBasicInPendingOrProcessing(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Lấy order theo deliveryAddressId
    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<CreateOrderResponse>>> getAllOrders(
           ) {
        final var result = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

}
