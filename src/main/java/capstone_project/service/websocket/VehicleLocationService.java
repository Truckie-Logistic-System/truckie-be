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
        // Broadcast to all vehicles topic for web clients
        messagingTemplate.convertAndSend(TOPIC_ALL_VEHICLES, message);

        // Broadcast to specific vehicle topic for clients tracking specific vehicle
        messagingTemplate.convertAndSend(TOPIC_VEHICLE_PREFIX + message.getVehicleId(), message);

        // Find all orders associated with this vehicle and broadcast to order topics
        broadcastToOrderTopics(message);

        log.debug("Broadcast vehicle location: vehicleId={}, lat={}, lng={}",
                message.getVehicleId(), message.getLatitude(), message.getLongitude());
    }

    /**
     * Broadcast vehicle location to all order topics that have this vehicle assigned
     */
    private void broadcastToOrderTopics(VehicleLocationMessage message) {
        if (message == null || message.getVehicleId() == null) {
            return;
        }

        try {
            // Find all active vehicle assignments for this vehicle with eagerly fetched drivers
            List<VehicleAssignmentEntity> assignments = vehicleAssignmentRepository
                    .findByVehicleEntityIdWithDrivers(message.getVehicleId());
            
            log.debug("=== [broadcastToOrderTopics] Found {} assignments for vehicle {}", 
                    assignments.size(), message.getVehicleId());

            // For each assignment, find active order details
            for (VehicleAssignmentEntity assignment : assignments) {
                log.debug("=== [broadcastToOrderTopics] Processing assignment: {}, status: {}", 
                        assignment.getId(), assignment.getStatus());
                
                // Check Order status (not OrderDetail status) for real-time tracking
                // Include all statuses from PICKING_UP onwards as defined in frontend OrderStatusEnum
                List<OrderDetailEntity> activeOrderDetails = orderDetailRepository
                        .findActiveOrderDetailsByVehicleAssignmentId(
                                assignment.getId(),
                                List.of("PICKING_UP", "ON_DELIVERED", "ONGOING_DELIVERED", 
                                       "DELIVERED", "IN_TROUBLES", "RESOLVED", "COMPENSATION",
                                       "SUCCESSFUL", "RETURNING", "RETURNED"));
                
                log.debug("=== [broadcastToOrderTopics] Found {} active order details for assignment {}", 
                        activeOrderDetails.size(), assignment.getId());

                // Broadcast to each order's topic
                for (OrderDetailEntity orderDetail : activeOrderDetails) {
                    String orderStatus = orderDetail.getOrderEntity() != null ? 
                            orderDetail.getOrderEntity().getStatus() : "NULL";
                    log.debug("=== [broadcastToOrderTopics] Processing order detail: {}, orderDetailStatus: {}, ORDER STATUS: {}", 
                            orderDetail.getId(), orderDetail.getStatus(), orderStatus);
                    
                    if (orderDetail.getOrderEntity() != null && orderDetail.getOrderEntity().getId() != null) {
                        UUID orderId = orderDetail.getOrderEntity().getId();
                        String orderTopic = TOPIC_ORDER_VEHICLES_PREFIX + orderId + "/vehicles";
                        
                        log.debug("=== [broadcastToOrderTopics] Broadcasting to order {} topic: {}", 
                                orderId, orderTopic);
                        
                        // Get enhanced message with assignment details using eager-fetched vehicle
                        VehicleEntity vehicle = vehicleEntityService.findByVehicleIdWithVehicleType(message.getVehicleId()).orElse(null);
                        VehicleLocationMessage enhancedMessage = buildEnhancedLocationMessage(vehicle, assignment);
                        
                        if (enhancedMessage != null) {
                            messagingTemplate.convertAndSend(orderTopic, enhancedMessage);
                            log.info("=== [broadcastToOrderTopics] SUCCESSFULLY broadcast vehicle {} location to order {} topic: {}", 
                                    message.getVehicleId(), orderId, orderTopic);
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
        VehicleLocationMessage message = VehicleLocationMessage.builder()
                .vehicleId(vehicleId)
                .latitude(latitude)
                .longitude(longitude)
                .licensePlateNumber(licensePlateNumber)
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
     * @return Enhanced vehicle location message
     */
    private VehicleLocationMessage buildEnhancedLocationMessage(VehicleEntity vehicle, VehicleAssignmentEntity assignmentEntity) {
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
                   .assignmentStatus(assignmentEntity.getStatus());

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
     * @param orderId Order ID
     * @return List of enhanced vehicle location messages or empty list if none found
     */
    public List<VehicleLocationMessage> getOrderVehicleLocations(UUID orderId) {
        if (orderId == null) {
            return new ArrayList<>();
        }

        // Get all vehicle assignments for this order
        List<VehicleAssignmentResponse> assignments = vehicleAssignmentService.getListVehicleAssignmentByOrderID(orderId);
        List<VehicleLocationMessage> vehicleLocations = new ArrayList<>();

        for (VehicleAssignmentResponse assignment : assignments) {
            if (assignment.vehicleId() != null) {
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
                                   .trackingCode(assignmentEntity.getTrackingCode())
                                   .assignmentStatus(assignmentEntity.getStatus());

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

        log.debug("Sent enhanced locations of {} vehicles for order {} to topic {}", 
                vehicleLocations.size(), orderId, topic);
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

            log.debug("Sent enhanced locations of {} vehicles for order code {} to topic {}", 
                    vehicleLocations.size(), orderCode, topic);
        } else {
            log.warn("No order found with order code: {}", orderCode);
        }
    }
}
