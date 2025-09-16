package capstone_project.service.services.order.seal;

import capstone_project.dtos.request.order.seal.OrderDetailSealRequest;
import capstone_project.dtos.response.order.seal.GetOrderDetailSealResponse;
import capstone_project.dtos.response.order.seal.GetSealFullResponse;

import java.util.UUID;

public interface OrderDetailSealService {
    GetSealFullResponse assignAFirstSealForOrderDetail(OrderDetailSealRequest orderDetailSealRequest);

    GetSealFullResponse removeSealForDetailsBySealId(UUID sealId);

    GetSealFullResponse getAllBySealId(UUID sealId);

    GetOrderDetailSealResponse getActiveOrderSealByOrderDetailId(UUID orderDetailId);



}
