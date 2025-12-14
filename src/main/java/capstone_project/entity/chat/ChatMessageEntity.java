package capstone_project.entity.chat;

import capstone_project.common.enums.chat.ChatMessageTypeEnum;
import capstone_project.common.enums.chat.ChatParticipantTypeEnum;
import capstone_project.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import capstone_project.common.utils.VietnamTimeUtils;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a single chat message
 */
@Entity
@Table(name = "chat_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private ChatConversationEntity conversation;

    // Sender info
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id")
    private UserEntity sender;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 20)
    private ChatParticipantTypeEnum senderType;

    @Column(name = "sender_name", length = 100)
    private String senderName;

    // Message content
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 20)
    @Builder.Default
    private ChatMessageTypeEnum messageType = ChatMessageTypeEnum.TEXT;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    // Read status
    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "read_by_staff_id")
    private UserEntity readByStaff;

    // Timestamps
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = VietnamTimeUtils.now();
        }
    }
}
