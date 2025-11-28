package capstone_project.service.services.notification;

import capstone_project.dtos.response.notification.NotificationResponse;
import capstone_project.entity.NotificationEntity;
import capstone_project.service.services.notification.impl.NotificationServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Event listener for notification events
 * Separated from NotificationServiceImpl to avoid self-invocation issues
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventListener {

    private final NotificationServiceImpl notificationService;

    /**
     * Handle WebSocket broadcast after transaction commits
     * This ensures notification is persisted before broadcasting
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleNotificationCreated(NotificationServiceImpl.NotificationCreatedEvent event) {
        log.info("🎯 NotificationCreatedEvent received in event listener");
        NotificationEntity notification = event.getNotification();
        log.info("📢 Transaction committed, broadcasting WebSocket notification: {}", notification.getId());
        
        try {
            NotificationResponse response = notificationService.toResponse(notification);
            notificationService.sendWebSocketNotification(notification.getUser().getId().toString(), response);
            log.info("✅ WebSocket broadcast completed successfully");
        } catch (Exception e) {
            log.error("❌ Failed to broadcast WebSocket notification", e);
        }
    }
}
