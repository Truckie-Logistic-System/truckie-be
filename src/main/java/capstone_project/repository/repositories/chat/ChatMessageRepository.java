package capstone_project.repository.repositories.chat;

import capstone_project.entity.chat.ChatMessageEntity;
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

/**
 * Repository for chat messages
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {
    
    /**
     * Find messages by conversation with pagination (newest first)
     */
    Page<ChatMessageEntity> findByConversationIdOrderByCreatedAtDesc(UUID conversationId, Pageable pageable);
    
    /**
     * Find messages by conversation ordered by created time (oldest first for display)
     */
    List<ChatMessageEntity> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);
    
    /**
     * Find messages after a specific message (for real-time updates)
     */
    @Query("SELECT m FROM ChatMessageEntity m WHERE m.conversation.id = :conversationId AND m.createdAt > :afterTime ORDER BY m.createdAt ASC")
    List<ChatMessageEntity> findNewMessages(@Param("conversationId") UUID conversationId, @Param("afterTime") LocalDateTime afterTime);
    
    /**
     * Find unread messages in conversation
     */
    List<ChatMessageEntity> findByConversationIdAndIsReadFalseOrderByCreatedAtAsc(UUID conversationId);
    
    /**
     * Count unread messages in conversation
     */
    long countByConversationIdAndIsReadFalse(UUID conversationId);
    
    /**
     * Mark messages as read by staff (only non-staff messages)
     */
    @Modifying
    @Query("UPDATE ChatMessageEntity m SET m.isRead = true, m.readAt = :readAt, m.readByStaff.id = :staffId WHERE m.conversation.id = :conversationId AND m.isRead = false AND m.senderType != 'STAFF'")
    int markAsRead(
            @Param("conversationId") UUID conversationId,
            @Param("readAt") LocalDateTime readAt,
            @Param("staffId") UUID staffId
    );
    
    /**
     * Mark staff messages as read by customer
     */
    @Modifying
    @Query("UPDATE ChatMessageEntity m SET m.isRead = true, m.readAt = :readAt WHERE m.conversation.id = :conversationId AND m.senderType = 'STAFF' AND m.isRead = false")
    int markStaffMessagesAsReadForCustomer(
            @Param("conversationId") UUID conversationId,
            @Param("readAt") LocalDateTime readAt
    );
    
    /**
     * Mark staff messages as read by driver
     */
    @Modifying
    @Query("UPDATE ChatMessageEntity m SET m.isRead = true, m.readAt = :readAt WHERE m.conversation.id = :conversationId AND m.senderType = 'STAFF' AND m.isRead = false")
    int markStaffMessagesAsReadForDriver(
            @Param("conversationId") UUID conversationId,
            @Param("readAt") LocalDateTime readAt
    );
    
    /**
     * Count unread staff messages for customer/driver/guest
     * This is used to show badge count for non-staff users
     */
    @Query("SELECT COUNT(m) FROM ChatMessageEntity m WHERE m.conversation.id = :conversationId AND m.senderType = 'STAFF' AND m.isRead = false")
    int countUnreadStaffMessages(@Param("conversationId") UUID conversationId);
    
    /**
     * Get latest message of conversation
     */
    @Query("SELECT m FROM ChatMessageEntity m WHERE m.conversation.id = :conversationId ORDER BY m.createdAt DESC LIMIT 1")
    ChatMessageEntity findLatestMessage(@Param("conversationId") UUID conversationId);
    
    /**
     * Search messages by content
     */
    @Query("SELECT m FROM ChatMessageEntity m WHERE m.conversation.id = :conversationId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.createdAt DESC")
    List<ChatMessageEntity> searchByContent(@Param("conversationId") UUID conversationId, @Param("keyword") String keyword);
    
    /**
     * Count messages by sender type
     */
    @Query("SELECT m.senderType, COUNT(m) FROM ChatMessageEntity m WHERE m.conversation.id = :conversationId GROUP BY m.senderType")
    List<Object[]> countBySenderType(@Param("conversationId") UUID conversationId);
    
    /**
     * Find messages with images
     */
    List<ChatMessageEntity> findByConversationIdAndImageUrlIsNotNullOrderByCreatedAtDesc(UUID conversationId);
    
    /**
     * Delete old messages (for cleanup)
     */
    @Modifying
    @Query("DELETE FROM ChatMessageEntity m WHERE m.conversation.id = :conversationId AND m.createdAt < :beforeDate")
    int deleteOldMessages(@Param("conversationId") UUID conversationId, @Param("beforeDate") LocalDateTime beforeDate);
}
