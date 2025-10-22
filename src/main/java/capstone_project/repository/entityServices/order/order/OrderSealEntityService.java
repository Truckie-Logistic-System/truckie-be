package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.OrderSealEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface OrderSealEntityService extends BaseEntityService<OrderSealEntity, UUID> {
    List<OrderSealEntity> saveAll(List<OrderSealEntity> orderSealEntities);

    List<OrderSealEntity> findBySealCode(String sealCode);

    OrderSealEntity findByVehicleAssignment(VehicleAssignmentEntity vehicleAssignment, String status);

    // New method to find all order seals for a vehicle assignment regardless of status
    List<OrderSealEntity> findAllByVehicleAssignment(VehicleAssignmentEntity vehicleAssignment);

    /**
     * Tìm tất cả OrderSeal theo VehicleAssignment và trạng thái
     * @param vehicleAssignment phương tiện vận chuyển
     * @param status trạng thái của seal cần tìm
     * @return danh sách các OrderSeal tương ứng
     */
    List<OrderSealEntity> findAllByVehicleAssignmentAndStatus(VehicleAssignmentEntity vehicleAssignment, String status);
}
