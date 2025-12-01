package capstone_project.service.services.order.order;

import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.enums.UnitEnum;
import capstone_project.dtos.request.order.CreateOrderDetailRequest;
import capstone_project.dtos.request.order.CreateOrderRequest;
import capstone_project.dtos.request.order.UpdateOrderRequest;
import capstone_project.dtos.response.order.*;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface OrderService {
    List<OrderForCustomerListResponse> getOrdersForCurrentCustomer();

    CreateOrderResponse createOrder(CreateOrderRequest orderRequest, List<CreateOrderDetailRequest> listCreateOrderDetailRequests);

    CreateOrderResponse changeAStatusOrder(UUID orderId, OrderStatusEnum status);

    CreateOrderResponse changeStatusOrderWithAllOrderDetail(UUID orderId, OrderStatusEnum status);

    boolean isValidTransition(OrderStatusEnum current, OrderStatusEnum next);

    List<CreateOrderResponse> getCreateOrderRequestsBySenderId(UUID senderId);

    List<CreateOrderResponse> getCreateOrderRequestsByDeliveryAddressId(UUID deliveryAddressId);

    boolean changeStatusOrderOnlyForAdmin(UUID orderId, OrderStatusEnum status);

    CreateOrderResponse updateOrderBasicInPendingOrProcessing(UpdateOrderRequest updateOrderRequest);

    List<OrderDetailEntity> batchCreateOrderDetails(
            List<CreateOrderDetailRequest> requests, OrderEntity savedOrder, LocalDateTime estimateStartTime);

    List<GetOrderForGetAllResponse> getAllOrders();

    List<CreateOrderResponse> getOrdersForCusByUserId(UUID userId);

    GetOrderResponse getOrderById(UUID orderId);

    List<UnitEnum> responseListUnitEnum();

    BigDecimal convertToTon(BigDecimal weightBaseUnit, String unit);

    GetOrderForCustomerResponse getOrderForCustomerByOrderId(UUID orderId);

    SimpleOrderForCustomerResponse getSimplifiedOrderForCustomerByOrderId(UUID orderId);

    OrderForStaffResponse getOrderForStaffByOrderId(UUID orderId);

    boolean signContractAndOrder(UUID contractId);

    boolean updateOrderStatus(UUID orderId, OrderStatusEnum newStatus);

    List<GetOrderForDriverResponse> getOrderForDriverByCurrentDrive();

    /**
     * Update order status to ONGOING_DELIVERED when vehicle is near delivery point
     * Validates that current status is ON_DELIVERED before updating
     * 
     * @param orderId the order ID to update
     * @return updated order response
     */
    CreateOrderResponse updateToOngoingDelivered(UUID orderId);

    /**
     * Update order status to DELIVERED when vehicle arrives at delivery point
     * Validates that current status is ONGOING_DELIVERED before updating
     * 
     * @param orderId the order ID to update
     * @return updated order response
     */
    CreateOrderResponse updateToDelivered(UUID orderId);

    /**
     * Update order status to SUCCESSFUL when driver confirms trip completion
     * Validates that current status is DELIVERED before updating
     * 
     * @param orderId the order ID to update
     * @return updated order response
     */
    CreateOrderResponse updateToSuccessful(UUID orderId);

    GetOrderByJpaResponse getSimplifiedOrderForCustomerV2ByOrderId(UUID orderId);

    OrderForDriverResponse getOrderForDriverByOrderId(UUID orderId);

    /**
     * Cancel an order
     * Only allowed for orders with status PENDING, PROCESSING, or CONTRACT_DRAFT
     * 
     * @param orderId the order ID to cancel
     * @return success status
     */
    boolean cancelOrder(UUID orderId);

    /**
     * Cancel an order by staff with a specific reason
     * Only allowed for orders with status PROCESSING
     * Sends notification and email to customer
     * 
     * @param orderId the order ID to cancel
     * @param cancellationReason the reason for cancellation
     * @return success status
     */
    boolean staffCancelOrder(UUID orderId, String cancellationReason);

    /**
     * Get list of cancellation reasons for staff
     * 
     * @return list of cancellation reasons
     */
    List<String> getStaffCancellationReasons();
}
