package capstone_project.entity;

import capstone_project.common.enums.NotificationTypeEnum;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.common.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity lưu trữ thông báo cho user
 * Hỗ trợ 3 roles: CUSTOMER, STAFF, DRIVER
 * 
 * Features:
 * - Lưu trữ persistent notification list
 * - Track read status và email/push notification status
 * - Reference đến các entities liên quan (Order, Issue, Contract, etc.)
 * - Metadata JSON cho flexible additional data
 */
@Entity
@Table(name = "notifications", 
       schema = "public", 
       catalog = "capstone-project",
       indexes = {
    @Index(name = "idx_user_created", columnList = "user_id, created_at DESC"),
    @Index(name = "idx_user_read_created", columnList = "user_id, is_read, created_at DESC"),
    @Index(name = "idx_type_created", columnList = "notification_type, created_at DESC"),
    @Index(name = "idx_order_id", columnList = "related_order_id"),
    @Index(name = "idx_issue_id", columnList = "related_issue_id")
})
@Data
@lombok.EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEntity extends BaseEntity {
    
    // User relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;
    
    // Role-based notification (CUSTOMER, STAFF, DRIVER)
    @Basic
    @Column(name = "recipient_role", nullable = false, length = 50)
    private String recipientRole;
    
    // Content
    @Basic
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Basic
    @Column(name = "description", nullable = false, length = 2000)
    private String description;
    
    // Notification type
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationTypeEnum notificationType;
    
    // Related entities (nullable, depending on notification type)
    @Basic
    @Column(name = "related_order_id")
    private UUID relatedOrderId;
    
    /**
     * JSON array of OrderDetail UUIDs
     * Example: ["550e8400-e29b-41d4-a716-446655440000", "550e8400-e29b-41d4-a716-446655440001"]
     */
    @Basic
    @Column(name = "related_order_detail_ids", columnDefinition = "TEXT")
    private String relatedOrderDetailIds;
    
    @Basic
    @Column(name = "related_issue_id")
    private UUID relatedIssueId;
    
    @Basic
    @Column(name = "related_vehicle_assignment_id")
    private UUID relatedVehicleAssignmentId;
    
    @Basic
    @Column(name = "related_contract_id")
    private UUID relatedContractId;
    
    /**
     * Metadata JSON for flexible additional data
     * Examples:
     * - Order info: {"orderCode": "ORD-001", "packageCount": 3}
     * - Driver info: {"driverName": "Nguyễn Văn A", "vehiclePlate": "29C-12345"}
     * - Payment info: {"amount": "5000000", "deadline": "2024-11-25T23:59:59"}
     */
    @Basic
    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata;
    
    // Status
    @Basic
    @Column(name = "is_read", nullable = false)
    @lombok.Builder.Default
    private boolean isRead = false;
    
    @Basic
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    // Email tracking (for customers)
    @Basic
    @Column(name = "email_sent")
    @lombok.Builder.Default
    private boolean emailSent = false;
    
    @Basic
    @Column(name = "email_sent_at")
    private LocalDateTime emailSentAt;
    
    // Push notification tracking (for drivers)
    @Basic
    @Column(name = "push_notification_sent")
    @lombok.Builder.Default
    private boolean pushNotificationSent = false;
    
    @Basic
    @Column(name = "push_notification_sent_at")
    private LocalDateTime pushNotificationSentAt;
    
    // Note: createdAt and updatedAt are inherited from BaseEntity
    // Do not redeclare them here to avoid conflicts
}
