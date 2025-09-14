package capstone_project.service.services.order.order;

import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.dtos.request.order.CreateOrderDetailRequest;
import capstone_project.dtos.request.order.UpdateOrderDetailRequest;
import capstone_project.dtos.response.order.CreateOrderResponse;
import capstone_project.dtos.response.order.GetOrderDetailResponse;
import capstone_project.dtos.response.order.GetOrderDetailsResponseForList;

import java.util.List;
import java.util.UUID;

public interface OrderDetailService {
    GetOrderDetailResponse changeStatusOrderDetailExceptTroubles(UUID orderDetailId, OrderStatusEnum orderDetailStatus);

    GetOrderDetailResponse changeStatusOrderDetailForTroublesByDriver(UUID orderDetailId);

    CreateOrderResponse createOrderDetailByOrderId(UUID orderId, List<CreateOrderDetailRequest> createOrderDetailRequest);

    List<GetOrderDetailsResponseForList> getOrderDetailByOrderIdResponseList(UUID orderId);

    GetOrderDetailResponse getOrderDetailById(UUID orderDetailId);

    GetOrderDetailResponse updateOrderDetailBasicInPendingOrProcessing(UpdateOrderDetailRequest updateOrderDetailRequest);

    boolean changeStatusOrderDetailOnlyForAdmin(UUID orderId, UUID orderDetailId, OrderStatusEnum status);

    List<GetOrderDetailsResponseForList> updateVehicleAssigmentForEachOrderDetails(UUID orderId);

    List<GetOrderDetailsResponseForList> getAllOrderDetails();
}
