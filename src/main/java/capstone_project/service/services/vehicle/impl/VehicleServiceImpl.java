package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.BatchUpdateLocationRequest;
import capstone_project.dtos.request.vehicle.UpdateLocationRequest;
import capstone_project.dtos.request.vehicle.UpdateVehicleRequest;
import capstone_project.dtos.request.vehicle.VehicleRequest;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleGetDetailsResponse;
import capstone_project.dtos.response.vehicle.VehicleMaintenanceResponse;
import capstone_project.dtos.response.vehicle.VehicleResponse;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleMaintenanceEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import capstone_project.service.mapper.vehicle.VehicleMaintenanceMapper;
import capstone_project.service.mapper.vehicle.VehicleMapper;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import capstone_project.service.services.vehicle.VehicleMaintenanceService;
import capstone_project.service.services.vehicle.VehicleService;
import capstone_project.service.services.vehicle.VehicleTypeService;
import capstone_project.service.websocket.VehicleLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleEntityService vehicleEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final VehicleMaintenanceEntityService vehicleMaintenanceEntityService;
    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final VehicleMapper vehicleMapper;
    private final VehicleAssignmentMapper vehicleAssignmentMapper;
    private final VehicleMaintenanceMapper vehicleMaintenanceMapper;
    private final VehicleLocationService vehicleLocationService;  // Thêm service cho broadcast vị trí

    @Override
    public List<VehicleResponse> getAllVehicles() {
        log.info("Fetching all vehicles");
        return Optional.of(vehicleEntityService.findAll())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new NotFoundException(
                        "There are no vehicles available.",
                        ErrorEnum.NOT_FOUND.getErrorCode()))
                .stream()
                .map(vehicleMapper::toResponse)
                .toList();
    }

    @Override
    public VehicleGetDetailsResponse getVehicleById(UUID id) {
        VehicleEntity entity = vehicleEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.warn("Vehicle with ID {} not found", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        List<VehicleAssignmentEntity> vha =
                vehicleAssignmentEntityService.findByVehicleEntityId(entity.getId());

        // map vehicle entity first
        VehicleGetDetailsResponse response = vehicleMapper.toVehicleDetailResponse(entity);

        // map assignments and attach to response
        List<VehicleAssignmentResponse> assignmentResponses = vha.stream()
                .map(vehicleAssignmentMapper::toResponse)
                .toList();

        response.setVehicleAssignmentResponse(assignmentResponses);

        List<VehicleMaintenanceEntity> vhm  =
                vehicleMaintenanceEntityService.findByVehicleEntityId(entity.getId());

        List<VehicleMaintenanceResponse> maintenanceResponses = vhm.stream()
                .map(vehicleMaintenanceMapper::toResponse)
                .toList();

        response.setVehicleMaintenanceResponse(maintenanceResponses);

        return response;
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        log.info("Creating new vehicle");

        VehicleEntity vehicleEntity = vehicleMapper.toVehicleEntity(request);
        VehicleEntity savedVehicle = vehicleEntityService.save(vehicleEntity);

        return vehicleMapper.toResponse(savedVehicle);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(UUID id, UpdateVehicleRequest request) {
        log.info("Updating vehicle with ID: {}", id);

        VehicleEntity existingVehicle = vehicleEntityService.findByVehicleId(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (request.licensePlateNumber() != null) {
            vehicleEntityService.findByLicensePlateNumber(request.licensePlateNumber())
                    .filter(v -> !v.getId().equals(id))
                    .ifPresent(v -> {
                        throw new BadRequestException(
                                "License plate number already exists",
                                ErrorEnum.ALREADY_EXISTED.getErrorCode()
                        );
                    });
        }

        vehicleMapper.toVehicleEntity(request, existingVehicle);

        if (request.currentLatitude() != null || request.currentLongitude() != null) {
            existingVehicle.setLastUpdated(LocalDateTime.now());
        }

        VehicleEntity updatedVehicle = vehicleEntityService.save(existingVehicle);

        return vehicleMapper.toResponse(updatedVehicle);
    }

    @Override
    @Transactional
    public void updateVehicleLocation(UUID id, UpdateLocationRequest request) {
        log.info("Updating vehicle location for vehicle ID: {}", id);

        // Basic validation
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BadRequestException(
                    "Latitude and longitude cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Use the direct update method instead of fetching entity first
        boolean updated = vehicleEntityService.updateLocationDirectly(
                id, request.getLatitude(), request.getLongitude());

        if (updated) {
            log.info("Successfully updated location for vehicle ID: {}", id);

            // Fetch vehicle details for broadcasting
            VehicleEntity vehicle = vehicleEntityService.findByVehicleId(id)
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle not found with ID: " + id,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            // Broadcast location update to all subscribers
            vehicleLocationService.broadcastVehicleLocation(vehicle);
        } else {
            // Check if vehicle exists at all - if not, throw not found
            if (!vehicleEntityService.findByVehicleId(id).isPresent()) {
                throw new NotFoundException(
                        "Vehicle not found with ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                );
            }
            log.debug("Location unchanged for vehicle ID: {}, skipping update", id);
        }
    }

    @Override
    @Transactional
    public boolean updateVehicleLocationWithRateLimit(UUID id, UpdateLocationRequest request, int minIntervalSeconds) {
        log.info("Updating vehicle location with rate limit for vehicle ID: {}", id);

        // Basic validation
        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new BadRequestException(
                    "Latitude and longitude cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        if (minIntervalSeconds <= 0) {
            minIntervalSeconds = 5; // Default to 5 seconds if invalid
        }

        boolean updated = vehicleEntityService.updateLocationWithRateLimit(
                id, request.getLatitude(), request.getLongitude(), minIntervalSeconds);

        if (updated) {
            log.info("Successfully updated rate-limited location for vehicle ID: {}", id);

            // Fetch vehicle details for broadcasting
            VehicleEntity vehicle = vehicleEntityService.findByVehicleId(id)
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle not found with ID: " + id,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            // Broadcast location update to all subscribers
            vehicleLocationService.broadcastVehicleLocation(vehicle);
        } else {
            // Check if vehicle exists
            if (!vehicleEntityService.findByVehicleId(id).isPresent()) {
                throw new NotFoundException(
                        "Vehicle not found with ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                );
            }
            log.debug("Location update for vehicle {} was skipped (rate limited or unchanged)", id);
        }

        return updated;
    }

    @Override
    @Transactional
    public int updateVehicleLocationsInBatch(BatchUpdateLocationRequest batchRequest) {
        log.info("Processing batch update for {} vehicles",
                batchRequest.getUpdates() != null ? batchRequest.getUpdates().size() : 0);

        if (batchRequest.getUpdates() == null || batchRequest.getUpdates().isEmpty()) {
            return 0;
        }

        // Basic validation
        for (BatchUpdateLocationRequest.VehicleLocationUpdate update : batchRequest.getUpdates()) {
            if (update.getVehicleId() == null || update.getLatitude() == null || update.getLongitude() == null) {
                throw new BadRequestException(
                        "Vehicle ID, latitude and longitude cannot be null in batch updates",
                        ErrorEnum.INVALID.getErrorCode()
                );
            }
        }

        int updatedCount = vehicleEntityService.updateLocationsInBatch(batchRequest);
        log.info("Batch update completed: {} of {} vehicles updated",
                updatedCount, batchRequest.getUpdates().size());

        // Broadcast updates for vehicles that were updated
        if (updatedCount > 0) {
            // Get list of updated vehicle IDs
            List<UUID> vehicleIds = batchRequest.getUpdates().stream()
                    .map(BatchUpdateLocationRequest.VehicleLocationUpdate::getVehicleId)
                    .toList();

            // Fetch vehicles and broadcast their updated locations
            vehicleEntityService.findAll().stream()
                    .filter(vehicle -> vehicleIds.contains(vehicle.getId()))
                    .forEach(vehicleLocationService::broadcastVehicleLocation);
        }

        return updatedCount;
    }

    @Override
    @Transactional
    public List<VehicleResponse> generateBulkVehicles(Integer count) {
        log.info("Generating {} bulk vehicles", count);

        List<VehicleEntity> createdVehicles = new ArrayList<>();
        List<VehicleResponse> vehicleResponses = new ArrayList<>();

        // Find the highest existing vehicle license plate number
        String prefix = "V";
        int startNumber = findHighestVehicleNumber() + 1;
        log.info("Starting vehicle generation from number: {}", startNumber);

        // Array of common truck manufacturers and models
        String[] manufacturers = {"Isuzu", "Hino", "Hyundai", "Thaco", "Kia", "Mitsubishi", "Ford"};
        String[] modelPrefixes = {"HD", "FT", "Mighty", "Frontier", "K", "Fuso", "Cargo"};

        // Get vehicle types from the database
        List<VehicleTypeEntity> vehicleTypes = vehicleTypeEntityService.findAll();
        if (vehicleTypes.isEmpty()) {
            throw new BadRequestException(
                    "No vehicle types found in the database",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        // Generate vehicles
        for (int i = 0; i < count; i++) {
            int currentNumber = startNumber + i;

            // Generate license plate with pattern: 29C-XXXXX (Hanoi truck format)
            String licensePlate = String.format("29C-%05d", currentNumber % 100000);

            // Rotate through manufacturers and models
            String manufacturer = manufacturers[i % manufacturers.length];
            String modelPrefix = modelPrefixes[i % modelPrefixes.length];
            String model = modelPrefix + "-" + (currentNumber % 1000);

            // Year between 2018 and current year (2025)
            int year = 2018 + (currentNumber % 8);

            // Capacity based on vehicle type
            VehicleTypeEntity vehicleType = vehicleTypes.get(i % vehicleTypes.size());

            // Create the vehicle entity
            VehicleEntity vehicleEntity = VehicleEntity.builder()
                    .licensePlateNumber(licensePlate)
                    .model(model)
                    .manufacturer(manufacturer)
                    .year(year)
                    .capacity(null)
                    .status(VehicleStatusEnum.ACTIVE.name())
                    .vehicleTypeEntity(vehicleType)
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                VehicleEntity savedVehicle = vehicleEntityService.save(vehicleEntity);
                createdVehicles.add(savedVehicle);
                log.info("Created vehicle with license plate: {}", licensePlate);
            } catch (Exception e) {
                log.error("Error creating vehicle with license plate {}: {}", licensePlate, e.getMessage());
            }
        }

        // Map all created vehicles to response objects
        for (VehicleEntity vehicle : createdVehicles) {
            vehicleResponses.add(vehicleMapper.toResponse(vehicle));
        }

        log.info("Successfully generated {} vehicles", vehicleResponses.size());
        return vehicleResponses;
    }

    /**
     * Find the highest vehicle number based on license plates
     * Looks for license plates with pattern 29C-XXXXX
     * @return highest number found, or 10000 as default
     */
    private int findHighestVehicleNumber() {
        int highestNumber = 10000; // Default starting number

        try {
            List<VehicleEntity> vehicles = vehicleEntityService.findAll();

            for (VehicleEntity vehicle : vehicles) {
                String licensePlate = vehicle.getLicensePlateNumber();
                if (licensePlate != null && licensePlate.startsWith("29C-")) {
                    try {
                        // Extract the number part from license plate (29C-12345 -> 12345)
                        int vehicleNumber = Integer.parseInt(licensePlate.substring(4));
                        if (vehicleNumber > highestNumber) {
                            highestNumber = vehicleNumber;
                        }
                    } catch (NumberFormatException e) {
                        log.warn("Skipping non-standard license plate format: {}", licensePlate);
                    }
                }
            }

            log.info("Found highest vehicle number: {}", highestNumber);
        } catch (Exception e) {
            log.error("Error finding highest vehicle number, using default: {}", highestNumber, e);
        }

        return highestNumber;
    }
}
