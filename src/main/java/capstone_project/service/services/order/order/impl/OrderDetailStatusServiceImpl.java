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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    
    @Override
    @Transactional
    public void updateOrderDetailStatusByAssignment(UUID vehicleAssignmentId, OrderDetailStatusEnum newStatus) {
        log.info("Updating OrderDetail status to {} for vehicle assignment {}", newStatus, vehicleAssignmentId);
        
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
        
        // Validate status transitions for all order details
        for (OrderDetailEntity detail : orderDetails) {
            OrderDetailStatusEnum currentStatus = detail.getStatus() != null 
                ? OrderDetailStatusEnum.valueOf(detail.getStatus())
                : OrderDetailStatusEnum.PENDING;
            
            if (!isValidStatusTransition(currentStatus, newStatus)) {
                throw new BadRequestException(
                    String.format("Invalid status transition from %s to %s for OrderDetail %s", 
                        currentStatus, newStatus, detail.getId()),
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
                );
            }
        }
        
        // Update status for all OrderDetails in this assignment
        orderDetails.forEach(detail -> {
            String oldStatus = detail.getStatus();
            detail.setStatus(newStatus.name());
            log.info("Updated OrderDetail {} status from {} to {}", 
                detail.getId(), oldStatus, newStatus);
        });
        
        orderDetailEntityService.saveAllOrderDetailEntities(orderDetails);
        
        // Auto-update Order Status based on all OrderDetails
        UUID orderId = orderDetails.get(0).getOrderEntity().getId();
        updateOrderStatusBasedOnDetails(orderId);
        
        log.info("Successfully updated {} OrderDetails to status {} for assignment {}", 
            orderDetails.size(), newStatus, vehicleAssignmentId);
    }
    
    @Override
    @Transactional
    public void updateOrderDetailStatus(UUID orderDetailId, OrderDetailStatusEnum newStatus) {
        log.info("Updating OrderDetail {} to status {}", orderDetailId, newStatus);
        
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
        
        log.info("Successfully updated OrderDetail {} to status {}", orderDetailId, newStatus);
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
                newStatus == OrderDetailStatusEnum.ASSIGNED_TO_DRIVER
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES;
                
            case ASSIGNED_TO_DRIVER -> 
                newStatus == OrderDetailStatusEnum.PICKING_UP 
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES;
                
            case PICKING_UP -> 
                newStatus == OrderDetailStatusEnum.ON_DELIVERED  // After seal confirmation
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES;
                
            case ON_DELIVERED -> 
                newStatus == OrderDetailStatusEnum.ONGOING_DELIVERED  // Auto-triggered by proximity
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES;
                
            case ONGOING_DELIVERED -> 
                newStatus == OrderDetailStatusEnum.DELIVERED  // After photo upload
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES;
                
            case DELIVERED -> 
                newStatus == OrderDetailStatusEnum.SUCCESSFUL  // After odometer end upload
                || newStatus == OrderDetailStatusEnum.RETURNING  // Start return process
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES
                || newStatus == OrderDetailStatusEnum.REJECTED;
                
            case IN_TROUBLES -> 
                newStatus == OrderDetailStatusEnum.RESOLVED
                || newStatus == OrderDetailStatusEnum.COMPENSATION;  // Can go to compensation if issue requires it
                
            case RESOLVED -> 
                // Can resume from where it was interrupted
                newStatus == OrderDetailStatusEnum.PICKING_UP
                || newStatus == OrderDetailStatusEnum.ON_DELIVERED
                || newStatus == OrderDetailStatusEnum.ONGOING_DELIVERED
                || newStatus == OrderDetailStatusEnum.DELIVERED
                || newStatus == OrderDetailStatusEnum.COMPENSATION;  // Can go to compensation after resolution
                
            case RETURNING -> 
                newStatus == OrderDetailStatusEnum.RETURNED
                || newStatus == OrderDetailStatusEnum.IN_TROUBLES;
                
            // Terminal states - no further transitions allowed
            case SUCCESSFUL, REJECTED, RETURNED, COMPENSATION -> false;
        };
    }
    
    /**
     * Auto-update Order Status based on the status of all its OrderDetails
     * Strategy for Multi-Trip Orders:
     * - Order Status reflects the furthest progress (MAX) among all trips
     * - EXCEPT for terminal states which have special rules:
     *   + SUCCESSFUL: Requires at least 1 trip SUCCESSFUL (partial success is success)
     *   + DELIVERED: Requires ALL trips DELIVERED
     *   + Problem states (IN_TROUBLES, COMPENSATION) have highest priority
     *   + REJECTED/RETURNED don't block other trips from completing
     */
    private void updateOrderStatusBasedOnDetails(UUID orderId) {
        log.debug("Auto-updating Order {} status based on OrderDetails", orderId);
        
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
            log.info("Auto-updated Order {} status from {} to {} based on OrderDetails (trips: {})", 
                orderId, currentOrderStatus, newOrderStatus, statuses.size());
            
            // Send WebSocket notification for status change
            try {
                orderStatusWebSocketService.sendOrderStatusChange(
                    orderId,
                    order.getOrderCode(),
                    previousStatus,
                    newOrderStatus
                );
                log.info("📢 Sent WebSocket notification for Order {} status change: {} → {}", 
                    orderId, previousStatus, newOrderStatus);
            } catch (Exception e) {
                log.error("❌ Failed to send WebSocket notification for order status change: {}", e.getMessage());
                // Don't throw - WebSocket failure shouldn't break business logic
            }
        } else {
            log.debug("Order {} status unchanged: {}", orderId, currentOrderStatus);
        }
    }
    
    /**
     * Determine Order Status for Multi-Trip Orders
     * Strategy: Order Status reflects GENERAL/HIGH-LEVEL progress
     * OrderDetail Status contains DETAILED information (SUCCESSFUL, REJECTED, COMPENSATION)
     * 
     * Priority Order (highest to lowest):
     * 1. COMPENSATION - terminal state (ANY trip)
     * 2. IN_TROUBLES - all trips must be in troubles (require ALL)
     * 3. RETURNING - all trips must be returning (require ALL)
     * 4. RETURNED - all trips must be returned (require ALL)
     * 5. DELIVERED - ALL trips must be DELIVERED (not just furthest progress)
     * 6. SUCCESSFUL - ALL trips must be SUCCESSFUL (not just furthest progress)
     * 7. Progressive states (PICKING_UP, ON_DELIVERED, ONGOING_DELIVERED) - use MAX
     * 
     * Business Rules:
     * - COMPENSATION: ANY trip → Order COMPENSATION (terminal, highest priority)
     * - IN_TROUBLES: ALL trips must be in troubles
     * - RETURNING/RETURNED: ALL trips must be returning/returned
     * - DELIVERED: Requires ALL trips DELIVERED (not just one)
     *   + If 1 trip DELIVERED, others ONGOING_DELIVERED → Order ONGOING_DELIVERED (wait for all)
     *   + If ALL trips DELIVERED → Order DELIVERED
     * - SUCCESSFUL: Requires ALL trips SUCCESSFUL (not just one)
     *   + If 1 trip SUCCESSFUL, others DELIVERED → Order DELIVERED (wait for all)
     *   + If ALL trips SUCCESSFUL → Order SUCCESSFUL
     * - Progressive states: Use MAX (furthest progress)
     * - REJECTED doesn't affect Order status, only visible in OrderDetails
     */
    private OrderDetailStatusEnum determineOrderStatusForMultiTrip(List<OrderDetailStatusEnum> statuses) {
        // ============================================
        // PRIORITY 1: COMPENSATION (Terminal state - ANY trip)
        // ============================================
        // If ANY trip has COMPENSATION → Order has COMPENSATION
        if (statuses.stream().anyMatch(s -> s == OrderDetailStatusEnum.COMPENSATION)) {
            log.debug("Order has trips with COMPENSATION");
            return OrderDetailStatusEnum.COMPENSATION;
        }
        
        // ============================================
        // PRIORITY 2: PROBLEM STATES (ANY trip has problem)
        // ============================================
        // If ANY trip is IN_TROUBLES → Order is IN_TROUBLES (highest priority among active states)
        // This handles cases like: 1 trip SUCCESSFUL + 1 trip IN_TROUBLES → Order IN_TROUBLES
        boolean hasInTroubles = statuses.stream()
            .anyMatch(s -> s == OrderDetailStatusEnum.IN_TROUBLES);
        if (hasInTroubles) {
            log.debug("Some trips IN_TROUBLES → Order IN_TROUBLES (problem takes priority)");
            return OrderDetailStatusEnum.IN_TROUBLES;
        }
        
        // ============================================
        // PRIORITY 3: RETURN FLOW (Require ALL trips)
        // ============================================
        
        // If ALL trips are RETURNING → Order is RETURNING
        boolean allReturning = statuses.stream()
            .allMatch(s -> s == OrderDetailStatusEnum.RETURNING);
        if (allReturning) {
            log.debug("All trips RETURNING → Order RETURNING");
            return OrderDetailStatusEnum.RETURNING;
        }
        
        // If ALL trips are RETURNED → Order is RETURNED
        boolean allReturned = statuses.stream()
            .allMatch(s -> s == OrderDetailStatusEnum.RETURNED);
        if (allReturned) {
            log.debug("All trips RETURNED");
            return OrderDetailStatusEnum.RETURNED;
        }
        
        // If SOME trips RETURNED but not all → continue with non-returned trips
        // Find max status of non-returned trips
        if (statuses.stream().anyMatch(s -> s == OrderDetailStatusEnum.RETURNED)) {
            OrderDetailStatusEnum maxNonReturned = statuses.stream()
                .filter(s -> s != OrderDetailStatusEnum.RETURNED)
                .max(Comparator.comparingInt(Enum::ordinal))
                .orElse(OrderDetailStatusEnum.RETURNED);
            log.debug("Some trips RETURNED, continuing with max non-returned: {}", maxNonReturned);
            return maxNonReturned;
        }
        
        // ============================================
        // PRIORITY 4: SUCCESSFUL (Require ALL trips - Terminal state)
        // ============================================
        // If ALL trips are SUCCESSFUL → Order is SUCCESSFUL
        boolean allSuccessful = statuses.stream()
            .allMatch(s -> s == OrderDetailStatusEnum.SUCCESSFUL);
        if (allSuccessful) {
            log.debug("All trips SUCCESSFUL → Order SUCCESSFUL");
            return OrderDetailStatusEnum.SUCCESSFUL;
        }
        
        // If SOME trips SUCCESSFUL but not all → Order stays at DELIVERED (wait for all)
        boolean hasSomeSuccessful = statuses.stream()
            .anyMatch(s -> s == OrderDetailStatusEnum.SUCCESSFUL);
        if (hasSomeSuccessful) {
            log.debug("Some trips SUCCESSFUL but not all → Order DELIVERED (waiting for all trips to complete)");
            return OrderDetailStatusEnum.DELIVERED;
        }
        
        // ============================================
        // PRIORITY 5: DELIVERED (Require ALL trips)
        // ============================================
        // If ALL trips are DELIVERED → Order is DELIVERED
        boolean allDelivered = statuses.stream()
            .allMatch(s -> s == OrderDetailStatusEnum.DELIVERED);
        if (allDelivered) {
            log.debug("All trips DELIVERED → Order DELIVERED");
            return OrderDetailStatusEnum.DELIVERED;
        }
        
        // If SOME trips DELIVERED but not all → Order stays at ONGOING_DELIVERED (wait for all)
        boolean hasSomeDelivered = statuses.stream()
            .anyMatch(s -> s == OrderDetailStatusEnum.DELIVERED);
        if (hasSomeDelivered) {
            log.debug("Some trips DELIVERED but not all → Order ONGOING_DELIVERED (waiting for all trips to be delivered)");
            return OrderDetailStatusEnum.ONGOING_DELIVERED;
        }
        
        // ============================================
        // PRIORITY 6: NORMAL PROGRESSIVE STATES (Use MAX)
        // ============================================
        // For progressive states (PICKING_UP, ON_DELIVERED, ONGOING_DELIVERED), use the furthest progress
        // This allows:
        // - Live tracking as soon as any trip starts (PICKING_UP)
        // - If 1 trip ONGOING_DELIVERED and others ON_DELIVERED → Order is ONGOING_DELIVERED
        OrderDetailStatusEnum maxStatus = statuses.stream()
            .filter(s -> s.ordinal() < OrderDetailStatusEnum.DELIVERED.ordinal())
            .max(Comparator.comparingInt(Enum::ordinal))
            .orElse(OrderDetailStatusEnum.PENDING);
        
        log.debug("Using MAX progressive status: {}", maxStatus);
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
            case DELIVERED -> OrderStatusEnum.DELIVERED;
            case SUCCESSFUL -> OrderStatusEnum.SUCCESSFUL;
            case IN_TROUBLES -> OrderStatusEnum.IN_TROUBLES;
            case RESOLVED -> OrderStatusEnum.RESOLVED;
            case COMPENSATION -> OrderStatusEnum.COMPENSATION;
            case REJECTED -> OrderStatusEnum.REJECTED;
            case RETURNING -> OrderStatusEnum.RETURNING;
            case RETURNED -> OrderStatusEnum.RETURNED;
        };
    }
}
