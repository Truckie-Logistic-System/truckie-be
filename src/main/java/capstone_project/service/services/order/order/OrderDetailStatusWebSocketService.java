package capstone_project.service.services.order.order;

import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.dtos.response.order.OrderDetailStatusChangeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for sending order detail (package) status change notifications via WebSocket
 * Allows frontend to track individual package status changes in real-time
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailStatusWebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Send order detail status change notification to WebSocket topic
     * Topic: /topic/orders/{orderId}/order-details/status
     * 
     * @param orderDetailId Order detail ID
     * @param trackingCode Tracking code of the order detail
     * @param orderId Parent order ID
     * @param orderCode Parent order code
     * @param vehicleAssignmentId Vehicle assignment ID (can be null)
     * @param previousStatus Previous status (can be null for initial status)
     * @param newStatus New status
     */
    public void sendOrderDetailStatusChange(
            UUID orderDetailId,
            String trackingCode,
            UUID orderId,
            String orderCode,
            UUID vehicleAssignmentId,
            OrderDetailStatusEnum previousStatus,
            OrderDetailStatusEnum newStatus
    ) {
        try {
            OrderDetailStatusChangeMessage message = OrderDetailStatusChangeMessage.builder()
                    .orderDetailId(orderDetailId)
                    .trackingCode(trackingCode)
                    .orderId(orderId)
                    .orderCode(orderCode)
                    .vehicleAssignmentId(vehicleAssignmentId)
                    .previousStatus(previousStatus != null ? previousStatus.name() : null)
                    .newStatus(newStatus.name())
                    .timestamp(Instant.now())
                    .message(getStatusChangeMessage(newStatus))
                    .build();
            
            String topic = "/topic/orders/" + orderId + "/order-details/status";
            messagingTemplate.convertAndSend(topic, message);
            
            log.info("📦 [OrderDetailStatusWebSocket] Sent status change notification: {} -> {} for package {} (order {})",
                    previousStatus, newStatus, trackingCode, orderCode);
        } catch (Exception e) {
            // Don't throw exception - WebSocket notification failure shouldn't break business logic
            log.error("❌ [OrderDetailStatusWebSocket] Failed to send status change notification for package {}: {}",
                    trackingCode, e.getMessage(), e);
        }
    }
    
    /**
     * Convenience method when previous status is a String (from entity.getStatus())
     */
    public void sendOrderDetailStatusChange(
            UUID orderDetailId,
            String trackingCode,
            UUID orderId,
            String orderCode,
            UUID vehicleAssignmentId,
            String previousStatus,
            OrderDetailStatusEnum newStatus
    ) {
        OrderDetailStatusEnum previousStatusEnum = null;
        if (previousStatus != null && !previousStatus.isEmpty()) {
            try {
                previousStatusEnum = OrderDetailStatusEnum.valueOf(previousStatus);
            } catch (IllegalArgumentException e) {
                log.warn("⚠️ Invalid previous status: {}", previousStatus);
            }
        }
        
        sendOrderDetailStatusChange(
                orderDetailId,
                trackingCode,
                orderId,
                orderCode,
                vehicleAssignmentId,
                previousStatusEnum,
                newStatus
        );
    }
    
    /**
     * Get human-readable message for status change
     */
    private String getStatusChangeMessage(OrderDetailStatusEnum status) {
        switch (status) {
            case PENDING:
                return "Kiện hàng đang chờ xử lý";
            case PICKING_UP:
                return "Tài xế đang đến lấy kiện hàng";
            case ON_DELIVERED:
                return "Kiện hàng đang được vận chuyển";
            case ONGOING_DELIVERED:
                return "Kiện hàng sắp được giao";
            case DELIVERED:
                return "Kiện hàng đã giao thành công";
            case IN_TROUBLES:
                return "Kiện hàng gặp sự cố";
            case COMPENSATION:
                return "Kiện hàng đang được bồi thường";
            case SUCCESSFUL:
                return "Kiện hàng đã hoàn thành";
            case RETURNING:
                return "Kiện hàng đang được trả về";
            case RETURNED:
                return "Kiện hàng đã trả về";
            case CANCELLED:
                return "Kiện hàng đã bị hủy";
            default:
                return "Trạng thái kiện hàng đã được cập nhật";
        }
    }
}
