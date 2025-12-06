package capstone_project.entity.chat;

import capstone_project.common.enums.chat.ConversationStatusEnum;
import capstone_project.common.enums.chat.ConversationTypeEnum;
import capstone_project.common.enums.chat.ChatParticipantTypeEnum;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing a chat conversation between users and staff
 */
@Entity
@Table(name = "chat_conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "conversation_type", nullable = false, length = 30)
    private ConversationTypeEnum conversationType;

    // Initiator - who started the conversation
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "initiator_id")
    private UserEntity initiator;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiator_type", nullable = false, length = 20)
    private ChatParticipantTypeEnum initiatorType;

    // Guest-specific fields
    @Column(name = "guest_session_id", length = 100)
    private String guestSessionId;

    @Column(name = "guest_name", length = 100)
    private String guestName;

    // Context - linked order or vehicle assignment
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_order_id")
    private OrderEntity currentOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_vehicle_assignment_id")
    private VehicleAssignmentEntity currentVehicleAssignment;

    // Status
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ConversationStatusEnum status = ConversationStatusEnum.ACTIVE;

    // Last activity tracking
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

    @Column(name = "last_message_preview", length = 200)
    private String lastMessagePreview;

    @Column(name = "unread_count")
    @Builder.Default
    private Integer unreadCount = 0;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "closed_by")
    private UserEntity closedBy;

    // Messages in this conversation
    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ChatMessageEntity> messages = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (updatedAt == null) {
            updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
