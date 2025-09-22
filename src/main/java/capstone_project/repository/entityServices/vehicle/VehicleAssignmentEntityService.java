package capstone_project.repository.entityServices.vehicle;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleAssignmentEntityService extends BaseEntityService<VehicleAssignmentEntity, UUID> {
    List<VehicleAssignmentEntity> findByStatus(String status);

    List<VehicleAssignmentEntity> findByVehicleEntityId(UUID vehicleID);

    List<VehicleAssignmentEntity> findVehicleWithOrder(UUID type);

    List<VehicleAssignmentEntity> findVehicleAssignmentsWithOrderID(UUID orderID);

    Optional<VehicleAssignmentEntity> findVehicleAssignmentByVehicleEntityAndStatus(VehicleEntity vehicle, String status);

    List<VehicleAssignmentEntity> findAssignmentsByVehicleOrderByCreatedAtDesc(VehicleEntity vehicle);

    List<Object[]> countAssignmentsThisMonthForVehicles(List<UUID> vehicleIds,
                                                        LocalDateTime startOfMonth,
                                                        LocalDateTime endOfMonth);
    boolean existsActiveAssignmentForDriver(UUID driverId);
}
