package capstone_project.repository.repositories.chat;

import capstone_project.common.enums.chat.ConversationStatusEnum;
import capstone_project.common.enums.chat.ConversationTypeEnum;
import capstone_project.entity.chat.ChatConversationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for chat conversations
 */
@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversationEntity, UUID> {
    
    /**
     * Find active conversation by initiator (customer/driver)
     */
    Optional<ChatConversationEntity> findByInitiatorIdAndStatusAndConversationType(
            UUID initiatorId,
            ConversationStatusEnum status,
            ConversationTypeEnum type
    );
    
    /**
     * Find active conversation by guest session
     */
    Optional<ChatConversationEntity> findByGuestSessionIdAndStatus(
            String guestSessionId,
            ConversationStatusEnum status
    );
    
    /**
     * Find all conversations for staff view (all types) - only those with messages
     */
    @Query("SELECT c FROM ChatConversationEntity c WHERE c.status = :status AND c.lastMessageAt IS NOT NULL ORDER BY c.lastMessageAt DESC")
    Page<ChatConversationEntity> findByStatusOrderByLastMessageAtDesc(
            @Param("status") ConversationStatusEnum status,
            Pageable pageable
    );
    
    /**
     * Find conversations by type for staff view - only those with messages
     */
    @Query("SELECT c FROM ChatConversationEntity c WHERE c.conversationType = :type AND c.status = :status AND c.lastMessageAt IS NOT NULL ORDER BY c.lastMessageAt DESC")
    Page<ChatConversationEntity> findByConversationTypeAndStatusOrderByLastMessageAtDesc(
            @Param("type") ConversationTypeEnum type,
            @Param("status") ConversationStatusEnum status,
            Pageable pageable
    );
    
    /**
     * Find all active conversations ordered by last message
     */
    @Query("SELECT c FROM ChatConversationEntity c WHERE c.status = :status ORDER BY c.lastMessageAt DESC NULLS LAST")
    List<ChatConversationEntity> findAllActiveOrderByLastMessageDesc(@Param("status") ConversationStatusEnum status);
    
    /**
     * Find conversations with unread messages
     */
    @Query("SELECT c FROM ChatConversationEntity c WHERE c.status = :status AND c.unreadCount > 0 ORDER BY c.lastMessageAt DESC")
    List<ChatConversationEntity> findWithUnreadMessages(@Param("status") ConversationStatusEnum status);
    
    /**
     * Count conversations by type and status
     */
    long countByConversationTypeAndStatus(ConversationTypeEnum type, ConversationStatusEnum status);
    
    /**
     * Count total unread messages across all conversations
     */
    @Query("SELECT COALESCE(SUM(c.unreadCount), 0) FROM ChatConversationEntity c WHERE c.status = :status")
    Integer countTotalUnreadMessages(@Param("status") ConversationStatusEnum status);
    
    /**
     * Update last message info
     */
    @Modifying
    @Query("UPDATE ChatConversationEntity c SET c.lastMessageAt = :lastMessageAt, c.lastMessagePreview = :preview, c.unreadCount = c.unreadCount + 1, c.updatedAt = :updatedAt WHERE c.id = :conversationId")
    void updateLastMessage(
            @Param("conversationId") UUID conversationId,
            @Param("lastMessageAt") LocalDateTime lastMessageAt,
            @Param("preview") String preview,
            @Param("updatedAt") LocalDateTime updatedAt
    );
    
    /**
     * Reset unread count
     */
    @Modifying
    @Query("UPDATE ChatConversationEntity c SET c.unreadCount = 0, c.updatedAt = :updatedAt WHERE c.id = :conversationId")
    void resetUnreadCount(@Param("conversationId") UUID conversationId, @Param("updatedAt") LocalDateTime updatedAt);
    
    /**
     * Close conversation
     */
    @Modifying
    @Query("UPDATE ChatConversationEntity c SET c.status = :status, c.closedAt = :closedAt, c.closedBy.id = :closedById, c.updatedAt = :updatedAt WHERE c.id = :conversationId")
    void closeConversation(
            @Param("conversationId") UUID conversationId,
            @Param("status") ConversationStatusEnum status,
            @Param("closedAt") LocalDateTime closedAt,
            @Param("closedById") UUID closedById,
            @Param("updatedAt") LocalDateTime updatedAt
    );
    
    /**
     * Find conversations by initiator (for history)
     */
    List<ChatConversationEntity> findByInitiatorIdOrderByCreatedAtDesc(UUID initiatorId);
    
    /**
     * Search conversations by initiator name or guest name
     */
    @Query("SELECT c FROM ChatConversationEntity c LEFT JOIN c.initiator u WHERE " +
           "(c.status = :status) AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(c.guestName) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "ORDER BY c.lastMessageAt DESC")
    List<ChatConversationEntity> searchByName(@Param("keyword") String keyword, @Param("status") ConversationStatusEnum status);
}
