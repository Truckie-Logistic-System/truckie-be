package capstone_project.service.services.order.order;

import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.dtos.response.order.OrderStatusChangeMessage;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for sending order status change notifications via WebSocket
 * Uses event-based approach to ensure WebSocket messages are sent AFTER transaction commits
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusWebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    private final ApplicationEventPublisher eventPublisher;
    
    /**
     * Event class for order status change - will be handled after transaction commits
     */
    @Getter
    public static class OrderStatusChangeEvent {
        private final UUID orderId;
        private final String orderCode;
        private final OrderStatusEnum previousStatus;
        private final OrderStatusEnum newStatus;
        
        public OrderStatusChangeEvent(UUID orderId, String orderCode, 
                                       OrderStatusEnum previousStatus, OrderStatusEnum newStatus) {
            this.orderId = orderId;
            this.orderCode = orderCode;
            this.previousStatus = previousStatus;
            this.newStatus = newStatus;
        }
    }
    
    /**
     * Publish order status change event - will be handled after transaction commits
     * This ensures staff/customer receives the updated status, not the stale one
     * 
     * @param orderId Order ID
     * @param orderCode Order code
     * @param previousStatus Previous status
     * @param newStatus New status
     */
    public void sendOrderStatusChange(
            UUID orderId,
            String orderCode,
            OrderStatusEnum previousStatus,
            OrderStatusEnum newStatus
    ) {
        log.info("📤 [OrderStatusWebSocket] Publishing status change event for order {}: {} -> {}", 
            orderCode, previousStatus, newStatus);
        eventPublisher.publishEvent(new OrderStatusChangeEvent(orderId, orderCode, previousStatus, newStatus));
    }
    
    /**
     * Handle order status change event AFTER transaction commits
     * This ensures the database has the updated status before WebSocket message is sent
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusChangeEvent(OrderStatusChangeEvent event) {
        try {
            log.info("✅ [OrderStatusWebSocket] Transaction committed, sending WebSocket for order {}: {} -> {}", 
                event.getOrderCode(), event.getPreviousStatus(), event.getNewStatus());
            
            OrderStatusChangeMessage message = OrderStatusChangeMessage.builder()
                    .orderId(event.getOrderId())
                    .orderCode(event.getOrderCode())
                    .previousStatus(event.getPreviousStatus() != null ? event.getPreviousStatus().name() : null)
                    .newStatus(event.getNewStatus().name())
                    .timestamp(Instant.now())
                    .message(getStatusChangeMessage(event.getNewStatus()))
                    .build();
            
            String topic = "/topic/orders/" + event.getOrderId() + "/status";
            messagingTemplate.convertAndSend(topic, message);
            
            log.info("📢 [OrderStatusWebSocket] WebSocket message sent to topic: {}", topic);

        } catch (Exception e) {
            // Don't throw exception - WebSocket notification failure shouldn't break business logic
            log.error("❌ [OrderStatusWebSocket] Failed to send status change notification for order {}: {}",
                    event.getOrderCode(), e.getMessage(), e);
        }
    }
    
    /**
     * Get human-readable message for status change
     */
    private String getStatusChangeMessage(OrderStatusEnum status) {
        switch (status) {
            case PENDING:
                return "Đơn hàng đang chờ xử lý";
            case PROCESSING:
                return "Đơn hàng đang được xử lý";
            case CONTRACT_DRAFT:
                return "Hợp đồng đã được tạo";
            case CONTRACT_SIGNED:
                return "Hợp đồng đã được ký";
            case ON_PLANNING:
                return "Đơn hàng đang được lên kế hoạch vận chuyển";
            case ASSIGNED_TO_DRIVER:
                return "Đơn hàng đã được phân công cho tài xế";
            case FULLY_PAID:
                return "Đơn hàng đã thanh toán đầy đủ";
            case PICKING_UP:
                return "Tài xế đã bắt đầu lấy hàng";
            case ON_DELIVERED:
                return "Đang trên đường giao hàng";
            case ONGOING_DELIVERED:
                return "Sắp giao hàng tới";
            case DELIVERED:
                return "Đã giao hàng thành công";
            case IN_TROUBLES:
                return "Đơn hàng gặp sự cố";
            case COMPENSATION:
                return "Đang xử lý bồi thường";
            case SUCCESSFUL:
                return "Đơn hàng hoàn thành thành công";
            case RETURNING:
                return "Đang trả hàng";
            case RETURNED:
                return "Đã trả hàng";
            case CANCELLED:
                return "Đơn hàng đã bị hủy";
            default:
                return "Trạng thái đơn hàng đã được cập nhật";
        }
    }
}
