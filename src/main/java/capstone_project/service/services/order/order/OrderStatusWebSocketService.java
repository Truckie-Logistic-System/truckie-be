package capstone_project.service.services.order.order;

import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.dtos.response.order.OrderStatusChangeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for sending order status change notifications via WebSocket
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderStatusWebSocketService {
    
    private final SimpMessagingTemplate messagingTemplate;
    
    /**
     * Send order status change notification to WebSocket topic
     * Topic: /topic/orders/{orderId}/status
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
        try {
            OrderStatusChangeMessage message = OrderStatusChangeMessage.builder()
                    .orderId(orderId)
                    .orderCode(orderCode)
                    .previousStatus(previousStatus != null ? previousStatus.name() : null)
                    .newStatus(newStatus.name())
                    .timestamp(Instant.now())
                    .message(getStatusChangeMessage(newStatus))
                    .build();
            
            String topic = "/topic/orders/" + orderId + "/status";
            messagingTemplate.convertAndSend(topic, message);

        } catch (Exception e) {
            // Don't throw exception - WebSocket notification failure shouldn't break business logic
            log.error("❌ [OrderStatusWebSocket] Failed to send status change notification for order {}: {}",
                    orderCode, e.getMessage(), e);
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
