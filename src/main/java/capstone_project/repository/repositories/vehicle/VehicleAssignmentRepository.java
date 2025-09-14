package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleAssignmentRepository extends BaseRepository<VehicleAssignmentEntity> {
    List<VehicleAssignmentEntity> findByStatus(String status);

    List<VehicleAssignmentEntity> findByVehicleEntityId(UUID vehicleEntityId);

    List<VehicleAssignmentEntity> findByVehicleEntityId(String vehicleType);
}
