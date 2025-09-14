package capstone_project.service.services.order.order;

import capstone_project.dtos.request.order.CreateOrderSizeRequest;
import capstone_project.dtos.request.order.UpdateOrderSizeRequest;
import capstone_project.dtos.response.order.GetOrderSizeResponse;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface OrderSizeService {

        GetOrderSizeResponse createOrderSize(CreateOrderSizeRequest request);

        GetOrderSizeResponse updateOrderSize(UpdateOrderSizeRequest request);

        boolean deleteOrderSize(UUID id);

        GetOrderSizeResponse getOrderSizeById(UUID id);

        List<GetOrderSizeResponse> getAllOrderSizes();

        boolean activeOrderSize(UUID id);


}
