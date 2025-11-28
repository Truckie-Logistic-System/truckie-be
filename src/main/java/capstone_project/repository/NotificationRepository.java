package capstone_project.repository;

import capstone_project.common.enums.NotificationTypeEnum;
import capstone_project.entity.NotificationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, UUID> {
    
    /**
     * Lấy danh sách notifications của user với pagination
     * Sắp xếp theo thời gian tạo mới nhất
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findByUserIdOrderByCreatedAtDesc(
        @Param("userId") UUID userId, 
        Pageable pageable
    );
    
    /**
     * Lấy danh sách notifications chưa đọc của user
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findUnreadByUserId(
        @Param("userId") UUID userId, 
        Pageable pageable
    );
    
    /**
     * Lấy danh sách notifications theo type
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId " +
           "AND n.notificationType = :type " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findByUserIdAndType(
        @Param("userId") UUID userId,
        @Param("type") NotificationTypeEnum type,
        Pageable pageable
    );
    
    /**
     * Lấy danh sách notifications trong khoảng thời gian
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.user.id = :userId " +
           "AND n.createdAt BETWEEN :startDate AND :endDate " +
           "ORDER BY n.createdAt DESC")
    Page<NotificationEntity> findByUserIdAndDateRange(
        @Param("userId") UUID userId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        Pageable pageable
    );
    
    /**
     * Đếm tổng số notifications của user
     */
    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
    
    /**
     * Đếm số lượng notifications chưa đọc của user
     */
    @Query("SELECT COUNT(n) FROM NotificationEntity n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") UUID userId);
    
    /**
     * Đếm số lượng notifications theo type
     */
    @Query("SELECT n.notificationType, COUNT(n) FROM NotificationEntity n " +
           "WHERE n.user.id = :userId " +
           "GROUP BY n.notificationType")
    List<Object[]> countByUserIdGroupByType(@Param("userId") UUID userId);
    
    /**
     * Mark notification as read
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.id = :notificationId AND n.user.id = :userId")
    int markAsRead(
        @Param("notificationId") UUID notificationId, 
        @Param("userId") UUID userId, 
        @Param("readAt") LocalDateTime readAt
    );
    
    /**
     * Mark all notifications as read for user
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = :readAt " +
           "WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsRead(
        @Param("userId") UUID userId, 
        @Param("readAt") LocalDateTime readAt
    );
    
    /**
     * Xóa notifications cũ hơn N ngày
     */
    @Modifying
    @Query("DELETE FROM NotificationEntity n WHERE n.createdAt < :beforeDate")
    int deleteOldNotifications(@Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Lấy notifications liên quan đến order
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.relatedOrderId = :orderId " +
           "ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByOrderId(@Param("orderId") UUID orderId);
    
    /**
     * Lấy notifications liên quan đến issue
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.relatedIssueId = :issueId " +
           "ORDER BY n.createdAt DESC")
    List<NotificationEntity> findByIssueId(@Param("issueId") UUID issueId);
    
    /**
     * Lấy notifications chưa gửi email (cho customer)
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.recipientRole = 'CUSTOMER' " +
           "AND n.emailSent = false " +
           "ORDER BY n.createdAt ASC")
    List<NotificationEntity> findPendingEmails();
    
    /**
     * Lấy notifications chưa gửi push (cho driver)
     */
    @Query("SELECT n FROM NotificationEntity n WHERE n.recipientRole = 'DRIVER' " +
           "AND n.pushNotificationSent = false " +
           "ORDER BY n.createdAt ASC")
    List<NotificationEntity> findPendingPushNotifications();
    
    /**
     * Update email sent status
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.emailSent = true, n.emailSentAt = :sentAt " +
           "WHERE n.id = :notificationId")
    int markEmailAsSent(
        @Param("notificationId") UUID notificationId,
        @Param("sentAt") LocalDateTime sentAt
    );
    
    /**
     * Update push notification sent status
     */
    @Modifying
    @Query("UPDATE NotificationEntity n SET n.pushNotificationSent = true, n.pushNotificationSentAt = :sentAt " +
           "WHERE n.id = :notificationId")
    int markPushNotificationAsSent(
        @Param("notificationId") UUID notificationId,
        @Param("sentAt") LocalDateTime sentAt
    );
}
