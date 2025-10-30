package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.SealEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionCreateRequest;
import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionEndReadingRequest;
import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionInvoiceRequest;
import capstone_project.dtos.response.vehicle.VehicleFuelConsumptionResponse;
import capstone_project.entity.order.order.VehicleFuelConsumptionEntity;
import capstone_project.repository.entityServices.order.VehicleFuelConsumptionEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.order.SealEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.order.OrderService;
import capstone_project.service.services.order.order.OrderDetailStatusService;
import capstone_project.service.services.vehicle.VehicleFuelConsumptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleFuelConsumptionServiceImpl implements VehicleFuelConsumptionService {

    private final VehicleFuelConsumptionEntityService vehicleFuelConsumptionEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final CloudinaryService cloudinaryService;
    private final OrderEntityService orderEntityService;
    private final OrderService orderService;
    private final SealEntityService sealEntityService;
    private final OrderDetailStatusService orderDetailStatusService;

    @Override
    @Transactional
    public VehicleFuelConsumptionResponse createVehicleFuelConsumption(VehicleFuelConsumptionCreateRequest request) {
        log.info("Creating vehicle fuel consumption with vehicle assignment ID: {}", request.vehicleAssignmentId());

        final var vehicleAssignmentEntity = vehicleAssignmentEntityService.findById(request.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        "Vehicle assignment not found with ID: " + request.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        final var existingEntity = vehicleFuelConsumptionEntityService.findByVehicleAssignmentId(request.vehicleAssignmentId());

        if (existingEntity.isPresent()) {
            throw new IllegalStateException("Fuel consumption record already exists for vehicle assignment ID: " + request.vehicleAssignmentId());
        }

        final var odometerAtStartUrl = uploadImage(request.odometerAtStartImage(), "vehicle-fuel/odometer-start");

        final var entity = VehicleFuelConsumptionEntity.builder()
                .vehicleAssignmentEntity(vehicleAssignmentEntity)
                .odometerReadingAtStart(request.odometerReadingAtStart())
                .odometerAtStartUrl(odometerAtStartUrl)
                .dateRecorded(LocalDateTime.now())
                .build();

        final var savedEntity = vehicleFuelConsumptionEntityService.save(entity);

        // ✅ NEW: Auto-update OrderDetail status to PICKING_UP when driver starts trip
        try {
            orderDetailStatusService.updateOrderDetailStatusByAssignment(
                    request.vehicleAssignmentId(),
                    OrderDetailStatusEnum.PICKING_UP
            );
            log.info("✅ Auto-updated OrderDetail status: ASSIGNED_TO_DRIVER → PICKING_UP for assignment: {}", 
                    request.vehicleAssignmentId());
        } catch (Exception e) {
            log.warn("⚠️ Failed to auto-update OrderDetail status: {}", e.getMessage());
            // Don't fail the main operation - fuel consumption was created successfully
        }

        return mapToResponse(savedEntity);
    }

    @Override
    @Transactional
    public VehicleFuelConsumptionResponse updateInvoiceImage(VehicleFuelConsumptionInvoiceRequest request) {
        log.info("Updating invoice image for vehicle fuel consumption ID: {}", request.id());

        final var entity = vehicleFuelConsumptionEntityService.findEntityById(request.id())
                .orElseThrow(() -> new NotFoundException(
                        "Vehicle fuel consumption not found with ID: " + request.id(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        final var companyInvoiceImageUrl = uploadImage(request.companyInvoiceImage(), "vehicle-fuel/invoices");

        entity.setCompanyInvoiceImageUrl(companyInvoiceImageUrl);
        final var updatedEntity = vehicleFuelConsumptionEntityService.save(entity);

        return mapToResponse(updatedEntity);
    }

    @Override
    @Transactional
    public VehicleFuelConsumptionResponse updateFinalReading(VehicleFuelConsumptionEndReadingRequest request) {
        log.info("Updating final odometer reading for vehicle fuel consumption ID: {}", request.id());
        log.info("Request odometerReadingAtEnd: {}", request.odometerReadingAtEnd());
        log.info("Request odometerReadingAtEnd type: {}", request.odometerReadingAtEnd() != null ? request.odometerReadingAtEnd().getClass() : "null");

        final var entity = vehicleFuelConsumptionEntityService.findEntityById(request.id())
                .orElseThrow(() -> new NotFoundException(
                        "Vehicle fuel consumption not found with ID: " + request.id(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        final var odometerAtEndUrl = uploadImage(request.odometerAtEndImage(), "vehicle-fuel/odometer-end");

        log.info("Entity odometerReadingAtStart: {}", entity.getOdometerReadingAtStart());

        // Ensure both odometer readings are not null
        if (request.odometerReadingAtEnd() == null) {
            throw new IllegalArgumentException("Odometer reading at end cannot be null");
        }
        if (entity.getOdometerReadingAtStart() == null) {
            throw new IllegalArgumentException("Odometer reading at start is null in the database");
        }

        final var distanceTraveled = request.odometerReadingAtEnd().subtract(entity.getOdometerReadingAtStart());

        final var vehicleType = entity.getVehicleAssignmentEntity().getVehicleEntity().getVehicleTypeEntity();
        final var averageFuelConsumption = vehicleType.getAverageFuelConsumptionLPer100km();

        // Check if average fuel consumption is null
        if (averageFuelConsumption == null) {
            log.warn("Average fuel consumption is null for vehicle type: {}, using default value",
                    vehicleType != null ? vehicleType.getId() : "unknown");
            // Use default fuel consumption value of 10L/100km if null
            final var fuelVolume = distanceTraveled.multiply(new BigDecimal("10"))
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            entity.setFuelVolume(fuelVolume);
        } else {
            final var fuelVolume = distanceTraveled.multiply(averageFuelConsumption)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            entity.setFuelVolume(fuelVolume);
        }

        entity.setOdometerAtEndUrl(odometerAtEndUrl);
        entity.setOdometerReadingAtEnd(request.odometerReadingAtEnd());
        entity.setDistanceTraveled(distanceTraveled);

        final var updatedEntity = vehicleFuelConsumptionEntityService.save(entity);

        // ✅ NEW: Auto-update OrderDetail status to SUCCESSFUL after odometer end upload
        try {
            UUID vehicleAssignmentId = entity.getVehicleAssignmentEntity().getId();
            orderDetailStatusService.updateOrderDetailStatusByAssignment(
                    vehicleAssignmentId,
                    OrderDetailStatusEnum.SUCCESSFUL
            );
            log.info("✅ Auto-updated OrderDetail status: DELIVERED → SUCCESSFUL for assignment: {}", 
                    vehicleAssignmentId);
            
            // Update the IN_USE seal to USED (there should be only one)
            final var vehicleAssignment = entity.getVehicleAssignmentEntity();
            final var inUseSeal = sealEntityService.findByVehicleAssignment(
                    vehicleAssignment,
                    SealEnum.IN_USE.name());

            if (inUseSeal != null) {
                inUseSeal.setStatus(SealEnum.USED.name());
                sealEntityService.save(inUseSeal);
                log.info("Updated seal {} status from IN_USE to USED", inUseSeal.getSealCode());
            }
        } catch (Exception e) {
            log.warn("⚠️ Failed to auto-update OrderDetail status or seal: {}", e.getMessage());
            // Don't fail the main operation - odometer was updated successfully
        }

        return mapToResponse(updatedEntity);
    }

    @Override
    public VehicleFuelConsumptionResponse getVehicleFuelConsumptionById(UUID id) {
        log.info("Getting vehicle fuel consumption by ID: {}", id);

        final var entity = vehicleFuelConsumptionEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Vehicle fuel consumption not found with ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        return mapToResponse(entity);
    }

    @Override
    public VehicleFuelConsumptionResponse getVehicleFuelConsumptionByVehicleAssignmentId(UUID vehicleAssignmentId) {
        log.info("Getting vehicle fuel consumption by vehicle assignment ID: {}", vehicleAssignmentId);

        final var entity = vehicleFuelConsumptionEntityService.findByVehicleAssignmentId(vehicleAssignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Vehicle fuel consumption not found for vehicle assignment ID: " + vehicleAssignmentId,
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        return mapToResponse(entity);
    }

    private String uploadImage(MultipartFile file, String folder) {
        try {
            final var uploadResult = cloudinaryService.uploadFile(file.getBytes(), file.getOriginalFilename(), folder);
            return (String) uploadResult.get("secure_url");
        } catch (IOException e) {
            log.error("Error uploading image to Cloudinary: {}", e.getMessage());
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    private VehicleFuelConsumptionResponse mapToResponse(VehicleFuelConsumptionEntity entity) {
        return new VehicleFuelConsumptionResponse(
                entity.getId(),
                entity.getFuelVolume(),
                entity.getCompanyInvoiceImageUrl(),
                entity.getOdometerAtStartUrl(),
                entity.getOdometerReadingAtStart(),
                entity.getOdometerAtEndUrl(),
                entity.getOdometerReadingAtEnd(),
                entity.getDistanceTraveled(),
                entity.getDateRecorded(),
                entity.getNotes(),
                entity.getVehicleAssignmentEntity() != null ? entity.getVehicleAssignmentEntity().getId() : null
        );
    }
}
