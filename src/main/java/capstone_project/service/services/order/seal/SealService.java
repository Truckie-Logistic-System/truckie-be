package capstone_project.service.services.order.seal;

import capstone_project.dtos.request.order.seal.SealRequest;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;

import java.util.List;
import java.util.UUID;

public interface SealService {
    /**
     * Xác nhận việc gắn seal đã được gán sẵn cho vehicle assignment
     * Driver sẽ cung cấp mã seal và ảnh chụp seal đã gắn
     */
    GetSealResponse confirmSealAttachment(SealRequest sealRequest);

    GetSealResponse removeSealBySealId(UUID sealId);

    GetSealResponse getAllBySealId(UUID sealId);

    GetSealResponse getSealByVehicleAssignmentId(UUID vehicleAssignmentId);

    List<GetSealResponse> getAllSealsByVehicleAssignmentId(UUID vehicleAssignmentId);

    /**
     * Cập nhật trạng thái của seal thành USED khi đơn hàng hoàn thành
     * @param vehicleAssignment phương tiện vận chuyển đơn hàng
     * @return Số lượng seal được cập nhật
     */
    int updateSealsToUsed(VehicleAssignmentEntity vehicleAssignment);
}
