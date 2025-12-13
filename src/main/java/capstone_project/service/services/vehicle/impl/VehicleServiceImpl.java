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
import capstone_project.dtos.response.vehicle.VehicleServiceRecordResponse;
import capstone_project.dtos.response.vehicle.VehicleResponse;
import capstone_project.dtos.response.vehicle.VehicleTypeResponse;
import capstone_project.dtos.response.vehicle.TopDriverResponse;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleServiceRecordEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleServiceRecordEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.repository.repositories.user.PenaltyHistoryRepository;
import capstone_project.repository.repositories.vehicle.VehicleAssignmentRepository;
import capstone_project.repository.repositories.issue.IssueRepository;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import capstone_project.service.mapper.vehicle.VehicleServiceRecordMapper;
import capstone_project.service.mapper.vehicle.VehicleMapper;
import capstone_project.service.mapper.vehicle.VehicleTypeMapper;
import capstone_project.service.mapper.user.PenaltyHistoryMapper;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import capstone_project.service.services.vehicle.VehicleServiceRecordService;
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
    private final VehicleServiceRecordEntityService vehicleServiceRecordEntityService;
    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final VehicleMapper vehicleMapper;
    private final VehicleAssignmentMapper vehicleAssignmentMapper;
    private final VehicleServiceRecordMapper vehicleServiceRecordMapper;
    private final VehicleTypeMapper vehicleTypeMapper;
    private final VehicleLocationService vehicleLocationService;
    private final PenaltyHistoryRepository penaltyHistoryRepository;
    private final PenaltyHistoryMapper penaltyHistoryMapper;
    private final VehicleAssignmentRepository vehicleAssignmentRepository;
    private final IssueRepository issueRepository;

    @Override
    public List<VehicleResponse> getAllVehicles() {
        
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

        // Get assignments
        List<VehicleAssignmentEntity> vha =
                vehicleAssignmentEntityService.findByVehicleEntityId(entity.getId());

        // map vehicle entity first
        VehicleGetDetailsResponse response = vehicleMapper.toVehicleDetailResponse(entity);

        // Set vehicle type response properly
        if (entity.getVehicleTypeEntity() != null) {
            VehicleTypeResponse vehicleTypeResponse = vehicleTypeMapper.toVehicleTypeResponse(entity.getVehicleTypeEntity());
            response.setVehicleTypeResponse(vehicleTypeResponse);
        }

        // map assignments and attach to response
        List<VehicleAssignmentResponse> assignmentResponses = vha.stream()
                .map(vehicleAssignmentMapper::toResponse)
                .toList();

        response.setVehicleAssignmentResponse(assignmentResponses);

        // Get service records
        List<VehicleServiceRecordEntity> vhm  =
                vehicleServiceRecordEntityService.findByVehicleEntityId(entity.getId());

        List<VehicleServiceRecordResponse> maintenanceResponses = vhm.stream()
                .map(vehicleServiceRecordMapper::toResponse)
                .toList();

        response.setVehicleMaintenanceResponse(maintenanceResponses);

        // Get top 3 drivers for this vehicle
        List<Object[]> topDriversData = vehicleAssignmentRepository.findTopDriversForVehicle(id);
        List<TopDriverResponse> topDrivers = topDriversData.stream()
                .limit(3)
                .map(row -> TopDriverResponse.builder()
                        .driverId(row[0] != null ? row[0].toString() : null)
                        .driverName(row[1] != null ? row[1].toString() : null)
                        .driverPhoneNumber(row[2] != null ? row[2].toString() : null)
                        .driverStatus(row[3] != null ? row[3].toString() : null)
                        .tripCount(row[4] != null ? ((Number) row[4]).longValue() : 0L)
                        .build())
                .toList();

        response.setTopDrivers(topDrivers);

        // Get penalties for this vehicle
        List<PenaltyHistoryEntity> penalties = penaltyHistoryRepository.findByVehicleId(id);
        List<PenaltyHistoryResponse> penaltyResponses = penalties.stream()
                .map(penalty -> {
                    PenaltyHistoryResponse penaltyResponse = penaltyHistoryMapper.toPenaltyHistoryResponse(penalty);
                    
                    // Get location from related Issue (issueCategory = PENALTY)
                    if (penalty.getVehicleAssignmentEntity() != null) {
                        UUID assignmentId = penalty.getVehicleAssignmentEntity().getId();
                        Optional<IssueEntity> penaltyIssue = issueRepository.findPenaltyIssueByVehicleAssignment(assignmentId);
                        
                        if (penaltyIssue.isPresent()) {
                            IssueEntity issue = penaltyIssue.get();
                            penaltyResponse.setViolationLatitude(issue.getLocationLatitude());
                            penaltyResponse.setViolationLongitude(issue.getLocationLongitude());
                        }
                    }
                    
                    return penaltyResponse;
                })
                .toList();

        response.setPenalties(penaltyResponses);

        return response;
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {

        VehicleEntity vehicleEntity = vehicleMapper.toVehicleEntity(request);
        VehicleEntity savedVehicle = vehicleEntityService.save(vehicleEntity);

        return vehicleMapper.toResponse(savedVehicle);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(UUID id, UpdateVehicleRequest request) {

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
            
        }
    }

    @Override
    @Transactional
    public boolean updateVehicleLocationWithRateLimit(UUID id, UpdateLocationRequest request, int minIntervalSeconds) {

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
            
        }

        return updated;
    }

    @Override
    @Transactional
    public int updateVehicleLocationsInBatch(BatchUpdateLocationRequest batchRequest) {

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

        List<VehicleEntity> createdVehicles = new ArrayList<>();
        List<VehicleResponse> vehicleResponses = new ArrayList<>();

        // Find the highest existing vehicle license plate number
        String prefix = "V";
        int startNumber = findHighestVehicleNumber() + 1;

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

            // Create the vehicle entity
            VehicleEntity vehicleEntity = VehicleEntity.builder()
                    .licensePlateNumber(licensePlate)
                    .model(model)
                    .manufacturer(manufacturer)
                    .year(year)
                    .status(VehicleStatusEnum.ACTIVE.name())
                    .vehicleTypeEntity(vehicleTypes.get(i % vehicleTypes.size()))
                    .createdAt(LocalDateTime.now())
                    .build();

            try {
                VehicleEntity savedVehicle = vehicleEntityService.save(vehicleEntity);
                createdVehicles.add(savedVehicle);
                
            } catch (Exception e) {
                log.error("Error creating vehicle with license plate {}: {}", licensePlate, e.getMessage());
            }
        }

        // Map all created vehicles to response objects
        for (VehicleEntity vehicle : createdVehicles) {
            vehicleResponses.add(vehicleMapper.toResponse(vehicle));
        }

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

        } catch (Exception e) {
            log.error("Error finding highest vehicle number, using default: {}", highestNumber, e);
        }

        return highestNumber;
    }
    
    @Override
    public long countVehiclesByStatus(String status) {
        return vehicleEntityService.countByStatus(status);
    }
    
    @Override
    public long countVehiclesByType(UUID vehicleTypeId) {
        return vehicleEntityService.countByVehicleTypeId(vehicleTypeId);
    }
}
