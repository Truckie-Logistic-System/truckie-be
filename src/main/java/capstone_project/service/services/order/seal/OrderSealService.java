package capstone_project.service.services.order.seal;

import capstone_project.dtos.request.order.seal.OrderSealRequest;
import capstone_project.dtos.response.order.seal.GetOrderSealResponse;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;

import java.util.List;
import java.util.UUID;

public interface OrderSealService {
    /**
     * Xác nhận việc gắn seal đã được gán sẵn cho vehicle assignment
     * Driver sẽ cung cấp mã seal và ảnh chụp seal đã gắn
     */
    GetOrderSealResponse confirmSealAttachment(OrderSealRequest orderSealRequest);

    GetOrderSealResponse removeSealBySealId(UUID sealId);

    GetOrderSealResponse getAllBySealId(UUID sealId);

    GetOrderSealResponse getActiveOrderSealByVehicleAssignmentId(UUID vehicleAssignmentId);

    List<GetOrderSealResponse> getAllOrderSealsByVehicleAssignmentId(UUID vehicleAssignmentId);

    /**
     * Cập nhật trạng thái của seal thành USED khi đơn hàng hoàn thành
     * @param vehicleAssignment phương tiện vận chuyển đơn hàng
     * @return Số lượng seal được cập nhật
     */
    int updateOrderSealsToUsed(VehicleAssignmentEntity vehicleAssignment);
}
