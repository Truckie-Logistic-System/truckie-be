package capstone_project.service.services.notification;

import capstone_project.common.enums.NotificationTypeEnum;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
import capstone_project.dtos.request.notification.GeneralNotificationMessageRequest;
import capstone_project.dtos.request.notification.NotificationMessageRequest;
import capstone_project.dtos.response.notification.NotificationResponse;
import capstone_project.dtos.response.notification.NotificationStatsResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.UUID;

public interface NotificationService {
    
    // ============= Legacy WebSocket Methods (giữ lại để tương thích) =============
    void sendToUser(String userId, NotificationMessageRequest message);
    void sendToAll(GeneralNotificationMessageRequest message);
    
    // ============= New Persistent Notification Methods =============
    
    /**
     * Tạo notification mới và lưu vào database
     * Sau đó gửi qua WebSocket, email (nếu customer), push notification (nếu driver)
     */
    NotificationResponse createNotification(CreateNotificationRequest request);
    
    /**
     * Lấy danh sách notifications của user với pagination
     */
    Page<NotificationResponse> getUserNotifications(
        UUID userId, 
        Pageable pageable,
        Boolean unreadOnly,
        NotificationTypeEnum type,
        LocalDateTime startDate,
        LocalDateTime endDate
    );
    
    /**
     * Lấy chi tiết một notification
     */
    NotificationResponse getNotificationById(UUID notificationId, UUID userId);
    
    /**
     * Lấy thống kê notifications của user
     */
    NotificationStatsResponse getNotificationStats(UUID userId);
    
    /**
     * Mark notification as read
     */
    void markAsRead(UUID notificationId, UUID userId);
    
    /**
     * Mark all notifications as read
     */
    void markAllAsRead(UUID userId);
    
    /**
     * Xóa notification
     */
    void deleteNotification(UUID notificationId, UUID userId);
    
    /**
     * Cleanup old notifications (scheduled job)
     */
    void cleanupOldNotifications(int olderThanDays);
}
