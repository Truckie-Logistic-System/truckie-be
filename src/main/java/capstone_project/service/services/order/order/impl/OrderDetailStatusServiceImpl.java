package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.service.services.order.order.OrderDetailStatusService;
import capstone_project.service.services.order.order.OrderStatusWebSocketService;
import capstone_project.service.services.notification.NotificationService;
import capstone_project.service.services.notification.NotificationBuilder;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
import capstone_project.repository.entityServices.auth.UserEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of OrderDetailStatusService
 * Manages status updates for OrderDetails and auto-syncs Order status
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderDetailStatusServiceImpl implements OrderDetailStatusService {
    
    private final OrderDetailEntityService orderDetailEntityService;
    private final OrderEntityService orderEntityService;
    private final OrderStatusWebSocketService orderStatusWebSocketService;
    private final NotificationService notificationService;
    private final UserEntityService userEntityService;
    private final capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService vehicleAssignmentEntityService;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    @Override
    @Transactional
    public void updateOrderDetailStatusByAssignment(UUID vehicleAssignmentId, OrderDetailStatusEnum newStatus) {

        // Validate input
        if (vehicleAssignmentId == null) {
            throw new BadRequestException(
                "Vehicle assignment ID cannot be null",
                ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }
        
        if (newStatus == null) {
            throw new BadRequestException(
                "New status cannot be null",
                ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }
        
        // Find all OrderDetails for this vehicle assignment
        List<OrderDetailEntity> orderDetails = orderDetailEntityService
            .findByVehicleAssignmentId(vehicleAssignmentId);
        
        if (orderDetails.isEmpty()) {
            log.warn("No order details found for vehicle assignment {}", vehicleAssignmentId);
            throw new NotFoundException(
                "No order details found for this vehicle assignment",
                ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        
        // ✅ CRITICAL FIX: Filter out OrderDetails with active issues (IN_TROUBLES)
        // When driver uploads delivery photos, we should NOT override IN_TROUBLES status
        // because those packages have reported issues and need separate resolution
        List<OrderDetailEntity> eligibleOrderDetails = new ArrayList<>();
        List<OrderDetailEntity> skippedOrderDetails = new ArrayList<>();
        
        for (OrderDetailEntity detail : orderDetails) {
            OrderDetailStatusEnum currentStatus = detail.getStatus() != null 
                ? OrderDetailStatusEnum.valueOf(detail.getStatus())
                : OrderDetailStatusEnum.PENDING;
            
            // Skip OrderDetails that are IN_TROUBLES - they require separate issue resolution
            if (currentStatus == OrderDetailStatusEnum.IN_TROUBLES) {
                skippedOrderDetails.add(detail);
                log.info("⚠️ Skipping OrderDetail {} - currently IN_TROUBLES, waiting for issue resolution", 
                    detail.getId());
                continue;
            }
            
            // Validate status transition for eligible OrderDetails
            if (!isValidStatusTransition(currentStatus, newStatus)) {
                throw new BadRequestException(
                    String.format("Invalid status transition from %s to %s for OrderDetail %s", 
                        currentStatus, newStatus, detail.getId()),
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
                );
            }
            
            eligibleOrderDetails.add(detail);
        }
        
        // Log summary
        if (!skippedOrderDetails.isEmpty()) {
            log.info("📦 Vehicle Assignment {}: {} OrderDetails updated to {}, {} OrderDetails skipped (IN_TROUBLES)",
                vehicleAssignmentId, eligibleOrderDetails.size(), newStatus, skippedOrderDetails.size());
        }
        
        // Update status only for eligible OrderDetails (not IN_TROUBLES)
        eligibleOrderDetails.forEach(detail -> {
            String oldStatus = detail.getStatus();
            detail.setStatus(newStatus.name());
            log.debug("OrderDetail {} status: {} → {}", detail.getId(), oldStatus, newStatus);
        });
        
        // Save only the updated OrderDetails
        if (!eligibleOrderDetails.isEmpty()) {
            orderDetailEntityService.saveAllOrderDetailEntities(eligibleOrderDetails);
        }
        
        // 🔧 NEW: Send per-trip notifications BEFORE order-level aggregation
        // This ensures each trip gets its own notification with correct packages
        try {
            sendTripStatusNotifications(vehicleAssignmentId, newStatus, eligibleOrderDetails);
        } catch (Exception e) {
            log.error("❌ Failed to send trip status notifications: {}", e.getMessage(), e);
            // Don't fail the main operation
        }
        
        // Auto-update Order Status based on all OrderDetails
        UUID orderId = orderDetails.get(0).getOrderEntity().getId();
        updateOrderStatusBasedOnDetails(orderId);

    }
    
    @Override
    @Transactional
    public void updateOrderDetailStatus(UUID orderDetailId, OrderDetailStatusEnum newStatus) {

        OrderDetailEntity detail = orderDetailEntityService.findEntityById(orderDetailId)
            .orElseThrow(() -> new NotFoundException(
                "OrderDetail not found with ID: " + orderDetailId,
                ErrorEnum.NOT_FOUND.getErrorCode()
            ));
        
        OrderDetailStatusEnum currentStatus = detail.getStatus() != null
            ? OrderDetailStatusEnum.valueOf(detail.getStatus())
            : OrderDetailStatusEnum.PENDING;
        
        if (!isValidStatusTransition(currentStatus, newStatus)) {
            throw new BadRequestException(
                String.format("Invalid status transition from %s to %s", currentStatus, newStatus),
                ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }
        
        detail.setStatus(newStatus.name());
        orderDetailEntityService.save(detail);
        
        // Auto-update Order Status
        updateOrderStatusBasedOnDetails(detail.getOrderEntity().getId());

    }
    
    @Override
    public List<OrderDetailEntity> getOrderDetailsByAssignment(UUID vehicleAssignmentId) {
        return orderDetailEntityService.findByVehicleAssignmentId(vehicleAssignmentId);
    }
    
    @Override
    public boolean isValidStatusTransition(OrderDetailStatusEnum currentStatus, OrderDetailStatusEnum newStatus) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }
        
        // Allow same status (idempotent updates)
        if (currentStatus == newStatus) {
            return true;
        }
        
        // Define valid transitions based on actual driver flow
        return switch (currentStatus) {
            case PENDING -> newStatus == OrderDetailStatusEnum.ON_PLANNING;
            case ON_PLANNING ->
                newStatus == OrderDetailStatusEnum.ASSIGNED_TO_DRIVER;

            case ASSIGNED_TO_DRIVER ->
                newStatus == OrderDetailStatusEnum.PICKING_UP;

            case PICKING_UP ->
                newStatus == OrderDetailStatusEnum.ON_DELIVERED  // After seal confirmation
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES;

            case ON_DELIVERED ->
                newStatus == OrderDetailStatusEnum.ONGOING_DELIVERED  // Auto-triggered by proximity
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES;

            case ONGOING_DELIVERED ->
                newStatus == OrderDetailStatusEnum.DELIVERED
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES
                || newStatus == OrderDetailStatusEnum.RETURNING;

            case IN_TROUBLES ->
                newStatus == OrderDetailStatusEnum.PICKING_UP
                ||  newStatus == OrderDetailStatusEnum.ON_DELIVERED
                || newStatus == OrderDetailStatusEnum.ONGOING_DELIVERED
                || newStatus == OrderDetailStatusEnum.RETURNING
                || newStatus == OrderDetailStatusEnum.COMPENSATION;  // Can go to compensation if issue requires it

            case RETURNING ->
                newStatus == OrderDetailStatusEnum.RETURNED
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES;
                
            // Terminal states - no further transitions allowed
            case DELIVERED, RETURNED, COMPENSATION, CANCELLED -> false;
        };
    }
    
    /**
     * Public method to manually trigger Order status update
     * Used by external services when they directly update OrderDetail status
     */
    @Override
    @Transactional
    public void triggerOrderStatusUpdate(UUID orderId) {
        
        updateOrderStatusBasedOnDetails(orderId);
    }
    
    /**
     * 🔧 NEW: Send notifications for a specific trip (vehicleAssignment)
     * This ensures each trip gets its own notification with the correct packages
     * 
     * @param vehicleAssignmentId The vehicle assignment (trip) ID
     * @param newStatus The new status just set for this trip's packages
     * @param updatedDetails The OrderDetails that were just updated
     */
    private void sendTripStatusNotifications(
        UUID vehicleAssignmentId, 
        OrderDetailStatusEnum newStatus, 
        List<OrderDetailEntity> updatedDetails
    ) {
        if (updatedDetails.isEmpty()) {
            return;
        }
        
        // Get vehicle assignment for trip info
        var vehicleAssignment = vehicleAssignmentEntityService.findById(vehicleAssignmentId)
            .orElse(null);
        if (vehicleAssignment == null) {
            log.warn("⚠️ VehicleAssignment not found for ID: {}", vehicleAssignmentId);
            return;
        }
        
        // Get order from first detail
        var order = updatedDetails.get(0).getOrderEntity();
        if (order == null || order.getSender() == null || order.getSender().getUser() == null) {
            log.warn("⚠️ Order or customer not found for trip notifications");
            return;
        }
        
        // Get trip context (vehicle, drivers, tracking code)
        String vehicleAssignmentTrackingCode = vehicleAssignment.getTrackingCode();
        String vehiclePlate = vehicleAssignment.getVehicleEntity() != null 
            ? vehicleAssignment.getVehicleEntity().getLicensePlateNumber() : "";
        String vehicleTypeDescription = "N/A";
        if (vehicleAssignment.getVehicleEntity() != null && 
            vehicleAssignment.getVehicleEntity().getVehicleTypeEntity() != null) {
            vehicleTypeDescription = vehicleAssignment.getVehicleEntity().getVehicleTypeEntity().getDescription() != null 
                ? vehicleAssignment.getVehicleEntity().getVehicleTypeEntity().getDescription() 
                : vehicleAssignment.getVehicleEntity().getVehicleTypeEntity().getVehicleTypeName();
        }
        
        String driver1Name = vehicleAssignment.getDriver1() != null && vehicleAssignment.getDriver1().getUser() != null 
            ? vehicleAssignment.getDriver1().getUser().getFullName() : "Tài xế";
        String driver1Phone = vehicleAssignment.getDriver1() != null && vehicleAssignment.getDriver1().getUser() != null 
            ? vehicleAssignment.getDriver1().getUser().getPhoneNumber() : "";
        
        // Get category description
        String categoryDescription = "Hàng hóa";
        if (order.getCategory() != null && order.getCategory().getDescription() != null) {
            categoryDescription = order.getCategory().getDescription();
        } else if (order.getCategory() != null && order.getCategory().getCategoryName() != null) {
            categoryDescription = order.getCategory().getCategoryName().name();
        }
        
        // Filter packages by the new status for this notification
        // This ensures we only show packages that match the notification event
        List<OrderDetailEntity> packagesForNotification = updatedDetails.stream()
            .filter(od -> newStatus.name().equals(od.getStatus()))
            .toList();
        
        if (packagesForNotification.isEmpty()) {
            log.warn("⚠️ No packages with status {} found for trip notification", newStatus);
            return;
        }
        
        // Send notification based on status
        try {
            switch (newStatus) {
                case PICKING_UP -> {
                    // Customer notification: Driver started picking up (Email: YES)
                    notificationService.createNotification(NotificationBuilder.buildPickingUpStarted(
                        order.getSender().getUser().getId(),
                        order.getOrderCode(),
                        driver1Name,
                        driver1Phone,
                        vehiclePlate,
                        packagesForNotification,
                        categoryDescription,
                        vehicleTypeDescription,
                        vehicleAssignmentTrackingCode,
                        order.getId(),
                        vehicleAssignment.getId()
                    ));
                    log.info("✅ Sent PICKING_UP notification for trip {} with {} packages", 
                        vehicleAssignmentTrackingCode, packagesForNotification.size());
                }
                
                case ON_DELIVERED -> {
                    // Customer notification: Delivery started (packages sealed and on the way)
                    String deliveryLocation = order.getDeliveryAddress() != null 
                        ? order.getDeliveryAddress().getStreet() : "điểm giao hàng";
                    
                    notificationService.createNotification(NotificationBuilder.buildDeliveryStarted(
                        order.getSender().getUser().getId(),
                        order.getOrderCode(),
                        driver1Name,
                        vehiclePlate,
                        packagesForNotification,
                        categoryDescription,
                        deliveryLocation,
                        vehicleTypeDescription,
                        vehicleAssignmentTrackingCode,
                        order.getId(),
                        vehicleAssignment.getId()
                    ));
                    log.info("✅ Sent ON_DELIVERED notification for trip {} with {} packages", 
                        vehicleAssignmentTrackingCode, packagesForNotification.size());
                }
                
                case ONGOING_DELIVERED -> {
                    // Customer notification: Near delivery point
                    String deliveryLocation = order.getDeliveryAddress() != null 
                        ? order.getDeliveryAddress().getStreet() : "điểm giao hàng";
                    
                    notificationService.createNotification(NotificationBuilder.buildDeliveryInProgress(
                        order.getSender().getUser().getId(),
                        order.getOrderCode(),
                        driver1Name,
                        driver1Phone,
                        packagesForNotification,
                        categoryDescription,
                        deliveryLocation,
                        vehicleTypeDescription,
                        vehicleAssignmentTrackingCode,
                        order.getId(),
                        vehicleAssignment.getId()
                    ));
                    log.info("✅ Sent ONGOING_DELIVERED notification for trip {} with {} packages", 
                        vehicleAssignmentTrackingCode, packagesForNotification.size());
                }
                
                case DELIVERED -> {
                    // Customer notification: Trip packages delivered successfully
                    String deliveryLocation = order.getDeliveryAddress() != null 
                        ? order.getDeliveryAddress().getStreet() : "điểm giao hàng";
                    String receiverName = order.getReceiverName() != null 
                        ? order.getReceiverName() : "người nhận";
                    
                    // Get all order details to check if this is partial or full delivery
                    var allOrderDetails = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(order.getId());
                    int totalPackageCount = allOrderDetails.size();
                    boolean isFullDelivery = allOrderDetails.stream()
                        .allMatch(od -> "DELIVERED".equals(od.getStatus()));
                    
                    notificationService.createNotification(NotificationBuilder.buildDeliveryCompleted(
                        order.getSender().getUser().getId(),
                        order.getOrderCode(),
                        packagesForNotification.size(), // delivered count for THIS trip
                        totalPackageCount, // total packages in order
                        deliveryLocation,
                        receiverName,
                        packagesForNotification, // only packages from THIS trip
                        order.getId(),
                        packagesForNotification.stream().map(OrderDetailEntity::getId).toList(),
                        vehicleAssignment.getId(),
                        isFullDelivery
                    ));
                    
                    // Staff notification: Delivery completed (NO EMAIL)
                    CreateNotificationRequest staffDeliveryTemplate = NotificationBuilder.buildStaffDeliveryCompleted(
                        null, // Will be set for each staff user in sendStaffNotification
                        order.getOrderCode(),
                        order.getSender().getUser().getFullName(),
                        packagesForNotification.size(), // delivered count for THIS trip
                        totalPackageCount, // total packages in order
                        packagesForNotification, // only packages from THIS trip
                        order.getId(),
                        packagesForNotification.stream().map(OrderDetailEntity::getId).toList()
                    );
                    sendStaffNotification(staffDeliveryTemplate);
                    
                    log.info("✅ Sent DELIVERED notification for trip {} with {} packages (full={}) ", 
                        vehicleAssignmentTrackingCode, packagesForNotification.size(), isFullDelivery);
                }
                
                // Note: RETURNING and RETURNED status are typically issue-driven (return shipping)
                // and notifications are sent by IssueServiceImpl, not per-trip status changes
                
                default -> {
                    // No notification for other statuses at trip level
                    log.debug("No trip-level notification for status: {}", newStatus);
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to send trip notification for status {}: {}", newStatus, e.getMessage(), e);
        }
    }
    
    /**
     * Auto-update Order status based on all OrderDetails of that order
     * This method fetches all OrderDetails, determines the target Order status
     * using multi-trip logic, and updates the Order if status changed
     * 
     * Priority Logic:
     *   + COMPENSATION has highest priority (ANY trip)
     *   + IN_TROUBLES comes next (ANY trip has problem)
     *   + CANCELLED requires all trips cancelled (ALL) or ignore if partial
     *   + RETURNING/RETURNED requires all trips in return flow (ALL)
     *   + DELIVERED - ALL trips delivered → Order DELIVERED (wait for odometer upload)
     *   + Problem states (IN_TROUBLES, COMPENSATION) have highest priority
     *   + REJECTED/RETURNED don't block other trips from completing
     *   + SUCCESSFUL - Only set by VehicleFuelConsumptionServiceImpl after odometer end upload
     */
    private void updateOrderStatusBasedOnDetails(UUID orderId) {

        List<OrderDetailEntity> allDetails = orderDetailEntityService
            .findOrderDetailEntitiesByOrderEntityId(orderId);
        
        if (allDetails.isEmpty()) {
            log.warn("No OrderDetails found for Order {}, skipping auto-update", orderId);
            return;
        }
        
        // Get all statuses
        List<OrderDetailStatusEnum> statuses = allDetails.stream()
            .map(d -> d.getStatus() != null 
                ? OrderDetailStatusEnum.valueOf(d.getStatus())
                : OrderDetailStatusEnum.PENDING)
            .toList();
        
        // Determine Order Status using multi-trip logic
        OrderDetailStatusEnum targetStatus = determineOrderStatusForMultiTrip(statuses);
        OrderStatusEnum newOrderStatus = mapDetailStatusToOrderStatus(targetStatus);
        
        // Update Order if status changed
        OrderEntity order = orderEntityService.findEntityById(orderId)
            .orElseThrow(() -> new NotFoundException(
                "Order not found with ID: " + orderId,
                ErrorEnum.NOT_FOUND.getErrorCode()
            ));
        
        String currentOrderStatus = order.getStatus();
        if (!newOrderStatus.name().equals(currentOrderStatus)) {
            OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(currentOrderStatus);
            order.setStatus(newOrderStatus.name());
            orderEntityService.save(order);
            
            // Special case for ON_PLANNING: Update all order details to ON_PLANNING as well
            // This ensures consistency when order status is set to ON_PLANNING
            if (newOrderStatus == OrderStatusEnum.ON_PLANNING) {
                log.info("🔧 Updating all order details to ON_PLANNING for order {}", orderId);
                allDetails.forEach(detail -> detail.setStatus(OrderDetailStatusEnum.ON_PLANNING.name()));
                orderDetailEntityService.saveAllOrderDetailEntities(allDetails);
                log.info("✅ Updated {} order details to ON_PLANNING for order {}", allDetails.size(), orderId);
            }
            
            // Send WebSocket notification for status change (event-based, sent AFTER transaction commits)
            // This ensures staff/customer receives the updated status, not the stale one
            orderStatusWebSocketService.sendOrderStatusChange(
                orderId,
                order.getOrderCode(),
                previousStatus,
                newOrderStatus
            );
            
            // 📧 Send persistent notifications for order status changes
            try {
                // Get order details count for multi-trip awareness
                int totalPackageCount = allDetails.size();
                String customerName = order.getSender() != null && order.getSender().getUser() != null 
                    ? order.getSender().getUser().getFullName() : "Khách hàng";
                
                // Get vehicle assignment from first order detail (for driver info)
                var firstDetail = allDetails.get(0);
                var vehicleAssignment = firstDetail.getVehicleAssignmentEntity();
                
                // 🔧 MULTI-TRIP FIX: Get only order details for this specific vehicle assignment
                // This ensures notifications only show packages for the current trip, not all packages in the order
                final var currentAssignmentId = vehicleAssignment != null ? vehicleAssignment.getId() : null;
                List<OrderDetailEntity> tripSpecificDetails = currentAssignmentId != null
                    ? allDetails.stream()
                        .filter(od -> od.getVehicleAssignmentEntity() != null 
                            && od.getVehicleAssignmentEntity().getId().equals(currentAssignmentId))
                        .toList()
                    : allDetails;
                
                // Get tracking code for vehicle assignment (not UUID)
                String vehicleAssignmentTrackingCode = vehicleAssignment != null 
                    ? vehicleAssignment.getTrackingCode() : null;
                
                switch (newOrderStatus) {
                    case ON_PLANNING:
                        // Staff notification: Deposit received, ready for planning
                        sendStaffNotification(NotificationBuilder.buildStaffDepositReceived(
                            null, // Will be set for each staff
                            order.getOrderCode(),
                            0, // Deposit amount - not available here
                            customerName,
                            totalPackageCount,
                            order.getId()
                        ));
                        break;
                        
                    // ⚠️ COMMENTED OUT: These notifications are now handled per-trip in sendTripStatusNotifications()
                    // This prevents duplicate notifications and ensures correct per-assignment package filtering
                    /*
                    case PICKING_UP:
                        // NOW HANDLED IN sendTripStatusNotifications() - per-trip with correct packages
                        break;
                        
                    case ON_DELIVERED:
                        // NOW HANDLED IN sendTripStatusNotifications() - per-trip with correct packages
                        break;
                        
                        // NOW HANDLED IN sendTripStatusNotifications() - per-trip with correct packages
                        break;
                    */
                    
                    case DELIVERED:
                        // Customer notification: All packages delivered
                        if (order.getSender() != null && order.getSender().getUser() != null) {
                            String deliveryLocation = order.getDeliveryAddress() != null 
                                ? order.getDeliveryAddress().getStreet() : "điểm giao hàng";
                            String receiverName = order.getReceiverName() != null 
                                ? order.getReceiverName() : "người nhận";
                            
                            notificationService.createNotification(NotificationBuilder.buildDeliveryCompleted(
                                order.getSender().getUser().getId(),
                                order.getOrderCode(),
                                totalPackageCount,
                                totalPackageCount,
                                deliveryLocation,
                                receiverName,
                                allDetails,
                                order.getId(),
                                allDetails.stream().map(OrderDetailEntity::getId).toList(),
                                vehicleAssignment != null ? vehicleAssignment.getId() : null,
                                true // All packages delivered
                            ));
                        }
                        break;
                        
                    case CANCELLED:
                        // Customer notification: Order cancelled
                        if (order.getSender() != null && order.getSender().getUser() != null) {
                            notificationService.createNotification(NotificationBuilder.buildOrderCancelledMultiTrip(
                                order.getSender().getUser().getId(),
                                order.getOrderCode(),
                                totalPackageCount,
                                totalPackageCount,
                                "Quá hạn thanh toán", // Default reason
                                order.getId(),
                                allDetails.stream().map(OrderDetailEntity::getId).toList(),
                                true // All packages cancelled
                            ));
                        }
                        // Staff notification: Order cancelled
                        sendStaffNotification(NotificationBuilder.buildStaffOrderCancelled(
                            null,
                            order.getOrderCode(),
                            totalPackageCount,
                            totalPackageCount,
                            "Quá hạn thanh toán",
                            customerName,
                            order.getId()
                        ));
                        break;
                        
                    case RETURNING:
                        // Customer notification: Returning packages
                        // This is handled in IssueServiceImpl when return shipping is required
                        break;
                        
                    case RETURNED:
                        // Customer notification: All packages returned
                        if (order.getSender() != null && order.getSender().getUser() != null) {
                            String pickupLocation = order.getPickupAddress() != null 
                                ? order.getPickupAddress().getStreet() : "điểm lấy hàng";
                            notificationService.createNotification(NotificationBuilder.buildReturnCompleted(
                                order.getSender().getUser().getId(),
                                order.getOrderCode(),
                                totalPackageCount,
                                totalPackageCount,
                                pickupLocation,
                                allDetails,
                                order.getId(),
                                allDetails.stream().map(OrderDetailEntity::getId).toList(),
                                vehicleAssignment != null ? vehicleAssignment.getId() : null,
                                true // All packages returned
                            ));
                        }
                        break;
                        
                    case COMPENSATION:
                        // Customer notification: Compensation processed
                        // This is handled in IssueServiceImpl when compensation is calculated
                        break;
                        
                    default:
                        // Other status changes may not need notifications
                        break;
                }
            } catch (Exception e) {
                log.error("❌ Failed to send order status notifications: {}", e.getMessage());
                // Don't throw - Notification failure shouldn't break business logic
            }
        } else {
            
        }
    }
    
    /**
     * ═══════════════════════════════════════════════════════════════════════════
     * MULTI-TRIP ORDER STATUS AGGREGATION
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Strategy: Order Status reflects OVERALL progress across ALL OrderDetails
     * with proper priority handling for problem states and multi-trip scenarios.
     * 
     * ╔═══════════════════════════════════════════════════════════════════════════╗
     * ║  PRIORITY MATRIX (Highest to Lowest)                                      ║
     * ╠═══════════════════════════════════════════════════════════════════════════╣
     * ║  Priority │ Status           │ Condition       │ Reason                   ║
     * ╠═══════════════════════════════════════════════════════════════════════════╣
     * ║  1 (HIGH) │ COMPENSATION     │ ANY             │ Bồi thường đã xử lý      ║
     * ║  2        │ IN_TROUBLES      │ ANY             │ Có issue cần attention   ║
     * ║  3        │ CANCELLED        │ ALL             │ Toàn bộ packages đã hủy  ║
     * ║  4        │ RETURNING        │ ALL active      │ Đang trả toàn bộ hàng    ║
     * ║  5        │ RETURNED         │ ALL active      │ Đã trả toàn bộ hàng      ║
     * ║  6        │ DELIVERED        │ ALL completable │ Tất cả đã giao xong      ║
     * ║  7        │ ONGOING_DELIVERED│ SOME delivered  │ Đang giao, chưa xong hết ║
     * ║  8 (LOW)  │ Progressive MAX  │ In-progress     │ Theo tiến độ xa nhất     ║
     * ╚═══════════════════════════════════════════════════════════════════════════╝
     * 
     * ═══════════════════════════════════════════════════════════════════════════
     * KEY BUSINESS RULES
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * 1. COMPENSATION (ANY): If ANY package has compensation → Order = COMPENSATION
     *    - Highest priority as it indicates financial resolution
     *    - Example: 1 COMPENSATION + 2 DELIVERED → Order COMPENSATION
     * 
     * 2. IN_TROUBLES (ANY): If ANY package has issue → Order = IN_TROUBLES
     *    - Signals staff attention needed, blocks order completion
     *    - Live tracking still works for other trips
     *    - Example: 1 IN_TROUBLES + 2 DELIVERED → Order IN_TROUBLES
     * 
     * 3. CANCELLED (ALL): Only if ALL packages cancelled → Order = CANCELLED
     *    - Partial cancellation ignored, continue with active packages
     *    - Example: 2 CANCELLED + 1 DELIVERED → Order DELIVERED
     * 
     * 4. RETURNED/RETURNING (ALL active): Complete return flow
     *    - ALL RETURNING → Order RETURNING
     *    - ALL RETURNED → Order RETURNED  
     *    - SOME RETURNED + SOME DELIVERED → Order DELIVERED (both complete states)
     * 
     * 5. DELIVERED (ALL completable): All deliverable packages delivered
     *    - Must wait for ALL trips before Order = DELIVERED
     *    - Order → SUCCESSFUL only after odometer end upload (manual)
     * 
     * 6. ONGOING_DELIVERED: Partial delivery progress
     *    - SOME packages delivered but not all
     *    - Example: 1 DELIVERED + 1 PICKING_UP → Order ONGOING_DELIVERED
     * 
     * 7. Progressive MAX: Furthest progress for in-flight states
     *    - Example: 2 PICKING_UP + 1 ON_DELIVERED → Order ON_DELIVERED
     * 
     * ═══════════════════════════════════════════════════════════════════════════
     * MULTI-TRIP SCENARIOS
     * ═══════════════════════════════════════════════════════════════════════════
     * 
     * Scenario 1: Trip1=PICKING_UP, Trip2=DELIVERED
     *   → Order = ONGOING_DELIVERED (wait for Trip1)
     * 
     * Scenario 2: Trip1=PICKING_UP, Trip2=[IN_TROUBLES, RETURNED, DELIVERED]
     *   → Order = IN_TROUBLES (needs attention, live tracking still works)
     * 
     * Scenario 3: Trip1=DELIVERED, Trip2=[RETURNED, DELIVERED]
     *   → Order = DELIVERED (all packages in complete state)
     * 
     * Scenario 4: All trips DELIVERED
     *   → Order = DELIVERED (wait for odometer upload → SUCCESSFUL)
     */
    private OrderDetailStatusEnum determineOrderStatusForMultiTrip(List<OrderDetailStatusEnum> statuses) {
        if (statuses == null || statuses.isEmpty()) {
            log.warn("⚠️ Empty statuses list, returning PENDING");
            return OrderDetailStatusEnum.PENDING;
        }
        
        final int totalCount = statuses.size();
        
        // ═══════════════════════════════════════════════════════════════
        // Count all status types in a single pass for efficiency
        // ═══════════════════════════════════════════════════════════════
        long compensationCount = 0;
        long inTroublesCount = 0;
        long cancelledCount = 0;
        long returningCount = 0;
        long returnedCount = 0;
        long deliveredCount = 0;
        
        for (OrderDetailStatusEnum status : statuses) {
            switch (status) {
                case COMPENSATION -> compensationCount++;
                case IN_TROUBLES -> inTroublesCount++;
                case CANCELLED -> cancelledCount++;
                case RETURNING -> returningCount++;
                case RETURNED -> returnedCount++;
                case DELIVERED -> deliveredCount++;
                default -> {} // Progressive states handled later
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 1: COMPENSATION (Terminal - ANY)
        // ═══════════════════════════════════════════════════════════════
        // If ANY package has COMPENSATION → Order = COMPENSATION
        // Highest priority as it represents financial settlement
        if (compensationCount > 0) {
            log.debug("📊 Order status = COMPENSATION ({}/{} packages)", compensationCount, totalCount);
            return OrderDetailStatusEnum.COMPENSATION;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 2: IN_TROUBLES (Active Problem - ANY)
        // ═══════════════════════════════════════════════════════════════
        // If ANY package has IN_TROUBLES → Order = IN_TROUBLES
        // This signals staff attention is needed
        // Live tracking still works for other trips (order is still "active")
        if (inTroublesCount > 0) {
            log.debug("📊 Order status = IN_TROUBLES ({}/{} packages)", inTroublesCount, totalCount);
            return OrderDetailStatusEnum.IN_TROUBLES;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 3: CANCELLED (Terminal - ALL only)
        // ═══════════════════════════════════════════════════════════════
        // Only if ALL packages cancelled → Order = CANCELLED
        // Partial cancellation: ignore cancelled, continue with active packages
        if (cancelledCount == totalCount) {
            log.debug("📊 Order status = CANCELLED (all {} packages)", totalCount);
            return OrderDetailStatusEnum.CANCELLED;
        }
        
        // Calculate active package count (excluding cancelled)
        final long activeCount = totalCount - cancelledCount;
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 4: RETURNING (ALL active packages returning)
        // ═══════════════════════════════════════════════════════════════
        if (returningCount == activeCount && activeCount > 0) {
            log.debug("📊 Order status = RETURNING (all {} active packages)", activeCount);
            return OrderDetailStatusEnum.RETURNING;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 5: RETURNED (ALL active packages returned)
        // ═══════════════════════════════════════════════════════════════
        if (returnedCount == activeCount && activeCount > 0) {
            log.debug("📊 Order status = RETURNED (all {} active packages)", activeCount);
            return OrderDetailStatusEnum.RETURNED;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 6: DELIVERED (ALL completable packages delivered)
        // ═══════════════════════════════════════════════════════════════
        // Completable = not cancelled, not in return flow
        // If mix of RETURNED + DELIVERED → both are "complete" states
        // Order = DELIVERED means all packages reached a terminal success state
        long completedCount = deliveredCount + returnedCount;
        
        if (completedCount == activeCount && deliveredCount > 0) {
            log.debug("📊 Order status = DELIVERED ({} delivered + {} returned out of {} active)", 
                deliveredCount, returnedCount, activeCount);
            return OrderDetailStatusEnum.DELIVERED;
        }
        
        // Special case: ALL active are RETURNED (no DELIVERED) - handled in Priority 5
        // This handles: 1 CANCELLED + 2 RETURNED → Order RETURNED
        if (returnedCount == activeCount && activeCount > 0) {
            log.debug("📊 Order status = RETURNED (all {} active packages)", activeCount);
            return OrderDetailStatusEnum.RETURNED;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 7: ONGOING_DELIVERED (Partial delivery in progress)
        // ═══════════════════════════════════════════════════════════════
        // SOME packages delivered/returned but not all
        // Indicates delivery is in progress, waiting for other trips
        if (deliveredCount > 0 || returnedCount > 0) {
            log.debug("📊 Order status = ONGOING_DELIVERED ({} delivered + {} returned, {} still in progress)", 
                deliveredCount, returnedCount, activeCount - completedCount);
            return OrderDetailStatusEnum.ONGOING_DELIVERED;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 8: PROGRESSIVE STATES (Use MAX ordinal)
        // ═══════════════════════════════════════════════════════════════
        // For in-progress states, show furthest progress across all trips
        // Progressive states: PENDING(0), ON_PLANNING(1), ASSIGNED_TO_DRIVER(2), 
        //                     PICKING_UP(3), ON_DELIVERED(4), ONGOING_DELIVERED(5)
        OrderDetailStatusEnum maxProgressiveStatus = statuses.stream()
            .filter(s -> s != OrderDetailStatusEnum.CANCELLED) // Ignore cancelled packages
            .filter(s -> s.ordinal() <= OrderDetailStatusEnum.ONGOING_DELIVERED.ordinal()) // Only progressive states
            .max(Comparator.comparingInt(Enum::ordinal))
            .orElse(OrderDetailStatusEnum.PENDING);
        
        log.debug("📊 Order status = {} (progressive max)", maxProgressiveStatus);
        return maxProgressiveStatus;
    }
    
    /**
     * Map OrderDetail Status to corresponding Order Status
     * This mapping ensures Order status reflects the overall delivery progress
     */
    private OrderStatusEnum mapDetailStatusToOrderStatus(OrderDetailStatusEnum detailStatus) {
        return switch (detailStatus) {
            case PENDING -> OrderStatusEnum.PENDING;
            case ON_PLANNING -> OrderStatusEnum.ON_PLANNING;
            case ASSIGNED_TO_DRIVER -> OrderStatusEnum.ASSIGNED_TO_DRIVER;
            case PICKING_UP -> OrderStatusEnum.PICKING_UP;
            case ON_DELIVERED -> OrderStatusEnum.ON_DELIVERED;
            case ONGOING_DELIVERED -> OrderStatusEnum.ONGOING_DELIVERED;
            case DELIVERED -> OrderStatusEnum.DELIVERED;  // Wait for odometer end upload to set SUCCESSFUL
            case IN_TROUBLES -> OrderStatusEnum.IN_TROUBLES;
            case COMPENSATION -> OrderStatusEnum.COMPENSATION;
            case RETURNING -> OrderStatusEnum.RETURNING;
            case RETURNED -> OrderStatusEnum.RETURNED;
            case CANCELLED -> OrderStatusEnum.CANCELLED;
        };
    }
    
    /**
     * Helper method to send staff notifications
     * Creates a notification for each staff user from a template
     */
    private void sendStaffNotification(CreateNotificationRequest template) {
        try {
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            if (!staffUsers.isEmpty()) {
                for (var staff : staffUsers) {
                    // Create a new notification request for each staff user
                    CreateNotificationRequest staffNotification = CreateNotificationRequest.builder()
                        .userId(staff.getId())
                        .recipientRole("STAFF")
                        .title(template.getTitle())
                        .description(template.getDescription())
                        .notificationType(template.getNotificationType())
                        .relatedOrderId(template.getRelatedOrderId())
                        .relatedIssueId(template.getRelatedIssueId())
                        .relatedVehicleAssignmentId(template.getRelatedVehicleAssignmentId())
                        .relatedContractId(template.getRelatedContractId())
                        .metadata(template.getMetadata())
                        .build();
                    
                    notificationService.createNotification(staffNotification);
                }
                log.info("📧 Staff notifications sent: {} staff users notified, type: {}", 
                    staffUsers.size(), template.getNotificationType());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send staff notification: {}", e.getMessage());
            // Don't throw - Notification failure shouldn't break business logic
        }
    }
}
