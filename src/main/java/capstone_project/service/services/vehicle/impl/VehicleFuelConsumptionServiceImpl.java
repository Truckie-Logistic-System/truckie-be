package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.SealEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.enums.VehicleAssignmentStatusEnum;
import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionCreateRequest;
import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionEndReadingRequest;
import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionInvoiceRequest;
import capstone_project.dtos.response.vehicle.VehicleFuelConsumptionListResponse;
import capstone_project.dtos.response.vehicle.VehicleFuelConsumptionResponse;
import capstone_project.entity.order.order.VehicleFuelConsumptionEntity;
import capstone_project.repository.entityServices.order.VehicleFuelConsumptionEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.order.SealEntityService;
import capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.entity.order.order.JourneySegmentEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.order.OrderService;
import capstone_project.service.services.order.order.OrderDetailStatusService;
import capstone_project.service.services.order.order.OrderStatusWebSocketService;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.order.FuelTypeEntity;

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
    private final capstone_project.repository.entityServices.order.order.OrderDetailEntityService orderDetailEntityService;
    private final capstone_project.service.services.order.order.OrderDetailStatusWebSocketService orderDetailStatusWebSocketService;
    private final OrderStatusWebSocketService orderStatusWebSocketService;
    private final JourneyHistoryEntityService journeyHistoryEntityService;
    private final capstone_project.repository.entityServices.vehicle.VehicleEntityService vehicleEntityService;

    @Override
    public List<VehicleFuelConsumptionListResponse> getAllVehicleFuelConsumptions() {
        List<VehicleFuelConsumptionEntity> entities = vehicleFuelConsumptionEntityService.findAll();
        
        // Sort by createdAt DESC (newest first)
        entities.sort(Comparator.comparing(
                VehicleFuelConsumptionEntity::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        
        return entities.stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }
    
    private VehicleFuelConsumptionListResponse mapToListResponse(VehicleFuelConsumptionEntity entity) {
        VehicleAssignmentEntity va = entity.getVehicleAssignmentEntity();
        
        // Get vehicle info safely
        VehicleEntity vehicle = va != null ? va.getVehicleEntity() : null;
        
        // Get driver info safely
        String driverName = null;
        String driverPhone = null;
        UUID driverId = null;
        if (va != null && va.getDriver1() != null) {
            DriverEntity driver = va.getDriver1();
            driverId = driver.getId();
            UserEntity driverUser = driver.getUser();
            if (driverUser != null) {
                driverName = driverUser.getFullName();
                driverPhone = driverUser.getPhoneNumber();
            }
        }
        
        return VehicleFuelConsumptionListResponse.builder()
                .id(entity.getId())
                .fuelVolumeLiters(entity.getFuelVolume())
                .odometerAtStartUrl(entity.getOdometerAtStartUrl())
                .odometerAtEndUrl(entity.getOdometerAtEndUrl())
                .companyInvoiceImageUrl(entity.getCompanyInvoiceImageUrl())
                .odometerStartKm(entity.getOdometerReadingAtStart())
                .odometerEndKm(entity.getOdometerReadingAtEnd())
                .distanceTraveledKm(entity.getDistanceTraveled())
                .dateRecorded(entity.getDateRecorded())
                .notes(entity.getNotes())
                .createdAt(entity.getCreatedAt())
                .vehicleAssignment(va != null ? VehicleFuelConsumptionListResponse.VehicleAssignmentInfo.builder()
                        .id(va.getId())
                        .trackingCode(va.getTrackingCode())
                        .status(va.getStatus())
                        .build() : null)
                .vehicle(vehicle != null ? VehicleFuelConsumptionListResponse.VehicleInfo.builder()
                        .id(vehicle.getId())
                        .licensePlateNumber(vehicle.getLicensePlateNumber())
                        .vehicleType(vehicle.getVehicleTypeEntity() != null ? vehicle.getVehicleTypeEntity().getVehicleTypeName() : null)
                        .brand(vehicle.getManufacturer())
                        .model(vehicle.getModel())
                        .build() : null)
                .driver(driverId != null ? VehicleFuelConsumptionListResponse.DriverInfo.builder()
                        .id(driverId)
                        .fullName(driverName)
                        .phoneNumber(driverPhone)
                        .build() : null)
                .fuelType(null) // FuelType relationship not in entity, can be added later
                .build();
    }

    @Override
    @Transactional
    public VehicleFuelConsumptionResponse createVehicleFuelConsumption(VehicleFuelConsumptionCreateRequest request) {

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
            
            // ✅ NEW: Update vehicle status to IN_TRANSIT when start odometer is uploaded
            var vehicleEntity = vehicleAssignmentEntity.getVehicleEntity();
            if (vehicleEntity != null) {
                String oldStatus = vehicleEntity.getStatus();
                vehicleEntity.setStatus(VehicleStatusEnum.IN_TRANSIT.name());
                vehicleEntityService.save(vehicleEntity);
                
                log.info("✅ Vehicle {} status updated from {} to IN_TRANSIT after start odometer upload", 
                        vehicleEntity.getId(), oldStatus);
            }
            
        } catch (Exception e) {
            log.warn("⚠️ Failed to auto-update OrderDetail status or vehicle status: {}", e.getMessage());
            // Don't fail the main operation - fuel consumption was created successfully
        }

        return mapToResponse(savedEntity);
    }

    @Override
    @Transactional
    public VehicleFuelConsumptionResponse updateInvoiceImage(VehicleFuelConsumptionInvoiceRequest request) {

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

        final var entity = vehicleFuelConsumptionEntityService.findEntityById(request.id())
                .orElseThrow(() -> new NotFoundException(
                        "Vehicle fuel consumption not found with ID: " + request.id(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        final var odometerAtEndUrl = uploadImage(request.odometerAtEndImage(), "vehicle-fuel/odometer-end");

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
        final var vehicleWeightLimit = vehicleType.getWeightLimitTon();

        log.info("\uD83D\uDD0E Calculating fuel consumption for VehicleAssignment={}, vehicleType={}, odoStart={}, odoEnd={}, distanceKm={}",
                entity.getVehicleAssignmentEntity().getId(),
                vehicleType != null ? vehicleType.getId() : "unknown",
                entity.getOdometerReadingAtStart(),
                request.odometerReadingAtEnd(),
                distanceTraveled);

        // ✅ Use journey-based calculation with load factors + odo distance validation
        BigDecimal fuelVolume;

        BigDecimal effectiveBaseConsumption;
        if (averageFuelConsumption == null) {
            log.warn("Average fuel consumption is null for vehicle type: {}, using default value 10L/100km",
                    vehicleType != null ? vehicleType.getId() : "unknown");
            effectiveBaseConsumption = new BigDecimal("10");
        } else {
            effectiveBaseConsumption = averageFuelConsumption;
        }

        BigDecimal effectiveWeightLimit = vehicleWeightLimit != null ? vehicleWeightLimit : new BigDecimal("5");

        fuelVolume = calculateFuelConsumptionWithLoad(
                entity.getVehicleAssignmentEntity().getId(),
                effectiveBaseConsumption,
                effectiveWeightLimit,
                distanceTraveled
        );

        log.info("\u2705 Fuel consumption calculated: vehicleAssignment={}, baseConsumptionLPer100km={}, weightLimitTon={}, distanceKm={}, fuelVolumeL={}",
                entity.getVehicleAssignmentEntity().getId(),
                effectiveBaseConsumption,
                effectiveWeightLimit,
                distanceTraveled,
                fuelVolume);

        entity.setFuelVolume(fuelVolume);

        entity.setOdometerAtEndUrl(odometerAtEndUrl);
        entity.setOdometerReadingAtEnd(request.odometerReadingAtEnd());
        entity.setDistanceTraveled(distanceTraveled);

        final var updatedEntity = vehicleFuelConsumptionEntityService.save(entity);

        // ✅ CRITICAL: Manually update Order status to SUCCESSFUL after odometer end upload
        // This is the ONLY place where Order → SUCCESSFUL happens
        // OrderDetail status stays as DELIVERED (final state for successful delivery)
        try {
            UUID vehicleAssignmentId = entity.getVehicleAssignmentEntity().getId();

            // Get all OrderDetails for this assignment
            var allOrderDetails = orderDetailEntityService.findByVehicleAssignmentId(vehicleAssignmentId);

            if (!allOrderDetails.isEmpty()) {
                UUID orderId = allOrderDetails.get(0).getOrderEntity().getId();

                // Get ALL OrderDetails of the entire Order (across all trips)
                var allDetailsInOrder = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);

                // Condition 1: Check if ANY OrderDetail is DELIVERED
                boolean hasDelivered = allDetailsInOrder.stream()
                        .anyMatch(od -> "DELIVERED".equals(od.getStatus()));

                if (hasDelivered) {
                    // Condition 2: All vehicle assignments of this order must have end odometer recorded
                    var allAssignments = vehicleAssignmentEntityService.findVehicleAssignmentsWithOrderID(orderId);

                    boolean allAssignmentsHaveEndOdometer = allAssignments.stream().allMatch(assignment -> {
                        var fuelOpt = vehicleFuelConsumptionEntityService.findByVehicleAssignmentId(assignment.getId());
                        return fuelOpt.isPresent() && fuelOpt.get().getOdometerReadingAtEnd() != null;
                    });

                    if (allAssignmentsHaveEndOdometer) {
                        // Manually force Order → SUCCESSFUL (don't use aggregation)
                        var order = orderEntityService.findEntityById(orderId)
                                .orElseThrow(() -> new NotFoundException(
                                        "Order not found with ID: " + orderId,
                                        ErrorEnum.NOT_FOUND.getErrorCode()
                                ));

                        String oldStatus = order.getStatus();

                        // Only update if not already SUCCESSFUL or higher priority states
                        if (!"SUCCESSFUL".equals(oldStatus)
                                && !"COMPENSATION".equals(oldStatus)
                                && !"RETURNED".equals(oldStatus)) {

                            OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
                            order.setStatus(capstone_project.common.enums.OrderStatusEnum.SUCCESSFUL.name());
                            orderEntityService.save(order);

                            // Send WebSocket notification for status change
                            try {
                                orderStatusWebSocketService.sendOrderStatusChange(
                                    order.getId(),
                                    order.getOrderCode(),
                                    previousStatus,
                                    OrderStatusEnum.SUCCESSFUL
                                );
                            } catch (Exception e) {
                                log.error("Failed to send WebSocket notification for order status change: {}", e.getMessage());
                            }

                            log.info("✅ Order {} updated to SUCCESSFUL after odometer end upload (had {} DELIVERED details, all assignments completed)",
                                    orderId,
                                    allDetailsInOrder.stream().filter(od -> "DELIVERED".equals(od.getStatus())).count());
                        }
                    } else {
                        log.info("ℹ️ Not all vehicle assignments for order {} have end odometer yet - skipping SUCCESSFUL status update", orderId);
                    }
                }
            }
            
            // Update the IN_USE seal to REMOVED
            final var vehicleAssignment = entity.getVehicleAssignmentEntity();
            final var inUseSeal = sealEntityService.findByVehicleAssignment(
                    vehicleAssignment,
                    SealEnum.IN_USE.name());

            if (inUseSeal != null) {
                inUseSeal.setStatus(SealEnum.REMOVED.name());
                sealEntityService.save(inUseSeal);
                
            }
            
            // ✅ Update VehicleAssignment status to COMPLETED after odometer end upload
            if (vehicleAssignment != null) {
                String oldVaStatus = vehicleAssignment.getStatus();
                vehicleAssignment.setStatus(VehicleAssignmentStatusEnum.COMPLETED.name());
                vehicleAssignmentEntityService.save(vehicleAssignment);
                
            }
            
            // ✅ NEW: Update vehicle status to ACTIVE when end odometer is uploaded (trip completed)
            var vehicleEntity = vehicleAssignment.getVehicleEntity();
            if (vehicleEntity != null) {
                String oldVehicleStatus = vehicleEntity.getStatus();
                vehicleEntity.setStatus(VehicleStatusEnum.ACTIVE.name());
                vehicleEntityService.save(vehicleEntity);
                
                log.info("✅ Vehicle {} status updated from {} to ACTIVE after end odometer upload", 
                        vehicleEntity.getId(), oldVehicleStatus);
            }
        } catch (Exception e) {
            log.error("❌ Failed to auto-update OrderDetail/Order status or seal: {}", e.getMessage(), e);
            // Don't fail the main operation - odometer was updated successfully
        }

        return mapToResponse(updatedEntity);
    }

    @Override
    public VehicleFuelConsumptionResponse getVehicleFuelConsumptionById(UUID id) {

        final var entity = vehicleFuelConsumptionEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Vehicle fuel consumption not found with ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        return mapToResponse(entity);
    }

    @Override
    public VehicleFuelConsumptionResponse getVehicleFuelConsumptionByVehicleAssignmentId(UUID vehicleAssignmentId) {

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

    /**
     * ✅ NEW: Calculate fuel consumption based on journey segments and cargo load
     * Takes into account different load weights across different segments of the journey
     * 
     * @param vehicleAssignmentId The vehicle assignment ID
     * @param baseFuelConsumption Base fuel consumption rate (L/100km)
     * @param vehicleWeightLimit Vehicle weight limit in tons
     * @param totalDistanceKm Total distance from odometer (for validation)
     * @return Calculated fuel volume in liters
     */
    private BigDecimal calculateFuelConsumptionWithLoad(
            UUID vehicleAssignmentId,
            BigDecimal baseFuelConsumption,
            BigDecimal vehicleWeightLimit,
            BigDecimal totalDistanceKm) {

        try {
            // ✅ Get the most recent ACTIVE or COMPLETED journey history
            var activeJourney = journeyHistoryEntityService.findLatestActiveJourney(vehicleAssignmentId);

            if (activeJourney.isEmpty()) {
                log.warn("⚠️ No active journey found for vehicle assignment {}, falling back to simple odo-based calculation", vehicleAssignmentId);
                BigDecimal simpleFuel = totalDistanceKm.multiply(baseFuelConsumption)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                log.info("ℹ️ Simple fuel calculation (no journey): distanceKm={}, baseConsumptionLPer100km={}, fuelVolumeL={}",
                        totalDistanceKm, baseFuelConsumption, simpleFuel);
                return simpleFuel;
            }

            var journey = activeJourney.get();

            var segments = journey.getJourneySegments();

            if (segments == null || segments.isEmpty()) {
                log.warn("⚠️ No segments found in journey {}, falling back to simple odo-based calculation", journey.getId());
                BigDecimal simpleFuel = totalDistanceKm.multiply(baseFuelConsumption)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                log.info("ℹ️ Simple fuel calculation (no segments): distanceKm={}, baseConsumptionLPer100km={}, fuelVolumeL={}",
                        totalDistanceKm, baseFuelConsumption, simpleFuel);
                return simpleFuel;
            }

            // Sort segments by order
            segments.sort((s1, s2) -> s1.getSegmentOrder().compareTo(s2.getSegmentOrder()));

            // Get all order details for this assignment
            var orderDetails = orderDetailEntityService.findByVehicleAssignmentId(vehicleAssignmentId);

            if (log.isInfoEnabled()) {
                log.info("📝 Fuel calc (segments-only): journeyId={}, segmentsCount={}, orderDetailsCount={}, baseConsumptionLPer100km={}, vehicleWeightLimitTon={}, odoDistanceKm={}",
                        journey.getId(),
                        segments.size(),
                        orderDetails.size(),
                        baseFuelConsumption,
                        vehicleWeightLimit,
                        totalDistanceKm);
            }

            BigDecimal totalFuelVolumeFromSegments = BigDecimal.ZERO;
            BigDecimal totalSegmentDistance = BigDecimal.ZERO;

            for (JourneySegmentEntity segment : segments) {

                BigDecimal segmentDistanceKm = segment.getDistanceKilometers() != null
                        ? segment.getDistanceKilometers()
                        : BigDecimal.ZERO;

                totalSegmentDistance = totalSegmentDistance.add(segmentDistanceKm);

                // Determine cargo weight for this segment
                BigDecimal cargoWeight = getCargoWeightForSegment(segment, segments, orderDetails);

                // Calculate load factor (1.0 for empty, up to 1.3 for fully loaded)
                BigDecimal loadFactor = calculateLoadFactor(cargoWeight, vehicleWeightLimit);

                // Calculate fuel consumption for this segment
                BigDecimal segmentFuel = segmentDistanceKm
                        .multiply(baseFuelConsumption)
                        .multiply(loadFactor)
                        .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

                totalFuelVolumeFromSegments = totalFuelVolumeFromSegments.add(segmentFuel);

                if (log.isInfoEnabled()) {
                    log.info("➡️ Segment fuel calc: journeyId={}, segmentOrder={}, startPoint='{}', endPoint='{}', distanceKm={}, cargoWeightKg={}, loadFactor={}, segmentFuelL={}",
                            journey.getId(),
                            segment.getSegmentOrder(),
                            segment.getStartPointName(),
                            segment.getEndPointName(),
                            segmentDistanceKm,
                            cargoWeight,
                            loadFactor,
                            segmentFuel.setScale(3, RoundingMode.HALF_UP));
                }
            }

            if (totalSegmentDistance.compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("⚠️ Total segment distance is zero or negative for journey {}, falling back to simple odo-based calculation", journey.getId());
                BigDecimal simpleFuel = totalDistanceKm.multiply(baseFuelConsumption)
                        .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

                log.info("ℹ️ Simple fuel calculation (zero segment distance): distanceKm={}, baseConsumptionLPer100km={}, fuelVolumeL={}",
                        totalDistanceKm, baseFuelConsumption, simpleFuel);
                return simpleFuel;
            }

            BigDecimal result = totalFuelVolumeFromSegments.setScale(2, RoundingMode.HALF_UP);

            if (log.isInfoEnabled()) {
                log.info("📊 Segment-based fuel summary: journeyId={}, totalSegmentDistanceKm={}, baseConsumptionLPer100km={}, odoDistanceKm={}, totalFuelL={}",
                        journey.getId(),
                        totalSegmentDistance.setScale(3, RoundingMode.HALF_UP),
                        baseFuelConsumption,
                        totalDistanceKm,
                        result);
            }

            return result;

        } catch (Exception e) {
            log.error("❌ Error calculating fuel with load: {}, falling back to simple calculation", e.getMessage(), e);
            // Fallback to simple calculation if something goes wrong
            BigDecimal simpleFuel = totalDistanceKm.multiply(baseFuelConsumption)
                    .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

            log.info("ℹ️ Simple fuel calculation (exception): distanceKm={}, baseConsumptionLPer100km={}, fuelVolumeL={}",
                    totalDistanceKm, baseFuelConsumption, simpleFuel);
            return simpleFuel;
        }
    }

    /**
     * Determine cargo weight for a specific segment based on segment type
     * 
     * ✅ FIXED: Use `weight` field (in tons) and convert to kg
     * - `weight`: Actual weight in TONS - used for calculation
     * - `weightBaseUnit`: Display-only field for frontend
     * 
     * Segment Types:
     * - Carrier → Pickup: 0 kg (empty truck)
     * - Pickup → Delivery: Full cargo weight (all packages on this trip)
     * - Delivery → Carrier: 0 kg (empty after delivery)
     * - Delivery → Pickup (return): Weight of returned packages only
     * - Pickup → Carrier (return): 0 kg (empty after return)
     * 
     * ✅ Status filtering removed: All OrderDetails belong to this VehicleAssignment,
     * so they're all relevant. Journey segment type determines load calculation.
     */
    private BigDecimal getCargoWeightForSegment(
            JourneySegmentEntity segment,
            java.util.List<JourneySegmentEntity> allSegments,
            java.util.List<OrderDetailEntity> orderDetails) {

        String startPoint = segment.getStartPointName().toLowerCase();
        String endPoint = segment.getEndPointName().toLowerCase();

        // Carrier → Pickup: Empty
        if (startPoint.contains("carrier") && endPoint.contains("pickup")) {
            if (log.isInfoEnabled()) {
                log.info("\uD83D\uDE9A Cargo weight: Carrier→Pickup segmentOrder={}, assuming empty (0 kg)",
                        segment.getSegmentOrder());
            }
            return BigDecimal.ZERO;
        }
        
        // Pickup → Delivery: Full load (all packages on this vehicle assignment)
        // ✅ CRITICAL FIX: Use `getWeight()` (tons) and convert to kg by multiplying 1000
        if (startPoint.contains("pickup") && endPoint.contains("delivery")) {

            var validOrderDetails = orderDetails.stream()
                    .filter(od -> !("RETURNED".equals(od.getStatus()) || "CANCELLED".equals(od.getStatus())))
                    .collect(java.util.stream.Collectors.toList());

            BigDecimal totalWeightTons = validOrderDetails.stream()
                    .map(OrderDetailEntity::getWeightTons)
                    .filter(w -> w != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Convert tons to kg
            BigDecimal weightKg = totalWeightTons.multiply(new BigDecimal("1000"));

            if (log.isInfoEnabled()) {
                log.info("\uD83D\uDE9A Cargo weight: Pickup→Delivery segmentOrder={}, packagesCount={}, totalWeightTons={}, weightKg={}",
                        segment.getSegmentOrder(),
                        validOrderDetails.size(),
                        totalWeightTons,
                        weightKg);
            }

            return weightKg;
        }
        
        // Delivery → Carrier: Empty (after successful delivery)
        if (startPoint.contains("delivery") && endPoint.contains("carrier")) {
            if (log.isInfoEnabled()) {
                log.info("\uD83D\uDE9A Cargo weight: Delivery→Carrier segmentOrder={}, assuming empty (0 kg)",
                        segment.getSegmentOrder());
            }
            return BigDecimal.ZERO;
        }
        
        // Delivery → Pickup (return flow): Weight of returned packages
        // ✅ CRITICAL FIX: Use `getWeight()` (tons) and convert to kg
        if (startPoint.contains("delivery") && endPoint.contains("pickup")) {

            var returnedOrderDetails = orderDetails.stream()
                    .filter(od -> "RETURNED".equals(od.getStatus()))
                    .collect(java.util.stream.Collectors.toList());

            BigDecimal totalWeightTons = returnedOrderDetails.stream()
                    .map(OrderDetailEntity::getWeightTons)
                    .filter(w -> w != null)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Convert tons to kg
            BigDecimal weightKg = totalWeightTons.multiply(new BigDecimal("1000"));

            if (log.isInfoEnabled()) {
                log.info("\uD83D\uDE9A Cargo weight: Delivery→Pickup (return) segmentOrder={}, returnedPackagesCount={}, totalWeightTons={}, weightKg={}",
                        segment.getSegmentOrder(),
                        returnedOrderDetails.size(),
                        totalWeightTons,
                        weightKg);
            }

            return weightKg;
        }
        
        // Pickup → Carrier (return flow): Empty (after returning packages to pickup point)
        if (startPoint.contains("pickup") && endPoint.contains("carrier")) {
            if (log.isInfoEnabled()) {
                log.info("\uD83D\uDE9A Cargo weight: Pickup→Carrier (return) segmentOrder={}, assuming empty (0 kg)",
                        segment.getSegmentOrder());
            }
            return BigDecimal.ZERO;
        }
        
        // Default: assume empty for unknown segment types
        log.warn("⚠️ Unknown segment type: {} → {} (segmentOrder={}), assuming empty", startPoint, endPoint, segment.getSegmentOrder());
        return BigDecimal.ZERO;
    }

    /**
     * Calculate load factor based on cargo weight vs vehicle capacity
     * 
     * Formula: loadFactor = 1.0 + (loadPercentage / 100) * 0.3
     * 
     * Logic (based on logistics research):
     * - Empty truck (0%): 1.0 (baseline fuel consumption)
     * - 25% loaded: 1.075 (7.5% increase)
     * - 50% loaded: 1.15 (15% increase)
     * - 75% loaded: 1.225 (22.5% increase)
     * - Fully loaded (100%): 1.3 (30% increase)
     * 
     * The 0.3 multiplier represents maximum fuel consumption increase
     * at full load (30% is standard for trucks based on empirical data)
     * 
     * Reference: European Commission - Energy Consumption and CO2 Emissions
     * of Freight Transport (2019) shows 25-35% fuel increase at full load
     */
    private BigDecimal calculateLoadFactor(BigDecimal cargoWeightKg, BigDecimal vehicleWeightLimitTon) {

        if (cargoWeightKg == null || cargoWeightKg.compareTo(BigDecimal.ZERO) == 0) {
            if (log.isInfoEnabled()) {
                log.info("\u2699\uFE0F Load factor: empty truck (cargoWeightKg={}), loadFactor=1.000", cargoWeightKg);
            }
            return BigDecimal.ONE; // Empty truck
        }
        
        if (vehicleWeightLimitTon == null || vehicleWeightLimitTon.compareTo(BigDecimal.ZERO) == 0) {
            log.warn("⚠️ Vehicle weight limit is null or zero, using default load factor = 1.0 (cargoWeightKg={})", cargoWeightKg);
            return BigDecimal.ONE;
        }
        
        // Convert vehicle weight limit from tons to kg
        BigDecimal vehicleWeightLimitKg = vehicleWeightLimitTon.multiply(new BigDecimal("1000"));

        // Calculate load percentage (0-100%)
        BigDecimal loadPercentage = cargoWeightKg
                .multiply(new BigDecimal("100"))
                .divide(vehicleWeightLimitKg, 2, RoundingMode.HALF_UP);

        // Cap at 100% if somehow cargo exceeds vehicle limit
        if (loadPercentage.compareTo(new BigDecimal("100")) > 0) {
            log.warn("⚠️ Cargo weight ({} kg) exceeds vehicle limit ({} kg), capping at 100%",
                    cargoWeightKg, vehicleWeightLimitKg);
            loadPercentage = new BigDecimal("100");
        }
        
        // Calculate load factor: 1.0 + (loadPercentage / 100) * 0.3
        // Example: 50% load = 1.0 + (50/100) * 0.3 = 1.15 (15% fuel increase)
        BigDecimal loadFactor = BigDecimal.ONE.add(
                loadPercentage.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                        .multiply(new BigDecimal("0.3"))
        );

        BigDecimal scaledLoadFactor = loadFactor.setScale(3, RoundingMode.HALF_UP);

        if (log.isInfoEnabled()) {
            log.info("\u2699\uFE0F Load factor: cargoWeightKg={}, vehicleLimitTon={}, vehicleLimitKg={}, loadPercentage={}, loadFactor={}",
                    cargoWeightKg,
                    vehicleWeightLimitTon,
                    vehicleWeightLimitKg,
                    loadPercentage,
                    scaledLoadFactor);
        }

        return scaledLoadFactor;
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
