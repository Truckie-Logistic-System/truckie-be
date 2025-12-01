package capstone_project.controller.order;

import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.enums.UnitEnum;
import capstone_project.dtos.request.order.CreateOrderAndDetailRequest;
import capstone_project.dtos.request.order.UpdateOrderRequest;
import capstone_project.dtos.request.order.StaffCancelOrderRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.*;
import capstone_project.service.services.order.order.OrderDetailStatusService;
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
    private final OrderDetailStatusService orderDetailStatusService;

    @GetMapping("/get-my-orders")
    public ResponseEntity<ApiResponse<List<OrderForCustomerListResponse>>> getMyOrders() {
        final var result = orderService.getOrdersForCurrentCustomer();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

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
    public ResponseEntity<ApiResponse<List<GetOrderForGetAllResponse>>> getAllOrders(
           ) {
        final var result = orderService.getAllOrders();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

//    @GetMapping("/get-orders-for-cus-by-user-id/{userId}")
//    public ResponseEntity<ApiResponse<List<CreateOrderResponse>>> getAllOrdersForCusByUserId(@PathVariable UUID userId
//    ) {
//        final var result = orderService.getOrdersForCusByUserId(userId);
//        return ResponseEntity.ok(ApiResponse.ok(result));
//    }

    @GetMapping("/get-by-id/{orderId}")
    public ResponseEntity<ApiResponse<GetOrderResponse>> getOrderById(@PathVariable UUID orderId
    ) {
        final var result = orderService.getOrderById(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/list-unit")
    public ResponseEntity<ApiResponse<List<UnitEnum>>> getAllUnits() {
        final var result = orderService.responseListUnitEnum();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-order-for-customer-by-order-id/{orderId}")
    public ResponseEntity<ApiResponse<SimpleOrderForCustomerResponse>> getOrderForCustomerByOrderId(@PathVariable UUID orderId) {
        final var result = orderService.getSimplifiedOrderForCustomerByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-order-for-staff-by-order-id/{orderId}")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderForStaffResponse>> getOrderForStaffByOrderId(@PathVariable UUID orderId) {
        final var result = orderService.getOrderForStaffByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/sign-contract")
    public ResponseEntity<ApiResponse<Boolean>> signContractAndOrder(
            @RequestParam UUID contractId) {
        final var result = orderService.signContractAndOrder(contractId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-list-order-for-driver")
    public ResponseEntity<ApiResponse<List<GetOrderForDriverResponse>>> getOrderForDriverByDriveId() {
        final var result = orderService.getOrderForDriverByCurrentDrive();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-order-for-driver-by-order-id/{orderId}")
    public ResponseEntity<ApiResponse<OrderForDriverResponse>> getOrderForDriverByOrderId(@PathVariable UUID orderId) {
        final var result = orderService.getOrderForDriverByOrderId(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Update order status to ONGOING_DELIVERED when vehicle is near delivery point (within 3km)
     * Only valid when current status is ON_DELIVERED
     * 
     * @param orderId the order ID to update
     * @return updated order response
     */
    @PutMapping("/{orderId}/start-ongoing-delivery")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> startOngoingDelivery(@PathVariable UUID orderId) {
        final var result = orderService.updateToOngoingDelivered(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Update order status to DELIVERED when vehicle arrives at delivery point
     * Only valid when current status is ONGOING_DELIVERED
     * 
     * @param orderId the order ID to update
     * @return updated order response
     */
    @PutMapping("/{orderId}/arrive-at-delivery")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> arriveAtDelivery(@PathVariable UUID orderId) {
        final var result = orderService.updateToDelivered(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Complete the trip and update order status to SUCCESSFUL
     * Only valid when current status is DELIVERED
     * Driver confirms successful delivery completion
     * 
     * @param orderId the order ID to update
     * @return updated order response
     */
    @PutMapping("/{orderId}/complete-trip")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> completeTrip(@PathVariable UUID orderId) {
        final var result = orderService.updateToSuccessful(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
    
    /**
     * NEW ENDPOINT: Update OrderDetail status for a specific vehicle assignment
     * This is the primary endpoint for drivers to update delivery status
     * Supports multi-trip orders by updating only the OrderDetails for a specific trip
     * 
     * @param vehicleAssignmentId ID of the vehicle assignment (trip)
     * @param status New status to set for all OrderDetails in this trip
     * @return Success response
     */
    @PutMapping("/vehicle-assignment/{vehicleAssignmentId}/status")
    public ResponseEntity<ApiResponse<Void>> updateOrderDetailStatusByAssignment(
            @PathVariable UUID vehicleAssignmentId,
            @RequestParam OrderDetailStatusEnum status) {
        orderDetailStatusService.updateOrderDetailStatusByAssignment(vehicleAssignmentId, status);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
    
    /**
     * NEW ENDPOINT: Update OrderDetail status to ONGOING_DELIVERED when driver is near delivery point
     * Specific endpoint for the proximity-triggered status update (< 3km from delivery)
     * 
     * @param vehicleAssignmentId ID of the vehicle assignment (trip)
     * @return Success response
     */
    @PutMapping("/vehicle-assignment/{vehicleAssignmentId}/ongoing-delivery")
    public ResponseEntity<ApiResponse<Boolean>> updateToOngoingDeliveredByAssignment(
            @PathVariable UUID vehicleAssignmentId) {
        orderDetailStatusService.updateOrderDetailStatusByAssignment(
                vehicleAssignmentId,
                OrderDetailStatusEnum.ONGOING_DELIVERED
        );
        return ResponseEntity.ok(ApiResponse.ok(true));
    }

    /**
     * Cancel an order
     * Only allowed for orders with status PENDING, PROCESSING, or CONTRACT_DRAFT
     * 
     * @param orderId the order ID to cancel
     * @return Success response
     */
    @PutMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<Boolean>> cancelOrder(@PathVariable UUID orderId) {
        final var result = orderService.cancelOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Staff cancel an order with a specific reason
     * Only allowed for orders with status PROCESSING
     * Sends notification and email to customer
     * 
     * @param orderId the order ID to cancel
     * @param request the cancellation request with reason
     * @return Success response
     */
    @PutMapping("/{orderId}/staff-cancel")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Boolean>> staffCancelOrder(
            @PathVariable UUID orderId,
            @Valid @RequestBody StaffCancelOrderRequest request) {
        final var result = orderService.staffCancelOrder(orderId, request.getCancellationReason());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Get list of cancellation reasons for staff
     * 
     * @return List of cancellation reasons
     */
    @GetMapping("/cancellation-reasons/staff")
    @PreAuthorize("hasRole('STAFF') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<String>>> getStaffCancellationReasons() {
        final var result = orderService.getStaffCancellationReasons();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
