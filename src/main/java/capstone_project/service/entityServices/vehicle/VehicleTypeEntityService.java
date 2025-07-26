package capstone_project.service.entityServices.vehicle;

import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.service.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface VehicleTypeEntityService extends BaseEntityService<VehicleTypeEntity, UUID> {
    Optional<VehicleTypeEntity> findByVehicleTypeName(String vehicleTypeName);
}
