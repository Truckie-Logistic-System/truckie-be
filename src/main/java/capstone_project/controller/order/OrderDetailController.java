package capstone_project.controller.order;

import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.dtos.request.order.CreateOrderDetailRequest;
import capstone_project.dtos.request.order.UpdateOrderDetailRequest;
import capstone_project.dtos.request.vehicle.CreateAndAssignForDetailsRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.CreateOrderResponse;
import capstone_project.dtos.response.order.GetOrderDetailResponse;
import capstone_project.dtos.response.order.GetOrderDetailsResponseForList;
import capstone_project.service.services.order.order.OrderDetailService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${order-detail.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class OrderDetailController {
    private final OrderDetailService orderDetailService;


    // Change status OrderDetail (ngoại trừ Troubles)
    @PutMapping("/{orderDetailId}/status")
    public ResponseEntity<ApiResponse<GetOrderDetailResponse>> changeStatusOrderDetailExceptTroubles(
            @PathVariable UUID orderDetailId,
            @RequestParam OrderStatusEnum status) {
        final var result = orderDetailService.changeStatusOrderDetailExceptTroubles(orderDetailId, status);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Change status OrderDetail thành IN_TROUBLES (do Driver report)
    @PutMapping("/{orderDetailId}/status/troubles")
    public ResponseEntity<ApiResponse<GetOrderDetailResponse>> changeStatusOrderDetailForTroublesByDriver(
            @PathVariable UUID orderDetailId) {
        final var result = orderDetailService.changeStatusOrderDetailForTroublesByDriver(orderDetailId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Tạo orderDetail cho 1 orderId
    @PostMapping("/{orderId}")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrderDetailByOrderId(
            @PathVariable UUID orderId,
            @Valid @RequestBody List<CreateOrderDetailRequest> requests) {
        final var result = orderDetailService.createOrderDetailByOrderId(orderId, requests);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Lấy tất cả orderDetail theo orderId
    @GetMapping("/order/{orderId}")
    public ResponseEntity<ApiResponse<List<GetOrderDetailsResponseForList>>> getOrderDetailByOrderIdResponseList(
            @PathVariable UUID orderId) {
        final var result = orderDetailService.getOrderDetailByOrderIdResponseList(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Lấy orderDetail theo orderDetailId
    @GetMapping("/{orderDetailId}")
    public ResponseEntity<ApiResponse<GetOrderDetailResponse>> getOrderDetailByOrderDetailId(
            @PathVariable UUID orderDetailId) {
        final var result = orderDetailService.getOrderDetailById(orderDetailId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Update basic fields của OrderDetail
    @PutMapping
    public ResponseEntity<ApiResponse<GetOrderDetailResponse>> updateOrderDetailBasic(
            @Valid @RequestBody UpdateOrderDetailRequest request) {
        final var result = orderDetailService.updateOrderDetailBasicInPendingOrProcessing(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<GetOrderDetailsResponseForList>>> getAllOrderDetails(
    ) {
        final var result = orderDetailService.getAllOrderDetails();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("update-vehicle-assignment-for-details")
    public ResponseEntity<ApiResponse<List<GetOrderDetailsResponseForList>>> updateVehicleAssignmentForDetailsIfContractExisted(
            @Valid @RequestParam UUID orderId) {
        final var result = orderDetailService.updateVehicleAssigmentForEachOrderDetails(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping(value = "create-and-assign-assignment-for-details", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<List<GetOrderDetailsResponseForList>>> createAndAssignVehicleAssignmentForDetails(
            @Valid @RequestBody CreateAndAssignForDetailsRequest request) {
        final var result = orderDetailService.createAndAssignVehicleAssignmentForDetails(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
