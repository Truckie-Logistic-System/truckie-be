package capstone_project.service.services.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service for broadcasting Notification-related WebSocket messages
 * Handles real-time notification delivery to users via WebSocket
 * Uses shared vehicle-tracking-browser endpoint with /topic/user/{userId}/notifications topic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationWebSocketService {
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Send lightweight notification update signal to user
     * Optimized: Only sends signal since frontend refetches data anyway
     * @param userId User ID
     * @param action Action type (NEW, READ, READ_ALL)
     */
    public void sendNotificationUpdateSignal(UUID userId, String action) {
        try {
            // Create lightweight signal payload (~30 bytes vs 500+ bytes)
            Map<String, Object> signal = new HashMap<>();
            signal.put("type", "NOTIFICATION_UPDATE");
            signal.put("action", action);
            signal.put("timestamp", Instant.now().toString());
            
            // Send to specific user topic
            messagingTemplate.convertAndSend(
                "/topic/user/" + userId + "/notifications",
                signal
            );
            
            log.info("✅ [NotificationWebSocket] Sent notification signal to user {}: {}", 
                    userId, action);
                    
        } catch (Exception e) {
            log.error("❌ [NotificationWebSocket] Failed to send notification signal to user: {}", userId, e);
        }
    }

    /**
     * Send detailed notification to specific user (kept for mobile push notifications)
     * @param userId User ID
     * @param notificationType Type of notification
     * @param title Notification title
     * @param description Notification description/message
     * @param priority Priority level (HIGH, MEDIUM, LOW)
     * @param metadata Additional metadata (optional)
     */
    public void sendDetailedNotificationToUser(
            UUID userId,
            String notificationType,
            String title,
            String description,
            String priority,
            Map<String, Object> metadata) {

        try {
            // Create notification payload
            Map<String, Object> notification = new HashMap<>();
            notification.put("id", UUID.randomUUID().toString());
            notification.put("notificationType", notificationType);
            notification.put("title", title);
            notification.put("description", description);
            notification.put("priority", priority != null ? priority : "MEDIUM");
            notification.put("isRead", false);
            notification.put("createdAt", Instant.now().toString());
            notification.put("timestamp", Instant.now().toString());
            
            // Add metadata if provided
            if (metadata != null && !metadata.isEmpty()) {
                notification.put("metadata", metadata);
            }
            
            // Send to specific user topic
            messagingTemplate.convertAndSend(
                "/topic/user/" + userId + "/notifications",
                notification
            );
            
            log.info("✅ [NotificationWebSocket] Sent detailed notification to user {}: {} - {}", 
                    userId, notificationType, title);
                    
        } catch (Exception e) {
            log.error("❌ [NotificationWebSocket] Failed to send detailed notification to user: {}", userId, e);
        }
    }

    /**
     * Send order status change notification
     * Optimized: Uses lightweight signal since frontend refetches data anyway
     * 
     * @param userId User ID
     * @param orderId Order ID
     * @param orderStatus New order status
     * @param orderCode Order tracking code
     */
    public void sendOrderStatusChangeNotification(
            UUID userId,
            UUID orderId,
            String orderStatus,
            String orderCode) {

        try {
            // Send lightweight signal - frontend will refetch order details
            sendNotificationUpdateSignal(userId, "NEW");

            log.info("✅ [NotificationWebSocket] Sent order status change signal to user: {} (order: {} - {})", 
                    userId, orderCode, orderStatus);

        } catch (Exception e) {
            log.error("❌ [NotificationWebSocket] Error sending order status notification: {}", 
                    e.getMessage(), e);
        }
    }

    /**
     * Send payment success notification
     * Optimized: Uses lightweight signal since frontend refetches data anyway
     * 
     * @param userId User ID
     * @param orderId Order ID
     * @param orderCode Order code
     * @param amount Payment amount
     */
    public void sendPaymentSuccessNotification(
            UUID userId,
            UUID orderId,
            String orderCode,
            Long amount) {

        try {
            // Send lightweight signal - frontend will refetch payment details
            sendNotificationUpdateSignal(userId, "NEW");

            log.info("✅ [NotificationWebSocket] Sent payment success signal to user: {} (order: {} - amount: {} VND)", 
                    userId, orderCode, amount);

        } catch (Exception e) {
            log.error("❌ [NotificationWebSocket] Error sending payment success notification: {}", 
                    e.getMessage(), e);
        }
    }

    /**
     * Send issue status change notification
     * Optimized: Uses lightweight signal since frontend refetches data anyway
     * 
     * @param userId User ID
     * @param issueId Issue ID
     * @param issueCategory Issue category
     * @param issueStatus New status
     */
    public void sendIssueStatusChangeNotification(
            UUID userId,
            UUID issueId,
            String issueCategory,
            String issueStatus) {

        try {
            // Send lightweight signal - frontend will refetch issue details
            sendNotificationUpdateSignal(userId, "NEW");

            log.info("✅ [NotificationWebSocket] Sent issue status change signal to user: {} (issue: {} - {} -> {})", 
                    userId, issueCategory, issueStatus);

        } catch (Exception e) {
            log.error("❌ [NotificationWebSocket] Error sending issue status notification: {}", 
                    e.getMessage(), e);
        }
    }

    /**
     * Broadcast notification to all STAFF users
     * 
     * @param notificationType Type of notification
     * @param title Notification title
     * @param description Notification description
     * @param metadata Additional metadata
     */
    public void broadcastToStaff(
            String notificationType,
            String title,
            String description,
            Map<String, Object> metadata) {

        try {
            Map<String, Object> notification = new HashMap<>();
            notification.put("id", UUID.randomUUID().toString());
            notification.put("notificationType", notificationType);
            notification.put("title", title);
            notification.put("description", description);
            notification.put("priority", "MEDIUM");
            notification.put("recipientRole", "STAFF");
            notification.put("isRead", false);
            notification.put("createdAt", Instant.now().toString());
            notification.put("timestamp", Instant.now().toString());
            
            if (metadata != null && !metadata.isEmpty()) {
                notification.put("metadata", metadata);
            }
            
            // Broadcast to all staff via public topic
            messagingTemplate.convertAndSend("/topic/staff/notifications", notification);
            
            log.info("✅ [NotificationWebSocket] Broadcasted notification to all staff (type: {})", 
                    notificationType);

        } catch (Exception e) {
            log.error("❌ [NotificationWebSocket] Error broadcasting to staff: {}", 
                    e.getMessage(), e);
        }
    }

    // Helper methods for translating status codes
    private String translateOrderStatus(String status) {
        return switch (status) {
            case "PENDING" -> "Chờ xử lý";
            case "PROCESSING" -> "Đang xử lý";
            case "ASSIGNED_TO_DRIVER" -> "Đã giao cho tài xế";
            case "PICKING_UP" -> "Đang lấy hàng";
            case "ON_DELIVERED" -> "Đang giao hàng";
            case "DELIVERED" -> "Đã giao hàng";
            case "SUCCESSFUL" -> "Hoàn thành";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    private String translateIssueCategory(String category) {
        return switch (category) {
            case "DAMAGE" -> "Hàng hóa bị hư hỏng";
            case "PENALTY" -> "Vi phạm";
            case "REROUTE" -> "Thay đổi lộ trình";
            case "SEAL_REPLACEMENT" -> "Thay thế seal";
            case "ORDER_REJECTION" -> "Khách hàng từ chối nhận hàng";
            default -> category;
        };
    }

    private String translateIssueStatus(String status) {
        return switch (status) {
            case "OPEN" -> "Đang mở";
            case "IN_PROGRESS" -> "Đang xử lý";
            case "RESOLVED" -> "Đã giải quyết";
            case "CLOSED" -> "Đã đóng";
            default -> status;
        };
    }
}
