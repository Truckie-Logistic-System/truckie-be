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
    public void triggerOrderStatusUpdate(UUID orderId) {
        
        updateOrderStatusBasedOnDetails(orderId);
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

            // Send WebSocket notification for status change
            try {
                orderStatusWebSocketService.sendOrderStatusChange(
                    orderId,
                    order.getOrderCode(),
                    previousStatus,
                    newOrderStatus
                );
                
            } catch (Exception e) {
                log.error("❌ Failed to send WebSocket notification for order status change: {}", e.getMessage());
                // Don't throw - WebSocket failure shouldn't break business logic
            }
            
            // 📧 Send persistent notifications for order status changes
            try {
                // Get order details count for multi-trip awareness
                int totalPackageCount = allDetails.size();
                String customerName = order.getSender() != null && order.getSender().getUser() != null 
                    ? order.getSender().getUser().getFullName() : "Khách hàng";
                
                // Get vehicle assignment from first order detail (for driver info)
                var firstDetail = allDetails.get(0);
                var vehicleAssignment = firstDetail.getVehicleAssignmentEntity();
                
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
                        
                    case PICKING_UP:
                        // Customer notification: Driver started picking up (Email: YES - for live tracking)
                        if (order.getSender() != null && order.getSender().getUser() != null && vehicleAssignment != null) {
                            String driverName = vehicleAssignment.getDriver1() != null && vehicleAssignment.getDriver1().getUser() != null 
                                ? vehicleAssignment.getDriver1().getUser().getFullName() : "Tài xế";
                            String driverPhone = vehicleAssignment.getDriver1() != null && vehicleAssignment.getDriver1().getUser() != null 
                                ? vehicleAssignment.getDriver1().getUser().getPhoneNumber() : "";
                            String vehiclePlate = vehicleAssignment.getVehicleEntity() != null 
                                ? vehicleAssignment.getVehicleEntity().getLicensePlateNumber() : "";
                                
                            notificationService.createNotification(NotificationBuilder.buildPickingUpStarted(
                                order.getSender().getUser().getId(),
                                order.getOrderCode(),
                                driverName,
                                driverPhone,
                                vehiclePlate,
                                totalPackageCount,
                                order.getId(),
                                vehicleAssignment.getId()
                            ));
                        }
                        break;
                        
                    case ON_DELIVERED:
                        // Customer notification: Delivery started
                        if (order.getSender() != null && order.getSender().getUser() != null && vehicleAssignment != null) {
                            String driverName = vehicleAssignment.getDriver1() != null && vehicleAssignment.getDriver1().getUser() != null 
                                ? vehicleAssignment.getDriver1().getUser().getFullName() : "Tài xế";
                            String vehiclePlate = vehicleAssignment.getVehicleEntity() != null 
                                ? vehicleAssignment.getVehicleEntity().getLicensePlateNumber() : "";
                            String deliveryLocation = order.getDeliveryAddress() != null 
                                ? order.getDeliveryAddress().getStreet() : "điểm giao hàng";
                                
                            notificationService.createNotification(NotificationBuilder.buildDeliveryStarted(
                                order.getSender().getUser().getId(),
                                order.getOrderCode(),
                                driverName,
                                vehiclePlate,
                                totalPackageCount,
                                deliveryLocation,
                                order.getId(),
                                vehicleAssignment.getId()
                            ));
                        }
                        break;
                        
                    case ONGOING_DELIVERED:
                        // Customer notification: Near delivery point
                        if (order.getSender() != null && order.getSender().getUser() != null && vehicleAssignment != null) {
                            String driverName = vehicleAssignment.getDriver1() != null && vehicleAssignment.getDriver1().getUser() != null 
                                ? vehicleAssignment.getDriver1().getUser().getFullName() : "Tài xế";
                            String driverPhone = vehicleAssignment.getDriver1() != null && vehicleAssignment.getDriver1().getUser() != null 
                                ? vehicleAssignment.getDriver1().getUser().getPhoneNumber() : "";
                            String deliveryLocation = order.getDeliveryAddress() != null 
                                ? order.getDeliveryAddress().getStreet() : "điểm giao hàng";
                                
                            notificationService.createNotification(NotificationBuilder.buildDeliveryInProgress(
                                order.getSender().getUser().getId(),
                                order.getOrderCode(),
                                driverName,
                                driverPhone,
                                totalPackageCount,
                                deliveryLocation,
                                order.getId(),
                                vehicleAssignment.getId()
                            ));
                        }
                        break;
                        
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
     * Determine Order Status for Multi-Trip Orders
     * Strategy: Order Status reflects OVERALL/HIGH-LEVEL progress across ALL OrderDetails
     * 
     * ═══════════════════════════════════════════════════════════════
     * COMPREHENSIVE PRIORITY LOGIC (Highest to Lowest)
     * ═══════════════════════════════════════════════════════════════
     * 
     * Priority 1: COMPENSATION (Terminal - ANY)
     *   - If ANY OrderDetail = COMPENSATION → Order = COMPENSATION
     *   - Reason: Highest severity, terminal state
     *   - Example: 1 COMPENSATION + 2 DELIVERED → Order COMPENSATION
     * 
     * Priority 2: IN_TROUBLES (Active Problem - ANY)
     *   - If ANY OrderDetail = IN_TROUBLES → Order = IN_TROUBLES
     *   - Reason: Active problem requiring attention
     *   - Example: 1 IN_TROUBLES + 2 SUCCESSFUL → Order IN_TROUBLES
     * 
     * Priority 3: CANCELLED (Terminal - ALL or PARTIAL)
     *   - If ALL OrderDetails = CANCELLED → Order = CANCELLED
     *   - If SOME CANCELLED + SOME others → Ignore CANCELLED, use max of non-cancelled
     *   - Reason: If all cancelled, order is cancelled; if partial, continue with active packages
     *   - Example 1: ALL CANCELLED → Order CANCELLED
     *   - Example 2: 2 CANCELLED + 3 DELIVERED → Order DELIVERED (ignore cancelled)
     * 
     * Priority 4: RETURNING/RETURNED (Return Flow - ALL)
     *   - If ALL OrderDetails = RETURNING → Order = RETURNING
     *   - If ALL OrderDetails = RETURNED → Order = RETURNED
     *   - If SOME RETURNED + SOME others → Use max of non-returned
     *   - Reason: Return is complete flow, not partial state
     *   - Example: 2 RETURNED + 1 DELIVERED → Order DELIVERED
     * 
     * Priority 5: DELIVERED (All Delivered → Order DELIVERED)
     *   - If ALL OrderDetails = DELIVERED → Order = DELIVERED
     *   - If SOME DELIVERED + SOME ONGOING_DELIVERED → Order = ONGOING_DELIVERED (wait)
     *   - Reason: Must wait for ALL trips to complete delivery before Order = DELIVERED
     *   - Example 1: ALL DELIVERED → Order DELIVERED
     *   - Example 2: 1 DELIVERED + 2 ONGOING_DELIVERED → Order ONGOING_DELIVERED (wait)
     *   - Order → SUCCESSFUL only after driver uploads odometer end (manual update)
     * 
     * Priority 7: Progressive States (Use MAX)
     *   - States: PENDING, ON_PLANNING, ASSIGNED_TO_DRIVER, PICKING_UP, ON_DELIVERED, ONGOING_DELIVERED
     *   - Use furthest progress (MAX ordinal)
     *   - Reason: Show real-time progress as it happens
     *   - Example: 2 PICKING_UP + 1 ON_DELIVERED → Order ON_DELIVERED
     * 
     * ═══════════════════════════════════════════════════════════════
     * EDGE CASES HANDLED
     * ═══════════════════════════════════════════════════════════════
     * 
     * Case 1: All packages same status
     *   → Order = that status
     * 
     * Case 2: Mix of terminal and active states
     *   → Terminal states (COMPENSATION, CANCELLED) take priority
     * 
     * Case 3: All packages delivered
     *   → ALL DELIVERED → Order DELIVERED (wait for odometer)
     *   → SOME DELIVERED → Order ONGOING_DELIVERED (wait for remaining trips)
     *   → Order SUCCESSFUL only after odometer end upload
     * 
     * Case 4: Mix of problem states
     *   → ANY IN_TROUBLES → Order IN_TROUBLES
     * 
     * Case 5: Partial cancellation (rejection timeout)
     *   → Ignore CANCELLED, use max of active packages
     * 
     * Case 6: Partial return
     *   → If not all returned, use max of non-returned
     * 
     * Case 7: Empty order
     *   → Return PENDING (should never happen)
     */
    private OrderDetailStatusEnum determineOrderStatusForMultiTrip(List<OrderDetailStatusEnum> statuses) {
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 1: COMPENSATION (Terminal state - ANY)
        // ═══════════════════════════════════════════════════════════════
        // If ANY OrderDetail has COMPENSATION → Order = COMPENSATION
        // This is highest priority as it represents critical failure
        if (statuses.stream().anyMatch(s -> s == OrderDetailStatusEnum.COMPENSATION)) {
            long compensationCount = statuses.stream().filter(s -> s == OrderDetailStatusEnum.COMPENSATION).count();
            
            return OrderDetailStatusEnum.COMPENSATION;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 2: IN_TROUBLES (Active problem - ANY)
        // ═══════════════════════════════════════════════════════════════
        // If ANY OrderDetail is IN_TROUBLES → Order = IN_TROUBLES
        // This takes priority over success states because it requires immediate attention
        // Example: 1 IN_TROUBLES + 2 SUCCESSFUL → Order IN_TROUBLES
        boolean hasInTroubles = statuses.stream()
            .anyMatch(s -> s == OrderDetailStatusEnum.IN_TROUBLES);
        if (hasInTroubles) {
            long troubleCount = statuses.stream().filter(s -> s == OrderDetailStatusEnum.IN_TROUBLES).count();
            
            return OrderDetailStatusEnum.IN_TROUBLES;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 3: CANCELLED (Terminal - ALL or PARTIAL)
        // ═══════════════════════════════════════════════════════════════
        // Check if ANY OrderDetails are CANCELLED
        long cancelledCount = statuses.stream().filter(s -> s == OrderDetailStatusEnum.CANCELLED).count();
        
        if (cancelledCount > 0) {
            // Case A: ALL OrderDetails are CANCELLED → Order = CANCELLED
            if (cancelledCount == statuses.size()) {
                
                return OrderDetailStatusEnum.CANCELLED;
            }
            
            // Case B: SOME CANCELLED but not all → Ignore cancelled, use max of non-cancelled
            // This handles rejection timeout: some packages cancelled, others delivered successfully
            List<OrderDetailStatusEnum> nonCancelledStatuses = statuses.stream()
                .filter(s -> s != OrderDetailStatusEnum.CANCELLED)
                .toList();
            
            if (!nonCancelledStatuses.isEmpty()) {
                OrderDetailStatusEnum maxNonCancelled = nonCancelledStatuses.stream()
                    .max(Comparator.comparingInt(Enum::ordinal))
                    .orElse(OrderDetailStatusEnum.CANCELLED);
                
                // Continue to process non-cancelled statuses in next priorities
                // Replace statuses list with non-cancelled for further processing
                statuses = nonCancelledStatuses;
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 4: RETURNING/RETURNED (Return flow - ALL)
        // ═══════════════════════════════════════════════════════════════
        
        // Case A: ALL OrderDetails are RETURNING → Order = RETURNING
        long returningCount = statuses.stream().filter(s -> s == OrderDetailStatusEnum.RETURNING).count();
        if (returningCount == statuses.size()) {
            
            return OrderDetailStatusEnum.RETURNING;
        }
        
        // Case B: ALL OrderDetails are RETURNED → Order = RETURNED
        long returnedCount = statuses.stream().filter(s -> s == OrderDetailStatusEnum.RETURNED).count();
        if (returnedCount == statuses.size()) {
            
            return OrderDetailStatusEnum.RETURNED;
        }
        
        // Case C: SOME RETURNED but not all → Use max of non-returned
        // This handles partial return: some packages returned, others delivered
        if (returnedCount > 0) {
            List<OrderDetailStatusEnum> nonReturnedStatuses = statuses.stream()
                .filter(s -> s != OrderDetailStatusEnum.RETURNED)
                .toList();
            
            if (!nonReturnedStatuses.isEmpty()) {
                OrderDetailStatusEnum maxNonReturned = nonReturnedStatuses.stream()
                    .max(Comparator.comparingInt(Enum::ordinal))
                    .orElse(OrderDetailStatusEnum.RETURNED);
                
                return maxNonReturned;
            }
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 5: DELIVERED (All delivered → Order DELIVERED)
        // ═══════════════════════════════════════════════════════════════
        // Must wait for ALL OrderDetails to be DELIVERED before Order = DELIVERED
        // Rationale: All trips must complete delivery before order is delivered
        // Order → SUCCESSFUL only when driver uploads odometer end (manual update in VehicleFuelConsumptionServiceImpl)
        long deliveredCount = statuses.stream().filter(s -> s == OrderDetailStatusEnum.DELIVERED).count();
        
        // Case A: ALL OrderDetails are DELIVERED → Order = DELIVERED
        if (deliveredCount == statuses.size()) {
            return OrderDetailStatusEnum.DELIVERED;
        }
        
        // Case B: SOME DELIVERED but not all → Order = ONGOING_DELIVERED (wait for remaining trips)
        // This prevents premature "delivered" status when some packages still in transit
        if (deliveredCount > 0) {
            return OrderDetailStatusEnum.ONGOING_DELIVERED;
        }
        
        // ═══════════════════════════════════════════════════════════════
        // PRIORITY 7: PROGRESSIVE STATES (Use MAX)
        // ═══════════════════════════════════════════════════════════════
        // For progressive states (PENDING, ON_PLANNING, ASSIGNED_TO_DRIVER, 
        // PICKING_UP, ON_DELIVERED, ONGOING_DELIVERED), use the furthest progress
        // This allows real-time tracking as order progresses
        // Example: 2 PICKING_UP + 1 ON_DELIVERED → Order ON_DELIVERED
        OrderDetailStatusEnum maxStatus = statuses.stream()
            .filter(s -> s.ordinal() < OrderDetailStatusEnum.DELIVERED.ordinal())
            .max(Comparator.comparingInt(Enum::ordinal))
            .orElse(OrderDetailStatusEnum.PENDING);

        return maxStatus;
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
