package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface VehicleMaintenanceRepository extends BaseRepository<VehicleMaintenanceEntity> {
    // Additional methods specific to VehicleMaintenanceEntity can be defined here

    List<VehicleMaintenanceEntity> findByVehicleEntityId(UUID vehicleEntityId);
}
