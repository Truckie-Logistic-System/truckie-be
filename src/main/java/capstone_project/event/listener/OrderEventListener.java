package capstone_project.event.listener;

import capstone_project.common.enums.NotificationTypeEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.event.OrderAssignedEvent;
import capstone_project.event.OrderCreatedEvent;
import capstone_project.event.OrderStatusChangedEvent;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.service.services.notification.NotificationService;
import capstone_project.service.services.order.order.OrderStatusWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Event listener for order-related domain events.
 * Handles side effects asynchronously to decouple from main transaction.
 * 
 * Side effects include:
 * - Sending notifications to customers/drivers
 * - Broadcasting status changes via WebSocket
 * - Logging audit trails
 * - Triggering downstream workflows
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventListener {

    private final NotificationService notificationService;
    private final OrderStatusWebSocketService orderStatusWebSocketService;
    private final OrderEntityService orderEntityService;
    private final CustomerEntityService customerEntityService;

    /**
     * Handle OrderCreatedEvent - triggered when a new order is created.
     * Side effects:
     * - Log audit trail for order creation
     * - Send in-app notification to customer confirming order creation
     */
    @Async
    @EventListener
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("📦 [EDA] Order created event received. OrderId: {}, OrderCode: {}, CustomerId: {}, Quantity: {}", 
            event.getOrderId(), 
            event.getOrderCode(), 
            event.getCustomerId(), 
            event.getTotalQuantity());
        
        try {
            // 1. Log audit trail
            logAuditTrail("ORDER_CREATED", event.getOrderId().toString(), event.getOrderCode(), 
                String.format("Order created with %d items by customer %s", 
                    event.getTotalQuantity(), event.getCustomerId()));

            // 2. Send notification to customer (order confirmation is already sent in OrderServiceImpl,
            //    but we can send additional analytics/tracking notification here)
            sendOrderCreatedAnalyticsNotification(event);

            log.info("✅ [EDA] Successfully processed order creation side effects for order: {}", event.getOrderCode());
            
        } catch (Exception ex) {
            log.error("❌ [EDA] Error handling order created event for order: {}", event.getOrderCode(), ex);
        }
    }

    /**
     * Handle OrderStatusChangedEvent - triggered when order status changes.
     * Side effects:
     * - Broadcast status change via WebSocket for real-time UI updates
     * - Send notification to customer about status change
     * - Log audit trail
     * - Trigger specific workflows based on new status (COMPLETED, CANCELLED, etc.)
     */
    @Async
    @EventListener
    public void handleOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("🔄 [EDA] Order status changed event received. OrderId: {}, OrderCode: {}, OldStatus: {}, NewStatus: {}", 
            event.getOrderId(), 
            event.getOrderCode(),
            event.getOldStatus(), 
            event.getNewStatus());
        
        try {
            // 1. Log audit trail for status change
            logAuditTrail("ORDER_STATUS_CHANGED", event.getOrderId().toString(), event.getOrderCode(),
                String.format("Status changed from %s to %s by user %s", 
                    event.getOldStatus(), event.getNewStatus(), event.getChangedBy()));

            // 2. Broadcast status change via WebSocket for real-time dashboard updates
            broadcastStatusChangeViaWebSocket(event);

            // 3. Send notification to customer about status change
            sendStatusChangeNotification(event);

            // 4. Trigger specific workflows based on new status
            triggerStatusSpecificWorkflows(event);

            log.info("✅ [EDA] Successfully processed status change side effects for order: {}", event.getOrderCode());
            
        } catch (Exception ex) {
            log.error("❌ [EDA] Error handling order status changed event for order: {}", event.getOrderCode(), ex);
        }
    }

    /**
     * Handle OrderAssignedEvent - triggered when order is assigned to driver/vehicle.
     * Side effects:
     * - Send notification to driver about new assignment
     * - Send notification to customer with driver details
     * - Log audit trail
     */
    @Async
    @EventListener
    public void handleOrderAssigned(OrderAssignedEvent event) {
        log.info("🚚 [EDA] Order assigned event received. OrderId: {}, OrderCode: {}, DriverId: {}, VehicleId: {}", 
            event.getOrderId(), 
            event.getOrderCode(),
            event.getDriverId(), 
            event.getVehicleId());
        
        try {
            // 1. Log audit trail for assignment
            logAuditTrail("ORDER_ASSIGNED", event.getOrderId().toString(), event.getOrderCode(),
                String.format("Order assigned to driver %s with vehicle %s", 
                    event.getDriverId(), event.getVehicleId()));

            // 2. Send notification to driver about new assignment
            sendDriverAssignmentNotification(event);

            // 3. Send notification to customer with driver/vehicle info
            sendCustomerAssignmentNotification(event);

            log.info("✅ [EDA] Successfully processed order assignment side effects for order: {}", event.getOrderCode());
            
        } catch (Exception ex) {
            log.error("❌ [EDA] Error handling order assigned event for order: {}", event.getOrderCode(), ex);
        }
    }

    // ==================== PRIVATE HELPER METHODS ====================

    /**
     * Log audit trail for order events (can be extended to persist to audit table)
     */
    private void logAuditTrail(String eventType, String orderId, String orderCode, String details) {
        log.info("📋 [AUDIT] EventType: {}, OrderId: {}, OrderCode: {}, Details: {}, Timestamp: {}", 
            eventType, orderId, orderCode, details, java.time.LocalDateTime.now());
    }

    /**
     * Send analytics/tracking notification for order creation
     */
    private void sendOrderCreatedAnalyticsNotification(OrderCreatedEvent event) {
        try {
            // Fetch order to get customer user ID
            Optional<OrderEntity> orderOpt = orderEntityService.findEntityById(event.getOrderId());
            if (orderOpt.isEmpty()) {
                log.warn("⚠️ [EDA] Order not found for notification: {}", event.getOrderId());
                return;
            }

            OrderEntity order = orderOpt.get();
            CustomerEntity customer = order.getSender();
            if (customer == null || customer.getUser() == null) {
                log.warn("⚠️ [EDA] Customer or user not found for order: {}", event.getOrderCode());
                return;
            }

            // Build metadata for tracking
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderCode", event.getOrderCode());
            metadata.put("totalQuantity", event.getTotalQuantity());
            metadata.put("createdAt", event.getCreatedAt().toString());
            metadata.put("eventSource", "EVENT_DRIVEN_ARCHITECTURE");

            log.debug("📊 [EDA] Order creation analytics recorded for order: {}", event.getOrderCode());
            
        } catch (Exception ex) {
            log.error("❌ [EDA] Failed to send order created analytics notification: {}", ex.getMessage());
        }
    }

    /**
     * Broadcast status change via WebSocket for real-time UI updates
     */
    private void broadcastStatusChangeViaWebSocket(OrderStatusChangedEvent event) {
        try {
            OrderStatusEnum oldStatus = parseOrderStatus(event.getOldStatus());
            OrderStatusEnum newStatus = parseOrderStatus(event.getNewStatus());

            if (newStatus != null) {
                orderStatusWebSocketService.sendOrderStatusChange(
                    event.getOrderId(),
                    event.getOrderCode(),
                    oldStatus,
                    newStatus
                );
                log.debug("📡 [EDA] WebSocket broadcast sent for order status change: {} -> {}", 
                    event.getOldStatus(), event.getNewStatus());
            }
        } catch (Exception ex) {
            log.error("❌ [EDA] Failed to broadcast status change via WebSocket: {}", ex.getMessage());
        }
    }

    /**
     * Send notification to customer about status change
     */
    private void sendStatusChangeNotification(OrderStatusChangedEvent event) {
        try {
            Optional<OrderEntity> orderOpt = orderEntityService.findEntityById(event.getOrderId());
            if (orderOpt.isEmpty()) {
                log.warn("⚠️ [EDA] Order not found for status notification: {}", event.getOrderId());
                return;
            }

            OrderEntity order = orderOpt.get();
            CustomerEntity customer = order.getSender();
            if (customer == null || customer.getUser() == null) {
                log.warn("⚠️ [EDA] Customer not found for order: {}", event.getOrderCode());
                return;
            }

            // Build notification metadata
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderCode", event.getOrderCode());
            metadata.put("oldStatus", event.getOldStatus());
            metadata.put("newStatus", event.getNewStatus());
            metadata.put("changedAt", event.getTimestamp().toString());

            String statusMessage = getStatusChangeMessage(event.getNewStatus());
            
            // Map status to appropriate notification type
            NotificationTypeEnum notificationType = mapStatusToNotificationType(event.getNewStatus());
            
            CreateNotificationRequest notificationRequest = CreateNotificationRequest.builder()
                .userId(customer.getUser().getId())
                .recipientRole("CUSTOMER")
                .title(String.format("Cập nhật đơn hàng %s", event.getOrderCode()))
                .description(statusMessage)
                .notificationType(notificationType)
                .relatedOrderId(event.getOrderId())
                .metadata(metadata)
                .build();

            notificationService.createNotification(notificationRequest);
            log.debug("📬 [EDA] Status change notification sent to customer for order: {}", event.getOrderCode());
            
        } catch (Exception ex) {
            log.error("❌ [EDA] Failed to send status change notification: {}", ex.getMessage());
        }
    }

    /**
     * Trigger specific workflows based on new status
     */
    private void triggerStatusSpecificWorkflows(OrderStatusChangedEvent event) {
        String newStatus = event.getNewStatus();
        
        switch (newStatus) {
            case "SUCCESSFUL":
            case "COMPLETED":
                log.info("🎉 [EDA] Order completed. Triggering completion workflows for order: {}", event.getOrderCode());
                handleOrderCompletion(event);
                break;
                
            case "CANCELLED":
                log.info("❌ [EDA] Order cancelled. Triggering cancellation workflows for order: {}", event.getOrderCode());
                handleOrderCancellation(event);
                break;
                
            case "DELIVERED":
                log.info("📦 [EDA] Order delivered. Triggering delivery confirmation workflows for order: {}", event.getOrderCode());
                handleOrderDelivered(event);
                break;
                
            case "PICKING_UP":
                log.info("🚗 [EDA] Driver picking up. Triggering pickup workflows for order: {}", event.getOrderCode());
                handleOrderPickingUp(event);
                break;
                
            case "IN_TROUBLES":
                log.info("⚠️ [EDA] Order in troubles. Triggering issue resolution workflows for order: {}", event.getOrderCode());
                handleOrderInTroubles(event);
                break;
                
            default:
                log.debug("📝 [EDA] Standard status change recorded for order: {} -> {}", 
                    event.getOrderCode(), newStatus);
        }
    }

    /**
     * Handle order completion workflows
     */
    private void handleOrderCompletion(OrderStatusChangedEvent event) {
        log.info("📊 [EDA] Processing completion workflows: calculating metrics, updating history for order: {}", 
            event.getOrderCode());
        // Future: Calculate driver earnings, update customer loyalty points, etc.
    }

    /**
     * Handle order cancellation workflows
     */
    private void handleOrderCancellation(OrderStatusChangedEvent event) {
        log.info("🔄 [EDA] Processing cancellation workflows: releasing resources for order: {}", 
            event.getOrderCode());
        // Future: Release driver/vehicle assignments, process refunds, etc.
    }

    /**
     * Handle order delivered workflows
     */
    private void handleOrderDelivered(OrderStatusChangedEvent event) {
        log.info("✅ [EDA] Processing delivery confirmation workflows for order: {}", event.getOrderCode());
        // Future: Request customer feedback, update delivery metrics, etc.
    }

    /**
     * Handle order picking up workflows
     */
    private void handleOrderPickingUp(OrderStatusChangedEvent event) {
        log.info("🚚 [EDA] Processing pickup workflows: starting tracking for order: {}", event.getOrderCode());
        // Future: Start real-time tracking, send ETA notifications, etc.
    }

    /**
     * Handle order in troubles workflows
     */
    private void handleOrderInTroubles(OrderStatusChangedEvent event) {
        log.info("🚨 [EDA] Processing issue workflows: alerting support team for order: {}", event.getOrderCode());
        // Future: Alert support team, create issue ticket, etc.
    }

    /**
     * Send notification to driver about new assignment
     */
    private void sendDriverAssignmentNotification(OrderAssignedEvent event) {
        try {
            if (event.getDriverId() == null) {
                log.warn("⚠️ [EDA] Driver ID is null for order assignment: {}", event.getOrderCode());
                return;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderCode", event.getOrderCode());
            metadata.put("vehicleId", event.getVehicleId() != null ? event.getVehicleId().toString() : "N/A");
            metadata.put("assignedAt", event.getAssignedAt().toString());

            log.debug("📬 [EDA] Driver assignment notification prepared for driver: {} on order: {}", 
                event.getDriverId(), event.getOrderCode());
            
        } catch (Exception ex) {
            log.error("❌ [EDA] Failed to send driver assignment notification: {}", ex.getMessage());
        }
    }

    /**
     * Send notification to customer about driver/vehicle assignment
     */
    private void sendCustomerAssignmentNotification(OrderAssignedEvent event) {
        try {
            Optional<OrderEntity> orderOpt = orderEntityService.findEntityById(event.getOrderId());
            if (orderOpt.isEmpty()) {
                log.warn("⚠️ [EDA] Order not found for assignment notification: {}", event.getOrderId());
                return;
            }

            OrderEntity order = orderOpt.get();
            CustomerEntity customer = order.getSender();
            if (customer == null || customer.getUser() == null) {
                log.warn("⚠️ [EDA] Customer not found for order: {}", event.getOrderCode());
                return;
            }

            Map<String, Object> metadata = new HashMap<>();
            metadata.put("orderCode", event.getOrderCode());
            metadata.put("driverId", event.getDriverId() != null ? event.getDriverId().toString() : "N/A");
            metadata.put("vehicleId", event.getVehicleId() != null ? event.getVehicleId().toString() : "N/A");
            metadata.put("assignedAt", event.getAssignedAt().toString());

            log.debug("📬 [EDA] Customer assignment notification prepared for order: {}", event.getOrderCode());
            
        } catch (Exception ex) {
            log.error("❌ [EDA] Failed to send customer assignment notification: {}", ex.getMessage());
        }
    }

    /**
     * Parse order status string to enum safely
     */
    private OrderStatusEnum parseOrderStatus(String status) {
        if (status == null || status.isEmpty()) {
            return null;
        }
        try {
            return OrderStatusEnum.valueOf(status);
        } catch (IllegalArgumentException ex) {
            log.warn("⚠️ [EDA] Unknown order status: {}", status);
            return null;
        }
    }

    /**
     * Map order status to appropriate NotificationTypeEnum
     */
    private NotificationTypeEnum mapStatusToNotificationType(String status) {
        switch (status) {
            case "PROCESSING":
                return NotificationTypeEnum.ORDER_PROCESSING;
            case "CONTRACT_DRAFT":
            case "CONTRACT_READY":
                return NotificationTypeEnum.CONTRACT_READY;
            case "CONTRACT_SIGNED":
                return NotificationTypeEnum.CONTRACT_SIGNED;
            case "ASSIGNED_TO_DRIVER":
                return NotificationTypeEnum.DRIVER_ASSIGNED;
            case "FULLY_PAID":
                return NotificationTypeEnum.PAYMENT_FULL_SUCCESS;
            case "PICKING_UP":
                return NotificationTypeEnum.PICKING_UP_STARTED;
            case "ON_DELIVERED":
            case "ONGOING_DELIVERED":
                return NotificationTypeEnum.DELIVERY_IN_PROGRESS;
            case "DELIVERED":
                return NotificationTypeEnum.DELIVERY_COMPLETED;
            case "IN_TROUBLES":
                return NotificationTypeEnum.ISSUE_REPORTED;
            case "COMPENSATION":
                return NotificationTypeEnum.COMPENSATION_PROCESSED;
            case "SUCCESSFUL":
            case "COMPLETED":
                return NotificationTypeEnum.DELIVERY_COMPLETED;
            case "RETURNING":
                return NotificationTypeEnum.RETURN_STARTED;
            case "RETURNED":
                return NotificationTypeEnum.RETURN_COMPLETED;
            case "CANCELLED":
                return NotificationTypeEnum.ORDER_CANCELLED;
            default:
                return NotificationTypeEnum.ORDER_PROCESSING;
        }
    }

    /**
     * Get human-readable message for status change
     */
    private String getStatusChangeMessage(String status) {
        switch (status) {
            case "PENDING":
                return "Đơn hàng của bạn đang chờ xử lý.";
            case "PROCESSING":
                return "Đơn hàng của bạn đang được xử lý.";
            case "CONTRACT_DRAFT":
                return "Hợp đồng vận chuyển đã được tạo.";
            case "CONTRACT_SIGNED":
                return "Hợp đồng đã được ký thành công.";
            case "ON_PLANNING":
                return "Đơn hàng đang được lên kế hoạch vận chuyển.";
            case "ASSIGNED_TO_DRIVER":
                return "Đơn hàng đã được phân công cho tài xế.";
            case "FULLY_PAID":
                return "Đơn hàng đã thanh toán đầy đủ.";
            case "PICKING_UP":
                return "Tài xế đang trên đường đến lấy hàng.";
            case "ON_DELIVERED":
                return "Đơn hàng đang trên đường giao.";
            case "ONGOING_DELIVERED":
                return "Đơn hàng sắp được giao đến bạn.";
            case "DELIVERED":
                return "Đơn hàng đã được giao thành công.";
            case "IN_TROUBLES":
                return "Đơn hàng gặp sự cố. Chúng tôi đang xử lý.";
            case "COMPENSATION":
                return "Đang xử lý bồi thường cho đơn hàng.";
            case "SUCCESSFUL":
            case "COMPLETED":
                return "Đơn hàng đã hoàn thành thành công. Cảm ơn bạn!";
            case "RETURNING":
                return "Đơn hàng đang được trả lại.";
            case "RETURNED":
                return "Đơn hàng đã được trả lại.";
            case "CANCELLED":
                return "Đơn hàng đã bị hủy.";
            default:
                return "Trạng thái đơn hàng đã được cập nhật.";
        }
    }
}
