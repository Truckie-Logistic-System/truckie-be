package capstone_project.repository.entityServices.vehicle;

import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface VehicleEntityService extends BaseEntityService<VehicleEntity, UUID> {
    Optional<VehicleEntity> findByLicensePlateNumber(String licensePlateNumber);

    Optional<VehicleEntity> findByVehicleId(UUID vehicleId);
}
