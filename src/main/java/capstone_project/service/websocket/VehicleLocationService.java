package capstone_project.service.websocket;

import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.dtos.websocket.VehicleLocationMessage;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.repositories.vehicle.VehicleAssignmentRepository;
import capstone_project.repository.repositories.order.order.OrderDetailRepository;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import capstone_project.entity.order.order.OrderDetailEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleLocationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final VehicleEntityService vehicleEntityService;
    private final VehicleAssignmentService vehicleAssignmentService;
    private final VehicleAssignmentRepository vehicleAssignmentRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final OrderEntityService orderEntityService;

    private static final String TOPIC_ALL_VEHICLES = "/topic/vehicles/locations";
    private static final String TOPIC_VEHICLE_PREFIX = "/topic/vehicles/";
    private static final String TOPIC_ORDER_VEHICLES_PREFIX = "/topic/orders/";

    /**
     * Broadcast updated vehicle location to all subscribers
     */
    public void broadcastVehicleLocation(VehicleEntity vehicle) {
        if (vehicle == null || vehicle.getCurrentLatitude() == null || vehicle.getCurrentLongitude() == null) {
            return;
        }

        VehicleLocationMessage message = VehicleLocationMessage.builder()
                .vehicleId(vehicle.getId())
                .latitude(vehicle.getCurrentLatitude())
                .longitude(vehicle.getCurrentLongitude())
                .licensePlateNumber(vehicle.getLicensePlateNumber())
                .build();

        broadcastVehicleLocation(message);
    }

    /**
     * Broadcast updated vehicle location to all subscribers using the message DTO
     */
    public void broadcastVehicleLocation(VehicleLocationMessage message) {
        // Calculate velocity for smooth frontend interpolation
        enhanceMessageWithVelocity(message);
        
        // Broadcast to all vehicles topic for web clients
        messagingTemplate.convertAndSend(TOPIC_ALL_VEHICLES, message);

        // Broadcast to specific vehicle topic for clients tracking specific vehicle
        messagingTemplate.convertAndSend(TOPIC_VEHICLE_PREFIX + message.getVehicleId(), message);

        // Find all orders associated with this vehicle and broadcast to order topics
        broadcastToOrderTopics(message);

    }
    
    /**
     * Enhance message with velocity data for smooth frontend interpolation
     * Uses actual speed and bearing from mobile if available, otherwise calculates defaults
     */
    private void enhanceMessageWithVelocity(VehicleLocationMessage message) {
        // Use speed from message if available, otherwise default
        double speedKmh = message.getSpeed() != null ? 
            message.getSpeed().doubleValue() : 45.0;
        
        // Use bearing from message if available, otherwise default
        double bearingDegrees = message.getBearing() != null ? 
            message.getBearing().doubleValue() : 0.0;
        
        // Ensure values are set
        if (message.getSpeed() == null) {
            message.setSpeed(BigDecimal.valueOf(speedKmh));
        }
        if (message.getBearing() == null) {
            message.setBearing(BigDecimal.valueOf(bearingDegrees));
        }
        
        // Calculate velocity components (degrees per second) for smooth interpolation
        double speedMs = speedKmh / 3.6; // Convert km/h to m/s
        double degreesPerSecond = speedMs / 111000.0; // Approximate meters per degree
        
        // Calculate velocity components based on bearing
        double bearingRad = Math.toRadians(bearingDegrees);
        double velocityLat = degreesPerSecond * Math.cos(bearingRad); // Northward component
        double velocityLng = degreesPerSecond * Math.sin(bearingRad); // Eastward component
        
        message.setVelocityLat(BigDecimal.valueOf(velocityLat));
        message.setVelocityLng(BigDecimal.valueOf(velocityLng));
    }

    /**
     * Broadcast vehicle location to all order topics that have this vehicle assigned
     * IMPORTANT: Deduplicates orders to prevent sending duplicate vehicle messages
     * when multiple order details share the same vehicle assignment
     */
    private void broadcastToOrderTopics(VehicleLocationMessage message) {
        if (message == null || message.getVehicleId() == null) {
            return;
        }

        try {
            // Find all active vehicle assignments for this vehicle with eagerly fetched drivers
            List<VehicleAssignmentEntity> assignments = vehicleAssignmentRepository
                    .findByVehicleEntityIdWithDrivers(message.getVehicleId());

            // Track which orders we've already broadcast to (to avoid duplicates)
            java.util.Set<UUID> broadcastedOrders = new java.util.HashSet<>();

            // For each assignment, find active order details
            for (VehicleAssignmentEntity assignment : assignments) {

                // Check Order status (not OrderDetail status) for real-time tracking
                // Include all statuses from PICKING_UP onwards as defined in frontend OrderStatusEnum
                List<OrderDetailEntity> activeOrderDetails = orderDetailRepository
                        .findActiveOrderDetailsByVehicleAssignmentId(
                                assignment.getId(),
                                List.of("PICKING_UP", "ON_DELIVERED", "ONGOING_DELIVERED", 
                                       "DELIVERED", "IN_TROUBLES", "RESOLVED", "COMPENSATION",
                                       "RETURNING", "RETURNED"));

                // Group order details by order ID to avoid duplicate broadcasts
                for (OrderDetailEntity orderDetail : activeOrderDetails) {
                    String orderStatus = orderDetail.getOrderEntity() != null ? 
                            orderDetail.getOrderEntity().getStatus() : "NULL";

                    if (orderDetail.getOrderEntity() != null && orderDetail.getOrderEntity().getId() != null) {
                        UUID orderId = orderDetail.getOrderEntity().getId();
                        
                        // Skip if we've already broadcast to this order
                        if (broadcastedOrders.contains(orderId)) {
                            
                            continue;
                        }
                        
                        String orderTopic = TOPIC_ORDER_VEHICLES_PREFIX + orderId + "/vehicles";

                        // Get enhanced message with assignment details using eager-fetched vehicle
                        VehicleEntity vehicle = vehicleEntityService.findByVehicleIdWithVehicleType(message.getVehicleId()).orElse(null);
                        String orderDetailStatus = orderDetail.getStatus() != null ? orderDetail.getStatus() : "UNKNOWN";
                        VehicleLocationMessage enhancedMessage = buildEnhancedLocationMessage(vehicle, assignment, orderDetailStatus);
                        
                        if (enhancedMessage != null) {
                            // CRITICAL: Copy bearing, speed, and velocity from original message
                            // These come from mobile app and are essential for smooth tracking
                            enhancedMessage.setBearing(message.getBearing());
                            enhancedMessage.setSpeed(message.getSpeed());
                            enhancedMessage.setVelocityLat(message.getVelocityLat());
                            enhancedMessage.setVelocityLng(message.getVelocityLng());
                            
                            // DEBUG: Log speed values to identify discrepancy
                            log.info("=== [WEBSOCKET SPEED DEBUG] Vehicle {} ({}) - Speed from mobile: {} km/h, Speed being sent: {} km/h", 
                                    message.getVehicleId(), 
                                    message.getLicensePlateNumber(),
                                    message.getSpeed() != null ? message.getSpeed().doubleValue() + "" : "NULL",
                                    enhancedMessage.getSpeed() != null ? enhancedMessage.getSpeed().doubleValue() + "" : "NULL");

                            messagingTemplate.convertAndSend(orderTopic, enhancedMessage);
                            broadcastedOrders.add(orderId); // Mark as broadcast
                            
                        } else {
                            log.warn("=== [broadcastToOrderTopics] Enhanced message is NULL for vehicle {}", 
                                    message.getVehicleId());
                        }
                    } else {
                        log.warn("=== [broadcastToOrderTopics] OrderDetail {} has no order entity or order ID", 
                                orderDetail.getId());
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error broadcasting to order topics for vehicle {}: {}", 
                    message.getVehicleId(), e.getMessage(), e);
        }
    }

    /**
     * Find the order ID that has the specified vehicle assignment
     * @param assignmentId The vehicle assignment ID
     * @return The order ID or null if no order found
     */
    private UUID findOrderWithAssignedVehicle(UUID assignmentId) {
        try {
            // Mỗi vehicle assignment chỉ gắn với một đơn hàng duy nhất
            // Sử dụng findVehicleAssignmentOrder để lấy order từ assignmentId
            Optional<OrderEntity> orderOpt = orderEntityService.findVehicleAssignmentOrder(assignmentId);

            // Nếu tìm thấy order, trả về ID của nó
            return orderOpt.map(OrderEntity::getId).orElse(null);
        } catch (Exception e) {
            log.error("Error finding order for vehicle assignment {}: {}", assignmentId, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Broadcast vehicle location update with basic parameters
     */
    public void broadcastVehicleLocation(UUID vehicleId, BigDecimal latitude, BigDecimal longitude, String licensePlateNumber) {
        broadcastVehicleLocation(vehicleId, latitude, longitude, licensePlateNumber, null, null);
    }
    
    /**
     * Broadcast vehicle location update with bearing and speed from mobile
     */
    public void broadcastVehicleLocation(UUID vehicleId, BigDecimal latitude, BigDecimal longitude, 
                                        String licensePlateNumber, BigDecimal bearing, BigDecimal speed) {
        VehicleLocationMessage message = VehicleLocationMessage.builder()
                .vehicleId(vehicleId)
                .latitude(latitude)
                .longitude(longitude)
                .licensePlateNumber(licensePlateNumber)
                .bearing(bearing)
                .speed(speed)
                .build();

        broadcastVehicleLocation(message);
    }

    /**
     * Get enhanced vehicle location data with all details for better UX
     * @param vehicleId Vehicle ID
     * @return Enhanced vehicle location data or null if vehicle not found
     */
    public VehicleLocationMessage getEnhancedVehicleLocation(UUID vehicleId) {
        if (vehicleId == null) {
            return null;
        }

        // Get vehicle entity with full details
        VehicleEntity vehicle = vehicleEntityService.findByVehicleId(vehicleId).orElse(null);
        if (vehicle == null || vehicle.getCurrentLatitude() == null || vehicle.getCurrentLongitude() == null) {
            return null;
        }

        // Build basic location message
        VehicleLocationMessage.VehicleLocationMessageBuilder builder = VehicleLocationMessage.builder()
                .vehicleId(vehicle.getId())
                .latitude(vehicle.getCurrentLatitude())
                .longitude(vehicle.getCurrentLongitude())
                .licensePlateNumber(vehicle.getLicensePlateNumber())
                .lastUpdated(vehicle.getLastUpdated())
                .manufacturer(vehicle.getManufacturer());

        // Add vehicle type information if available
        if (vehicle.getVehicleTypeEntity() != null) {
            builder.vehicleTypeName(vehicle.getVehicleTypeEntity().getVehicleTypeName());
        }

        return builder.build();
    }

    /**
     * Get enhanced location information for a vehicle with its assignment details
     * @param vehicle Vehicle entity
     * @param assignmentEntity Assignment entity with driver information
     * @param orderDetailStatus Status of the order detail for this assignment
     * @return Enhanced vehicle location message
     */
    private VehicleLocationMessage buildEnhancedLocationMessage(VehicleEntity vehicle, VehicleAssignmentEntity assignmentEntity, String orderDetailStatus) {
        if (vehicle == null || vehicle.getCurrentLatitude() == null || vehicle.getCurrentLongitude() == null) {
            return null;
        }

        // Create basic location message
        VehicleLocationMessage.VehicleLocationMessageBuilder builder = VehicleLocationMessage.builder()
                .vehicleId(vehicle.getId())
                .latitude(vehicle.getCurrentLatitude())
                .longitude(vehicle.getCurrentLongitude())
                .licensePlateNumber(vehicle.getLicensePlateNumber())
                .lastUpdated(vehicle.getLastUpdated())
                .manufacturer(vehicle.getManufacturer());

        // Add vehicle type information if available - handle lazy loading safely
        try {
            if (vehicle.getVehicleTypeEntity() != null) {
                String vehicleTypeName = null;
                try {
                    vehicleTypeName = vehicle.getVehicleTypeEntity().getVehicleTypeName();
                } catch (Exception e) {
                    log.warn("Could not load vehicle type name for vehicle {}: {}",
                             vehicle.getId(), e.getMessage());
                }

                if (vehicleTypeName != null) {
                    builder.vehicleTypeName(vehicleTypeName);
                }
            }
        } catch (Exception e) {
            // Log and continue - don't let this stop the whole message building
            log.warn("Error accessing vehicle type for vehicle {}: {}",
                     vehicle.getId(), e.getMessage());
        }

        // Add assignment information if available
        if (assignmentEntity != null) {
            builder.vehicleAssignmentId(assignmentEntity.getId())
                   .trackingCode(assignmentEntity.getTrackingCode())
                   .orderDetailStatus(orderDetailStatus);

            // Add driver information - handle lazy loading safely
            try {
                if (assignmentEntity.getDriver1() != null) {
                    try {
                        builder.driver1Name(assignmentEntity.getDriver1().getUser().getFullName())
                               .driver1Phone(assignmentEntity.getDriver1().getUser().getPhoneNumber());
                    } catch (Exception e) {
                        log.warn("Error accessing driver1 details for assignment {}: {}",
                                 assignmentEntity.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Error accessing driver1 for assignment {}: {}",
                         assignmentEntity.getId(), e.getMessage());
            }

            try {
                if (assignmentEntity.getDriver2() != null) {
                    try {
                        builder.driver2Name(assignmentEntity.getDriver2().getUser().getFullName())
                               .driver2Phone(assignmentEntity.getDriver2().getUser().getPhoneNumber());
                    } catch (Exception e) {
                        log.warn("Error accessing driver2 details for assignment {}: {}",
                                 assignmentEntity.getId(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Error accessing driver2 for assignment {}: {}",
                         assignmentEntity.getId(), e.getMessage());
            }
        }

        return builder.build();
    }

    /**
     * Get locations of all vehicles for a specific order with enhanced details
     * IMPORTANT: Deduplicates vehicles by vehicleId to prevent duplicate entries
     * when multiple order details share the same vehicle assignment
     * @param orderId Order ID
     * @return List of unique enhanced vehicle location messages or empty list if none found
     */
    public List<VehicleLocationMessage> getOrderVehicleLocations(UUID orderId) {
        if (orderId == null) {
            return new ArrayList<>();
        }

        // Get all vehicle assignments for this order
        List<VehicleAssignmentResponse> assignments = vehicleAssignmentService.getListVehicleAssignmentByOrderID(orderId);
        List<VehicleLocationMessage> vehicleLocations = new ArrayList<>();
        
        // Track which vehicles we've already added (to avoid duplicates)
        java.util.Set<UUID> processedVehicles = new java.util.HashSet<>();

        for (VehicleAssignmentResponse assignment : assignments) {
            if (assignment.vehicleId() != null) {
                // Skip if we've already processed this vehicle
                if (processedVehicles.contains(assignment.vehicleId())) {
                    
                    continue;
                }
                
                try {
                    // Get vehicle entity with eagerly fetched vehicle type
                    VehicleEntity vehicle = vehicleEntityService.findByVehicleIdWithVehicleType(assignment.vehicleId()).orElse(null);

                    if (vehicle != null) {
                        // Get assignment entity with eagerly fetched drivers
                        VehicleAssignmentEntity assignmentEntity = vehicleAssignmentRepository
                                .findByIdWithDrivers(assignment.id()).orElse(null);

                        // Build enhanced message
                        VehicleLocationMessage.VehicleLocationMessageBuilder builder = VehicleLocationMessage.builder()
                                .vehicleId(vehicle.getId())
                                .latitude(vehicle.getCurrentLatitude())
                                .longitude(vehicle.getCurrentLongitude())
                                .licensePlateNumber(vehicle.getLicensePlateNumber())
                                .lastUpdated(vehicle.getLastUpdated())
                                .manufacturer(vehicle.getManufacturer());

                        // Add vehicle type - now eagerly loaded
                        if (vehicle.getVehicleTypeEntity() != null) {
                            builder.vehicleTypeName(vehicle.getVehicleTypeEntity().getVehicleTypeName());
                        }

                        // Add assignment info if available
                        if (assignmentEntity != null) {
                            builder.vehicleAssignmentId(assignmentEntity.getId())
                                   .trackingCode(assignmentEntity.getTrackingCode());

                            // Get first order detail status for this assignment
                            List<OrderDetailEntity> orderDetails = orderDetailRepository
                                    .findByVehicleAssignmentEntityId(assignmentEntity.getId());
                            String orderDetailStatus = !orderDetails.isEmpty() && orderDetails.get(0).getStatus() != null
                                    ? orderDetails.get(0).getStatus() 
                                    : "UNKNOWN";
                            builder.orderDetailStatus(orderDetailStatus);

                            // Add driver1 info - now eagerly loaded
                            if (assignmentEntity.getDriver1() != null && assignmentEntity.getDriver1().getUser() != null) {
                                builder.driver1Name(assignmentEntity.getDriver1().getUser().getFullName())
                                       .driver1Phone(assignmentEntity.getDriver1().getUser().getPhoneNumber());
                            }

                            // Add driver2 info - now eagerly loaded
                            if (assignmentEntity.getDriver2() != null && assignmentEntity.getDriver2().getUser() != null) {
                                builder.driver2Name(assignmentEntity.getDriver2().getUser().getFullName())
                                       .driver2Phone(assignmentEntity.getDriver2().getUser().getPhoneNumber());
                            }
                        }

                        // Add the built message to the list
                        VehicleLocationMessage locationMessage = builder.build();
                        vehicleLocations.add(locationMessage);
                        processedVehicles.add(assignment.vehicleId()); // Mark as processed
                    }
                } catch (Exception e) {
                    log.error("Error processing vehicle location for assignment {}: {}",
                            assignment.id(), e.getMessage(), e);
                    // Continue with other assignments
                }
            }
        }

        return vehicleLocations;
    }

    /**
     * Send vehicle locations for an order to the appropriate WebSocket topic
     * @param orderId Order ID
     */
    public void sendOrderVehicleLocations(UUID orderId) {
        if (orderId == null) {
            return;
        }

        // Get enhanced vehicle locations
        List<VehicleLocationMessage> vehicleLocations = getOrderVehicleLocations(orderId);

        // Send to order-specific topic (matching browser subscription pattern)
        String topic = TOPIC_ORDER_VEHICLES_PREFIX + orderId + "/vehicles";
        messagingTemplate.convertAndSend(topic, vehicleLocations);

    }

    /**
     * Send vehicle locations for an order identified by order code to the appropriate WebSocket topic
     * @param orderCode Order code string
     */
    public void sendOrderVehicleLocationsByOrderCode(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            log.warn("Null or empty order code provided");
            return;
        }

        // Find the order by its code using the injected OrderEntityService
        Optional<OrderEntity> orderOpt = orderEntityService.findByOrderCode(orderCode);

        if (orderOpt.isPresent()) {
            OrderEntity order = orderOpt.get();
            UUID orderId = order.getId();

            // Get enhanced vehicle locations
            List<VehicleLocationMessage> vehicleLocations = getOrderVehicleLocations(orderId);

            // Send to order-specific topic (matching browser subscription pattern)
            String topic = TOPIC_ORDER_VEHICLES_PREFIX + orderId + "/vehicles";
            messagingTemplate.convertAndSend(topic, vehicleLocations);

        } else {
            log.warn("No order found with order code: {}", orderCode);
        }
    }
}
