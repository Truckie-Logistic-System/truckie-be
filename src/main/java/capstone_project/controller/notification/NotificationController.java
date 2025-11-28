package capstone_project.controller.notification;

import capstone_project.common.enums.NotificationTypeEnum;
import capstone_project.dtos.request.notification.GeneralNotificationMessageRequest;
import capstone_project.dtos.request.notification.NotificationMessageRequest;
import capstone_project.dtos.response.notification.NotificationResponse;
import capstone_project.dtos.response.notification.NotificationStatsResponse;
import capstone_project.common.utils.UserContextUtils;
import capstone_project.service.services.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("${notification.api.base-path}")
@RequiredArgsConstructor
//@PreAuthorize("isAuthenticated()")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserContextUtils userContextUtils;

    // ============= Legacy WebSocket Endpoints =============
    
    @PostMapping("/broadcast")
    public ResponseEntity<Void> broadcast(@RequestBody GeneralNotificationMessageRequest message) {
        notificationService.sendToAll(message);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/user/{userId}")
    public ResponseEntity<Void> sendToUser(@PathVariable String userId,
                                           @RequestBody NotificationMessageRequest message) {
        notificationService.sendToUser(userId, message);
        return ResponseEntity.ok().build();
    }
    
    // ============= New Persistent Notification Endpoints =============
    
    /**
     * Lấy danh sách notifications của user hiện tại
     * GET /api/notifications?page=0&size=20&unreadOnly=false&type=ORDER_CREATED
     */
    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) Boolean unreadOnly,
        @RequestParam(required = false) NotificationTypeEnum type,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate
    ) {
        // CRITICAL FIX: Use role-specific ID based on user role
        // Notifications are stored with user_id from UserEntity table
        UUID userId = userContextUtils.getCurrentUserId();
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        
        Page<NotificationResponse> notifications = notificationService.getUserNotifications(
            userId, pageable, unreadOnly, type, startDate, endDate
        );
        
        return ResponseEntity.ok(notifications);
    }
    
    /**
     * Lấy chi tiết một notification
     * GET /api/notifications/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<NotificationResponse> getNotificationById(
        @PathVariable UUID id
    ) {
        UUID userId = userContextUtils.getCurrentUserId();
        NotificationResponse notification = notificationService.getNotificationById(id, userId);
        return ResponseEntity.ok(notification);
    }
    
    /**
     * Lấy thống kê notifications
     * GET /api/notifications/stats
     */
    @GetMapping("/stats")
    public ResponseEntity<NotificationStatsResponse> getStats() {
        UUID userId = userContextUtils.getCurrentUserId();
        NotificationStatsResponse stats = notificationService.getNotificationStats(userId);
        return ResponseEntity.ok(stats);
    }
    
    /**
     * Mark notification as read
     * PUT /api/notifications/{id}/read
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(
        @PathVariable UUID id
    ) {
        UUID userId = userContextUtils.getCurrentUserId();
        notificationService.markAsRead(id, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Mark all notifications as read
     * PUT /api/notifications/read-all
     */
    @PutMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        UUID userId = userContextUtils.getCurrentUserId();
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * Xóa notification
     * DELETE /api/notifications/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotification(
        @PathVariable UUID id
    ) {
        UUID userId = userContextUtils.getCurrentUserId();
        notificationService.deleteNotification(id, userId);
        return ResponseEntity.ok().build();
    }
    
    // ============= Helper Methods =============
    // Using UserContextUtils.getCurrentUserId() for all roles
    // Notifications are stored with user_id from UserEntity table, not role-specific table IDs
}
