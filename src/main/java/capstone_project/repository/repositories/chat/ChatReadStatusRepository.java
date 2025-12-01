package capstone_project.repository.repositories.chat;

import capstone_project.entity.chat.ChatReadStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for chat read status tracking
 */
@Repository
public interface ChatReadStatusRepository extends JpaRepository<ChatReadStatusEntity, UUID> {
    
    /**
     * Check if staff has read a specific message
     */
    boolean existsByMessageIdAndStaffId(UUID messageId, UUID staffId);
    
    /**
     * Find read status for a message by staff
     */
    Optional<ChatReadStatusEntity> findByMessageIdAndStaffId(UUID messageId, UUID staffId);
    
    /**
     * Find all staff who read a message
     */
    List<ChatReadStatusEntity> findByMessageId(UUID messageId);
    
    /**
     * Find all messages read by a staff member
     */
    List<ChatReadStatusEntity> findByStaffId(UUID staffId);
    
    /**
     * Count how many staff have read a message
     */
    long countByMessageId(UUID messageId);
    
    /**
     * Get unread message IDs for a staff in a conversation
     */
    @Query("SELECT m.id FROM ChatMessageEntity m WHERE m.conversation.id = :conversationId " +
           "AND m.id NOT IN (SELECT rs.message.id FROM ChatReadStatusEntity rs WHERE rs.staff.id = :staffId)")
    List<UUID> findUnreadMessageIds(@Param("conversationId") UUID conversationId, @Param("staffId") UUID staffId);
}
