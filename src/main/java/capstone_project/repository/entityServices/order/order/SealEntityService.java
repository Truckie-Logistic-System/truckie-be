package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.SealEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface SealEntityService extends BaseEntityService<SealEntity, UUID> {
    List<SealEntity> saveAll(List<SealEntity> sealEntities);

    List<SealEntity> findBySealCode(String sealCode);

    SealEntity findByVehicleAssignment(VehicleAssignmentEntity vehicleAssignment, String status);

    // New method to find all order seals for a vehicle assignment regardless of status
    List<SealEntity> findAllByVehicleAssignment(VehicleAssignmentEntity vehicleAssignment);

    /**
     * Tìm tất cả Seal theo VehicleAssignment và trạng thái
     * @param vehicleAssignment phương tiện vận chuyển
     * @param status trạng thái của seal cần tìm
     * @return danh sách các Seal tương ứng
     */
    List<SealEntity> findAllByVehicleAssignmentAndStatus(VehicleAssignmentEntity vehicleAssignment, String status);
}
