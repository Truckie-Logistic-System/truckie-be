package capstone_project.service.services.notification.impl;

import capstone_project.common.enums.NotificationTypeEnum;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
import capstone_project.dtos.request.notification.GeneralNotificationMessageRequest;
import capstone_project.dtos.request.notification.NotificationMessageRequest;
import capstone_project.dtos.response.notification.NotificationResponse;
import capstone_project.dtos.response.notification.NotificationStatsResponse;
import capstone_project.entity.NotificationEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.NotificationRepository;
import capstone_project.repository.repositories.auth.UserRepository;
import capstone_project.repository.repositories.user.DriverRepository;
import capstone_project.service.services.notification.NotificationService;
import capstone_project.service.services.email.EmailNotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    // Event class for notification creation
    public static class NotificationCreatedEvent {
        private final NotificationEntity notification;
        
        public NotificationCreatedEvent(NotificationEntity notification) {
            this.notification = notification;
        }
        
        public NotificationEntity getNotification() {
            return notification;
        }
    }

    private final SimpMessagingTemplate messagingTemplate;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final ObjectMapper objectMapper;
    private final EmailNotificationService emailNotificationService;
    private final capstone_project.service.services.websocket.NotificationWebSocketService notificationWebSocketService;
    private final ApplicationEventPublisher applicationEventPublisher;

    // ============= Legacy WebSocket Methods =============
    
    @Override
    public void sendToUser(String userId, NotificationMessageRequest message) {
        messagingTemplate.convertAndSend("/queue/notifications/" + userId, message);
    }

    @Override
    public void sendToAll(GeneralNotificationMessageRequest message) {
        messagingTemplate.convertAndSend("/topic/notifications", message);
    }
    
    // ============= New Persistent Notification Methods =============

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public NotificationResponse createNotification(CreateNotificationRequest request) {
        log.info(" Creating notification for user: {}, type: {}", request.getUserId(), request.getNotificationType());
        
        try {
            // Validate user exists
            UserEntity user = userRepository.findById(request.getUserId())
                .orElse(null);
            
            if (user == null) {
                log.warn(" User not found for notification: {} - skipping notification creation", request.getUserId());
                return null;
            }

            // Special case: DRIVER_CREATED for DRIVER should send email only (no in-app notification / WebSocket)
            if (NotificationTypeEnum.DRIVER_CREATED.equals(request.getNotificationType())
                    && "DRIVER".equals(request.getRecipientRole())) {
                log.info("Sending DRIVER_CREATED email without creating persistent notification for user: {}", user.getId());

                NotificationEntity tempNotification = NotificationEntity.builder()
                        .user(user)
                        .recipientRole(request.getRecipientRole())
                        .title(request.getTitle())
                        .description(request.getDescription())
                        .notificationType(request.getNotificationType())
                        .createdAt(LocalDateTime.now())
                        .build();

                // Directly send email using the email service; no DB record, no WebSocket
                emailNotificationService.sendNotificationEmail(tempNotification, user);

                return null;
            }
        
            // Convert related order detail IDs to JSON string
            String orderDetailIdsJson = null;
            if (request.getRelatedOrderDetailIds() != null && !request.getRelatedOrderDetailIds().isEmpty()) {
                orderDetailIdsJson = objectMapper.writeValueAsString(request.getRelatedOrderDetailIds());
            }
            
            // Convert metadata to JSON string
            String metadataJson = null;
            if (request.getMetadata() != null && !request.getMetadata().isEmpty()) {
                metadataJson = objectMapper.writeValueAsString(request.getMetadata());
            }
            
            // Create notification entity
            NotificationEntity notification = NotificationEntity.builder()
                .user(user)
                .recipientRole(request.getRecipientRole())
                .title(request.getTitle())
                .description(request.getDescription())
                .notificationType(request.getNotificationType())
                .relatedOrderId(request.getRelatedOrderId())
                .relatedOrderDetailIds(orderDetailIdsJson)
                .relatedIssueId(request.getRelatedIssueId())
                .relatedVehicleAssignmentId(request.getRelatedVehicleAssignmentId())
                .relatedContractId(request.getRelatedContractId())
                .metadata(metadataJson)
                .isRead(false)
                .emailSent(false)
                .pushNotificationSent(false)
                .build();
            
            // Save to database
            notification = notificationRepository.save(notification);
            log.info("✅ Notification saved to database: {}", notification.getId());
            
            // Convert to response
            NotificationResponse response = toResponse(notification);
            
            // Publish event for WebSocket broadcast after transaction commit
            log.info("📤 Publishing NotificationCreatedEvent for notification: {}", notification.getId());
            applicationEventPublisher.publishEvent(new NotificationCreatedEvent(notification));
            log.info("✅ NotificationCreatedEvent published successfully");
            
            // Send email if customer AND notification requires email, OR if driver account created
            if (("CUSTOMER".equals(request.getRecipientRole()) && shouldSendEmail(request.getNotificationType(), request.getMetadata())) ||
                ("DRIVER".equals(request.getRecipientRole()) && NotificationTypeEnum.DRIVER_CREATED.equals(request.getNotificationType()))) {
                sendEmailNotificationAsync(notification, user);
            }
            
            // FCM push notification removed
            
            return response;
            
        } catch (JsonProcessingException e) {
            log.error("❌ Error converting to JSON for notification: {} - skipping", request.getNotificationType(), e);
            return null;
        } catch (Exception e) {
            log.error("❌ Unexpected error creating notification: {} - skipping", request.getNotificationType(), e);
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationResponse> getUserNotifications(
        UUID userId, 
        Pageable pageable,
        Boolean unreadOnly,
        NotificationTypeEnum type,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        log.info("📋 Getting notifications for user: {}, unreadOnly: {}, type: {}", userId, unreadOnly, type);
        
        Page<NotificationEntity> notifications;
        
        if (startDate != null && endDate != null) {
            // Filter by date range
            notifications = notificationRepository.findByUserIdAndDateRange(userId, startDate, endDate, pageable);
        } else if (type != null) {
            // Filter by type
            notifications = notificationRepository.findByUserIdAndType(userId, type, pageable);
        } else if (Boolean.TRUE.equals(unreadOnly)) {
            // Only unread
            notifications = notificationRepository.findUnreadByUserId(userId, pageable);
        } else {
            // All notifications
            notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        }
        
        return notifications.map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationResponse getNotificationById(UUID notificationId, UUID userId) {
        log.info("📄 Getting notification: {} for user: {}", notificationId, userId);
        
        NotificationEntity notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        // Verify ownership
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("User " + userId + " does not own notification " + notificationId);
        }
        
        return toResponse(notification);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationStatsResponse getNotificationStats(UUID userId) {
        log.info("📊 Getting notification stats for user: {}", userId);
        
        // Get total count for this user
        long totalCount = notificationRepository.countByUserId(userId);
        
        // Get unread count
        long unreadCount = notificationRepository.countUnreadByUserId(userId);
        
        // Get count by type
        List<Object[]> typeCountResults = notificationRepository.countByUserIdGroupByType(userId);
        Map<NotificationTypeEnum, Long> countByType = typeCountResults.stream()
            .collect(Collectors.toMap(
                result -> (NotificationTypeEnum) result[0],
                result -> (Long) result[1]
            ));
        
        return NotificationStatsResponse.builder()
            .totalCount(totalCount)
            .unreadCount(unreadCount)
            .readCount(totalCount - unreadCount)
            .countByType(countByType)
            .build();
    }

    @Override
    @Transactional
    public void markAsRead(UUID notificationId, UUID userId) {
        log.info("✓ Marking notification as read: {} for user: {}", notificationId, userId);
        
        int updated = notificationRepository.markAsRead(notificationId, userId, LocalDateTime.now());
        
        if (updated == 0) {
            throw new RuntimeException("Failed to mark notification as read: " + notificationId);
        }
    }

    @Override
    @Transactional
    public void markAllAsRead(UUID userId) {
        log.info("✓✓ Marking all notifications as read for user: {}", userId);
        
        int updated = notificationRepository.markAllAsRead(userId, LocalDateTime.now());
        
        log.info("✅ Marked {} notifications as read", updated);
    }

    @Override
    @Transactional
    public void deleteNotification(UUID notificationId, UUID userId) {
        log.info("🗑️ Deleting notification: {} for user: {}", notificationId, userId);
        
        NotificationEntity notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new RuntimeException("Notification not found: " + notificationId));
        
        // Verify ownership
        if (!notification.getUser().getId().equals(userId)) {
            throw new RuntimeException("User " + userId + " does not own notification " + notificationId);
        }
        
        notificationRepository.delete(notification);
        log.info("✅ Notification deleted: {}", notificationId);
    }

    @Override
    @Transactional
    public void cleanupOldNotifications(int olderThanDays) {
        log.info("🧹 Cleaning up notifications older than {} days", olderThanDays);
        
        LocalDateTime beforeDate = LocalDateTime.now().minusDays(olderThanDays);
        int deleted = notificationRepository.deleteOldNotifications(beforeDate);
        
        log.info("✅ Deleted {} old notifications", deleted);
    }
    
    // ============= Helper Methods =============
    
    /**
     * Convert entity to response DTO
     */
    public NotificationResponse toResponse(NotificationEntity entity) {
        // Parse order detail IDs from JSON
        List<UUID> orderDetailIds = null;
        if (entity.getRelatedOrderDetailIds() != null) {
            try {
                orderDetailIds = objectMapper.readValue(
                    entity.getRelatedOrderDetailIds(), 
                    new TypeReference<List<UUID>>() {}
                );
            } catch (JsonProcessingException e) {
                log.warn("⚠️ Failed to parse orderDetailIds JSON for notification: {} - using null", entity.getId());
            }
        }
        
        // Parse metadata from JSON
        Map<String, Object> metadata = null;
        if (entity.getMetadata() != null) {
            try {
                metadata = objectMapper.readValue(
                    entity.getMetadata(), 
                    new TypeReference<Map<String, Object>>() {}
                );
            } catch (JsonProcessingException e) {
                log.warn("⚠️ Failed to parse metadata JSON for notification: {} - using null", entity.getId());
            }
        }
        
        return NotificationResponse.builder()
            .id(entity.getId())
            .recipientRole(entity.getRecipientRole())
            .title(entity.getTitle())
            .description(entity.getDescription())
            .notificationType(entity.getNotificationType())
            .relatedOrderId(entity.getRelatedOrderId())
            .relatedOrderDetailIds(orderDetailIds)
            .relatedIssueId(entity.getRelatedIssueId())
            .relatedVehicleAssignmentId(entity.getRelatedVehicleAssignmentId())
            .relatedContractId(entity.getRelatedContractId())
            .metadata(metadata)
            .isRead(entity.isRead())
            .readAt(entity.getReadAt())
            .emailSent(entity.isEmailSent())
            .emailSentAt(entity.getEmailSentAt())
            .pushNotificationSent(entity.isPushNotificationSent())
            .pushNotificationSentAt(entity.getPushNotificationSentAt())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getModifiedAt())
            .build();
    }
    private String determinePriority(NotificationTypeEnum type) {
        return switch (type) {
            case RETURN_PAYMENT_SUCCESS, DAMAGE_RESOLVED, ORDER_REJECTION_RESOLVED,
                 CONTRACT_SIGN_OVERDUE -> "LOW";
            default -> "MEDIUM";
        };
    }
    
    /**
     * Determine if notification should trigger email to customer.
     * Only send email for notifications that require ACTION from customer or important milestones.
     * 
     * Email is sent for:
     * - ORDER_CREATED: Xác nhận đơn hàng
     * - CONTRACT_READY: ACTION - Ký hợp đồng + Thanh toán cọc
     * - DRIVER_ASSIGNED: ACTION - Thanh toán toàn bộ
     * - PICKING_UP_STARTED: ACTION - Vào web xem live tracking
     * - RETURN_STARTED: ACTION - Thanh toán cước trả hàng
     * - RETURN_PAYMENT_REQUIRED: ACTION - Thanh toán cước trả hàng
     * - PAYMENT_REMINDER: ACTION - Thanh toán trước deadline
     * - CONTRACT_SIGN_REMINDER: ACTION - Ký hợp đồng trước deadline
     * - DELIVERY_COMPLETED: Only when ALL packages delivered (check metadata.allPackagesDelivered)
     * - RETURN_COMPLETED: Only when ALL packages returned (check metadata.allPackagesReturned)
     * - ORDER_CANCELLED: Important milestone
     * - COMPENSATION_PROCESSED: Refund information
     * - PACKAGE_DAMAGED: Issue awareness
     * - ORDER_REJECTED_BY_RECEIVER: Issue awareness
     */
    private boolean shouldSendEmail(NotificationTypeEnum type, Map<String, Object> metadata) {
        return switch (type) {
            // ACTION Required - Always send email
            case ORDER_CREATED,
                 CONTRACT_READY,
                 DRIVER_ASSIGNED,
                 PICKING_UP_STARTED,
                 RETURN_STARTED,
                 RETURN_PAYMENT_REQUIRED,
                 PAYMENT_REMINDER,
                 CONTRACT_SIGN_REMINDER,
                 PAYMENT_OVERDUE,
                 CONTRACT_SIGN_OVERDUE,
                 SEAL_ASSIGNED,
                 SEAL_REPLACEMENT_COMPLETED -> true;
            
            // Important Issues - Always send email
            case PACKAGE_DAMAGED,
                 ORDER_REJECTED_BY_RECEIVER,
                 COMPENSATION_PROCESSED,
                 ORDER_CANCELLED -> true;
            
            // Delivery / Return completed - send email whenever there is at least one affected package
            case DELIVERY_COMPLETED -> true;
            case RETURN_COMPLETED -> true;
            case CUSTOMER_RETURN_IN_PROGRESS -> true;
            
            // Staff/Driver notifications - No email (they use in-app only)
            default -> false;
        };
    }
    
    /**
     * Send email notification async (customer only)
     * Updates email_sent and email_sent_at fields after successful send
     */
    @Async
    @Transactional
    protected void sendEmailNotificationAsync(NotificationEntity notification, UserEntity user) {
        try {
            log.info("📧 Sending email notification to customer: {}", user.getEmail());
            emailNotificationService.sendNotificationEmail(notification, user);
            
            // Update notification with email sent status
            notification.setEmailSent(true);
            notification.setEmailSentAt(java.time.LocalDateTime.now());
            notificationRepository.save(notification);
            
            log.info("✅ Email notification sent successfully to: {} and status updated", user.getEmail());
        } catch (Exception e) {
            log.error("❌ Failed to send email notification to {}: {}", user.getEmail(), e.getMessage(), e);
            // Don't throw exception to avoid breaking main notification flow
        }
    }
    
    
    /**
     * Send notification via WebSocket using NotificationWebSocketService
     * Uses shared vehicle-tracking-browser endpoint with proper topic routing:
     * - Customer/Staff: /topic/user/{userId}/notifications
     * - Driver: /topic/driver/{driverId}/notifications
     */
    public void sendWebSocketNotification(String userId, NotificationResponse notification) {
        try {
            // Use driver-specific topic for driver notifications
            if ("DRIVER".equalsIgnoreCase(notification.getRecipientRole())) {
                // For drivers, use driver ID (not user ID) to match mobile app subscription
                // Mobile subscribes to /topic/driver/{DRIVER_ID}/notifications
                String driverId = getDriverIdFromUserId(userId);
                notificationWebSocketService.sendDriverNotificationUpdateSignal(
                    UUID.fromString(driverId),
                    "NEW"
                );
                
                log.info("📡 WebSocket notification sent to driver topic: {} (type: {})", 
                        driverId, notification.getNotificationType());
            } else {
                // For customers and staff, use user-specific topic
                notificationWebSocketService.sendNotificationUpdateSignal(
                    UUID.fromString(userId),
                    "NEW"
                );
                
                log.info("📡 WebSocket notification sent to user topic: {} (type: {})", 
                        userId, notification.getNotificationType());
            }
                    
        } catch (Exception e) {
            log.error("❌ Failed to send WebSocket notification: userId={}, role={}", 
                    userId, notification.getRecipientRole(), e);
        }
    }
    
    /**
     * Get driver ID from user ID for driver notifications
     * This maps the user ID to the corresponding driver entity ID
     */
    private String getDriverIdFromUserId(String userId) {
        try {
            // Find driver entity by user ID
            DriverEntity driver = driverRepository.findByUserId(UUID.fromString(userId))
                .orElseThrow(() -> new RuntimeException("Driver not found for user ID: " + userId));
            
            log.debug("🔗 Mapped user ID {} to driver ID {}", userId, driver.getId());
            return driver.getId().toString();
            
        } catch (Exception e) {
            log.error("❌ Failed to map user ID to driver ID: {}", userId, e);
            // Fallback to user ID if driver mapping fails
            return userId;
        }
    }
}
