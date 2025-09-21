package capstone_project.repository.entityServices.vehicle;

import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleEntityService extends BaseEntityService<VehicleEntity, UUID> {
    Optional<VehicleEntity> findByLicensePlateNumber(String licensePlateNumber);

    Optional<VehicleEntity> findByVehicleId(UUID vehicleId);

    Optional<VehicleEntity>  findVehicleDetailsById(UUID id);

    List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntity(VehicleTypeEntity vehicleTypeEntity);

    List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntityAndStatus(VehicleTypeEntity vehicleTypeEntity, String status);
}
