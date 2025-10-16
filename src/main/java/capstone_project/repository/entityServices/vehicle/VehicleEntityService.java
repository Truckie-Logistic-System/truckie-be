package capstone_project.repository.entityServices.vehicle;

import capstone_project.dtos.request.vehicle.BatchUpdateLocationRequest;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleEntityService extends BaseEntityService<VehicleEntity, UUID> {
    Optional<VehicleEntity> findByLicensePlateNumber(String licensePlateNumber);

    Optional<VehicleEntity> findByVehicleId(UUID vehicleId);

    /**
     * Find vehicle by ID with eagerly fetched vehicleType relationship
     */
    Optional<VehicleEntity> findByVehicleIdWithVehicleType(UUID vehicleId);

    Optional<VehicleEntity>  findVehicleDetailsById(UUID id);

    List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntity(VehicleTypeEntity vehicleTypeEntity);

    List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntityAndStatus(VehicleTypeEntity vehicleTypeEntity, String status);

    List<VehicleEntity> findByVehicleTypeAndStatus(UUID vehicleTypeId, String status);

    /**
     * Update vehicle location directly with SQL without fetching the entity first
     * @return true if location was updated, false if unchanged or not found
     */
    boolean updateLocationDirectly(UUID id, BigDecimal latitude, BigDecimal longitude);

    /**
     * Update vehicle location with rate limiting - will only update if the minimum time
     * threshold has passed since last update
     * @param minIntervalSeconds minimum seconds that must have passed since last update
     * @return true if location was updated, false if skipped due to rate limit or unchanged
     */
    boolean updateLocationWithRateLimit(UUID id, BigDecimal latitude, BigDecimal longitude, int minIntervalSeconds);

    /**
     * Update locations of multiple vehicles in a single database operation
     * @return number of vehicles that were actually updated
     */
    int updateLocationsInBatch(BatchUpdateLocationRequest batchRequest);
}
