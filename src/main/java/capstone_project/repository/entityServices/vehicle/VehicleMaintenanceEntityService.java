package capstone_project.repository.entityServices.vehicle;

import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface VehicleMaintenanceEntityService extends BaseEntityService<VehicleMaintenanceEntity, UUID> {

    List<VehicleMaintenanceEntity> findByVehicleEntityId(UUID vehicleId);
}
