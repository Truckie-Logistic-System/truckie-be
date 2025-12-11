package capstone_project.repository.entityServices.vehicle;

import capstone_project.entity.vehicle.VehicleServiceRecordEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleServiceRecordEntityService extends BaseEntityService<VehicleServiceRecordEntity, UUID> {

    List<VehicleServiceRecordEntity> findByVehicleEntityId(UUID vehicleId);
    
    /**
     * Find a vehicle service record by ID with eagerly fetched vehicle and vehicle type
     */
    Optional<VehicleServiceRecordEntity> findByIdWithVehicleAndVehicleType(UUID recordId);
}
