package capstone_project.service.services.chat;

import capstone_project.dtos.request.chat.CreateConversationRequest;
import capstone_project.dtos.request.chat.SendMessageRequest;
import capstone_project.dtos.response.chat.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for user-to-user chat functionality
 */
public interface UserChatService {
    
    // ==================== Conversation Management ====================
    
    /**
     * Create or get existing conversation for customer
     */
    ChatConversationResponse createOrGetCustomerConversation(UUID customerId, UUID orderId);
    
    /**
     * Create or get existing conversation for driver
     */
    ChatConversationResponse createOrGetDriverConversation(UUID driverId, UUID vehicleAssignmentId);
    
    /**
     * Create or get existing conversation for guest
     */
    ChatConversationResponse createOrGetGuestConversation(String guestSessionId, String guestName);
    
    /**
     * Get conversation by ID
     */
    ChatConversationResponse getConversation(UUID conversationId);
    
    /**
     * Get all active conversations for staff
     */
    Page<ChatConversationResponse> getActiveConversations(String type, Pageable pageable);
    
    /**
     * Get conversations with unread messages
     */
    List<ChatConversationResponse> getUnreadConversations();
    
    /**
     * Close conversation
     */
    void closeConversation(UUID conversationId, UUID staffId);
    
    /**
     * Search conversations by name
     */
    List<ChatConversationResponse> searchConversations(String keyword);
    
    // ==================== Message Management ====================
    
    /**
     * Send message
     */
    ChatUserMessageResponse sendMessage(SendMessageRequest request);
    
    /**
     * Get messages for conversation with pagination
     */
    ChatMessagesPageResponse getMessages(UUID conversationId, int page, int size);
    
    /**
     * Mark messages as read
     */
    void markAsRead(UUID conversationId, UUID staffId);
    
    /**
     * Mark messages as read for customer
     */
    void markAsReadForCustomer(UUID conversationId, UUID customerId);
    
    /**
     * Mark messages as read for driver
     */
    void markAsReadForDriver(UUID conversationId, UUID driverId);
    
    /**
     * Upload chat image
     */
    String uploadChatImage(MultipartFile file, UUID conversationId) throws IOException;
    
    /**
     * Search messages in conversation
     */
    List<ChatUserMessageResponse> searchMessages(UUID conversationId, String keyword);
    
    // ==================== Overview Data ====================
    
    /**
     * Get customer overview for staff
     */
    CustomerOverviewResponse getCustomerOverview(UUID customerId);
    
    /**
     * Get driver overview for staff
     */
    DriverOverviewResponse getDriverOverview(UUID driverId);
    
    /**
     * Get order quick view
     */
    OrderQuickViewResponse getOrderQuickView(UUID orderId);
    
    /**
     * Get vehicle assignment quick view (comprehensive trip info for staff)
     */
    VehicleAssignmentQuickViewResponse getVehicleAssignmentQuickView(UUID vehicleAssignmentId);
    
    // ==================== Statistics ====================
    
    /**
     * Get chat statistics for dashboard
     */
    ChatStatisticsResponse getChatStatistics();
    
    /**
     * Get unread count for staff
     */
    int getUnreadCount();
}
