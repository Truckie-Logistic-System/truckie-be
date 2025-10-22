package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.OrderSealEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;

public interface OrderSealRepository extends BaseRepository<OrderSealEntity> {
    List<OrderSealEntity> findBySealCode(String sealCode);

    /**
     * Find the first order seal by vehicle assignment and status
     * Use findFirst to avoid "Query did not return a unique result" error when multiple seals exist
     */
    OrderSealEntity findFirstByVehicleAssignmentAndStatus(VehicleAssignmentEntity vehicleAssignment, String status);

    // New method to find all order seals for a vehicle assignment
    List<OrderSealEntity> findByVehicleAssignment(VehicleAssignmentEntity vehicleAssignment);

    /**
     * Tìm tất cả OrderSeal theo VehicleAssignment và trạng thái
     * @param vehicleAssignment phương tiện vận chuyển
     * @param status trạng thái của seal cần tìm
     * @return danh sách các OrderSeal tương ứng
     */
    List<OrderSealEntity> findAllByVehicleAssignmentAndStatus(VehicleAssignmentEntity vehicleAssignment, String status);
}
