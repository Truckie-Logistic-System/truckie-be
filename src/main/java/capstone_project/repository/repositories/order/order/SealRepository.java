package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.SealEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;

public interface SealRepository extends BaseRepository<SealEntity> {
    List<SealEntity> findBySealCode(String sealCode);

    /**
     * Find the first order seal by vehicle assignment and status
     * Use findFirst to avoid "Query did not return a unique result" error when multiple seals exist
     * Orders by createdAt DESC to get the most recent seal
     */
    SealEntity findFirstByVehicleAssignmentAndStatusOrderByCreatedAtDesc(VehicleAssignmentEntity vehicleAssignment, String status);

    // New method to find all order seals for a vehicle assignment
    List<SealEntity> findByVehicleAssignment(VehicleAssignmentEntity vehicleAssignment);

    /**
     * Tìm tất cả Seal theo VehicleAssignment và trạng thái
     * @param vehicleAssignment phương tiện vận chuyển
     * @param status trạng thái của seal cần tìm
     * @return danh sách các Seal tương ứng
     */
    List<SealEntity> findAllByVehicleAssignmentAndStatus(VehicleAssignmentEntity vehicleAssignment, String status);
}
