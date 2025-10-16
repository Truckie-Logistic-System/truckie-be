package capstone_project.repository.entityServices.vehicle.impl;

import capstone_project.dtos.request.vehicle.BatchUpdateLocationRequest;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.repositories.vehicle.VehicleRepository;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleEntityServiceImpl implements VehicleEntityService {

    private final VehicleRepository vehicleRepository;

    @Override
    public VehicleEntity save(VehicleEntity entity) {
        return vehicleRepository.save(entity);
    }

    @Override
    @EntityGraph(attributePaths = {"vehicleType", "vehicleAssignment", "vehicleMaintenance"})
    public Optional<VehicleEntity> findEntityById(UUID uuid) {
        return vehicleRepository.findById(uuid);
    }

    @Override
    public List<VehicleEntity> findAll() {
        return vehicleRepository.findAll();
    }

    @Override
    public Optional<VehicleEntity> findByLicensePlateNumber(String licensePlateNumber) {
        return vehicleRepository.findByLicensePlateNumber(licensePlateNumber);
    }

    @Override
    public Optional<VehicleEntity> findByVehicleId(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId);
    }

    @Override
    public Optional<VehicleEntity> findByVehicleIdWithVehicleType(UUID vehicleId) {
        return vehicleRepository.findByIdWithVehicleType(vehicleId);
    }

    @Override
    public Optional<VehicleEntity>  findVehicleDetailsById(UUID id) {
        return vehicleRepository.findVehicleWithJoinsById(id);
    }

    @Override
    public List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntity(VehicleTypeEntity vehicleTypeEntity) {
        return vehicleRepository.getVehicleEntitiesByVehicleTypeEntity(vehicleTypeEntity);
    }

    @Override
    public List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntityAndStatus(VehicleTypeEntity vehicleTypeEntity, String status) {
        return vehicleRepository.getVehicleEntitiesByVehicleTypeEntityAndStatus(vehicleTypeEntity,status);
    }

    @Override
    public List<VehicleEntity> findByVehicleTypeAndStatus(UUID vehicleTypeId, String status) {
        return vehicleRepository.findVehicleEntitiesByVehicleTypeEntityIdAndStatus(vehicleTypeId,status);
    }

    @Override
    public boolean updateLocationDirectly(UUID id, BigDecimal latitude, BigDecimal longitude) {
        LocalDateTime now = LocalDateTime.now();
        int updatedRows = vehicleRepository.updateLocationDirectly(id, latitude, longitude, now);
        log.debug("Direct location update for vehicle {}: affected {} rows", id, updatedRows);
        return updatedRows > 0;
    }

    @Override
    public boolean updateLocationWithRateLimit(UUID id, BigDecimal latitude, BigDecimal longitude, int minIntervalSeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime minLastUpdated = now.minusSeconds(minIntervalSeconds);

        int updatedRows = vehicleRepository.updateLocationWithRateLimit(
                id, latitude, longitude, now, minLastUpdated);

        if (updatedRows == 0) {
            log.debug("Location update for vehicle {} was rate limited or unchanged", id);
        } else {
            log.debug("Rate-limited location update for vehicle {} succeeded", id);
        }

        return updatedRows > 0;
    }

    @Override
    public int updateLocationsInBatch(BatchUpdateLocationRequest batchRequest) {
        if (batchRequest.getUpdates().isEmpty()) {
            return 0;
        }

        // Current implementation supports up to 5 vehicles in a batch
        // We'll process them in chunks of 5
        int totalUpdated = 0;
        List<BatchUpdateLocationRequest.VehicleLocationUpdate> updates = batchRequest.getUpdates();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 0; i < updates.size(); i += 5) {
            int chunkSize = Math.min(5, updates.size() - i);
            BatchUpdateLocationRequest.VehicleLocationUpdate[] chunk = new BatchUpdateLocationRequest.VehicleLocationUpdate[5];

            // Fill the chunk with actual updates
            for (int j = 0; j < chunkSize; j++) {
                chunk[j] = updates.get(i + j);
            }

            // Fill remaining slots with placeholder values to avoid null parameters
            for (int j = chunkSize; j < 5; j++) {
                // Using the first item's values for unused slots (they won't be updated due to WHERE clause)
                chunk[j] = updates.get(0);
            }

            int updated = vehicleRepository.updateBatchLocations(
                    chunk[0].getVehicleId(), chunk[0].getLatitude(), chunk[0].getLongitude(),
                    chunk[1].getVehicleId(), chunk[1].getLatitude(), chunk[1].getLongitude(),
                    chunk[2].getVehicleId(), chunk[2].getLatitude(), chunk[2].getLongitude(),
                    chunk[3].getVehicleId(), chunk[3].getLatitude(), chunk[3].getLongitude(),
                    chunk[4].getVehicleId(), chunk[4].getLatitude(), chunk[4].getLongitude(),
                    now
            );

            totalUpdated += updated;
        }

        log.debug("Batch update processed {} vehicles, actually updated {}",
                updates.size(), totalUpdated);
        return totalUpdated;
    }
}
