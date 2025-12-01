package capstone_project.controller.chat;

import capstone_project.dtos.request.chat.SendMessageRequest;
import capstone_project.dtos.request.chat.TypingIndicatorRequest;
import capstone_project.dtos.response.chat.*;
import capstone_project.service.services.chat.UserChatService;
import capstone_project.service.rateLimit.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * REST Controller for user-to-user chat functionality
 */
@RestController
@RequestMapping("${user-chat.api.base-path}")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "User Chat", description = "APIs for user-to-user chat functionality")
public class UserChatController {
    
    private final UserChatService userChatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final RateLimitService rateLimitService;
    
    // ==================== Customer Endpoints ====================
    
    @PostMapping("/customer/conversations")
    @Operation(summary = "Create or get customer conversation")
    public ResponseEntity<ChatConversationResponse> getOrCreateCustomerConversation(
            @RequestParam UUID customerId,
            @RequestParam(required = false) UUID orderId
    ) {
        log.info("Customer {} requesting conversation, orderId: {}", customerId, orderId);
        ChatConversationResponse response = userChatService.createOrGetCustomerConversation(customerId, orderId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/customer/conversations/{conversationId}/read")
    @Operation(summary = "Mark conversation messages as read for customer")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ResponseEntity<Void> markAsReadForCustomer(
            @PathVariable UUID conversationId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        // Extract JWT token and get user ID
        String token = authorizationHeader.substring(7); // Remove "Bearer " prefix
        UUID customerId = UUID.fromString(capstone_project.common.utils.JWTUtil.extractUserId(token));
        userChatService.markAsReadForCustomer(conversationId, customerId);
        return ResponseEntity.ok().build();
    }
    
    // ==================== Driver Endpoints ====================
    
    @PostMapping("/driver/conversations")
    @Operation(summary = "Create or get driver conversation")
    public ResponseEntity<ChatConversationResponse> getOrCreateDriverConversation(
            @RequestParam UUID driverId,
            @RequestParam(required = false) UUID vehicleAssignmentId
    ) {
        log.info("Driver {} requesting conversation, vehicleAssignmentId: {}", driverId, vehicleAssignmentId);
        ChatConversationResponse response = userChatService.createOrGetDriverConversation(driverId, vehicleAssignmentId);
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/driver/conversations/{conversationId}/read")
    @Operation(summary = "Mark conversation messages as read for driver")
    @PreAuthorize("hasRole('DRIVER')")
    public ResponseEntity<Void> markAsReadForDriver(
            @PathVariable UUID conversationId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        // Extract JWT token and get user ID
        String token = authorizationHeader.substring(7); // Remove "Bearer " prefix
        UUID driverId = UUID.fromString(capstone_project.common.utils.JWTUtil.extractUserId(token));
        userChatService.markAsReadForDriver(conversationId, driverId);
        return ResponseEntity.ok().build();
    }
    
    // ==================== Guest Endpoints ====================
    
    @PostMapping("/guest/conversations")
    @Operation(summary = "Create or get guest conversation")
    public ResponseEntity<ChatConversationResponse> getOrCreateGuestConversation(
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String guestName,
            HttpServletRequest request
    ) {
        String clientIp = rateLimitService.getClientIp(request);
        
        // Check if IP is banned
        if (rateLimitService.isIpBanned(clientIp)) {
            log.warn("Banned IP {} attempted to create guest conversation", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        // Check rate limit for guest conversation creation
        if (!rateLimitService.canCreateGuestConversation(clientIp)) {
            log.warn("Rate limit exceeded for guest conversation creation from IP: {}", clientIp);
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).build();
        }
        
        log.info("Guest requesting conversation, sessionId: {}, IP: {}", sessionId, clientIp);
        ChatConversationResponse response = userChatService.createOrGetGuestConversation(sessionId, guestName);
        return ResponseEntity.ok(response);
    }
    
    // ==================== Common Endpoints ====================
    
    @GetMapping("/conversations/{conversationId}")
    @Operation(summary = "Get conversation by ID")
    public ResponseEntity<ChatConversationResponse> getConversation(
            @PathVariable UUID conversationId
    ) {
        ChatConversationResponse response = userChatService.getConversation(conversationId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Get messages for a conversation with pagination")
    public ResponseEntity<ChatMessagesPageResponse> getMessages(
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        ChatMessagesPageResponse response = userChatService.getMessages(conversationId, page, size);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/conversations/{conversationId}/messages")
    @Operation(summary = "Send a message")
    public ResponseEntity<ChatUserMessageResponse> sendMessage(
            @PathVariable UUID conversationId,
            @RequestBody SendMessageRequest request
    ) {
        request.setConversationId(conversationId);
        ChatUserMessageResponse response = userChatService.sendMessage(request);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/upload-image")
    @Operation(summary = "Upload chat image")
    public ResponseEntity<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam UUID conversationId
    ) throws IOException {
        String imageUrl = userChatService.uploadChatImage(file, conversationId);
        return ResponseEntity.ok(imageUrl);
    }
    
    // ==================== Staff Endpoints ====================
    
    @GetMapping("/staff/conversations")
    @Operation(summary = "Get all active conversations for staff")
    public ResponseEntity<Page<ChatConversationResponse>> getStaffConversations(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<ChatConversationResponse> response = userChatService.getActiveConversations(type, PageRequest.of(page, size));
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/staff/conversations/unread")
    @Operation(summary = "Get conversations with unread messages")
    public ResponseEntity<List<ChatConversationResponse>> getUnreadConversations() {
        List<ChatConversationResponse> response = userChatService.getUnreadConversations();
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/staff/conversations/{conversationId}/read")
    @Operation(summary = "Mark conversation messages as read")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Void> markAsRead(
            @PathVariable UUID conversationId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        // Extract JWT token and get user ID
        String token = authorizationHeader.substring(7); // Remove "Bearer " prefix
        UUID staffId = UUID.fromString(capstone_project.common.utils.JWTUtil.extractUserId(token));
        userChatService.markAsRead(conversationId, staffId);
        return ResponseEntity.ok().build();
    }
    
    @PutMapping("/staff/conversations/{conversationId}/close")
    @Operation(summary = "Close a conversation")
    @PreAuthorize("hasRole('STAFF')")
    public ResponseEntity<Void> closeConversation(
            @PathVariable UUID conversationId,
            @RequestHeader("Authorization") String authorizationHeader
    ) {
        // Extract JWT token and get user ID
        String token = authorizationHeader.substring(7); // Remove "Bearer " prefix
        UUID staffId = UUID.fromString(capstone_project.common.utils.JWTUtil.extractUserId(token));
        userChatService.closeConversation(conversationId, staffId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/staff/conversations/search")
    @Operation(summary = "Search conversations by name")
    public ResponseEntity<List<ChatConversationResponse>> searchConversations(
            @RequestParam String keyword
    ) {
        List<ChatConversationResponse> response = userChatService.searchConversations(keyword);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/staff/messages/search")
    @Operation(summary = "Search messages in a conversation")
    public ResponseEntity<List<ChatUserMessageResponse>> searchMessages(
            @RequestParam UUID conversationId,
            @RequestParam String keyword
    ) {
        List<ChatUserMessageResponse> response = userChatService.searchMessages(conversationId, keyword);
        return ResponseEntity.ok(response);
    }
    
    // ==================== Overview Endpoints ====================
    
    @GetMapping("/staff/customer/{customerId}/overview")
    @Operation(summary = "Get customer overview for staff")
    public ResponseEntity<CustomerOverviewResponse> getCustomerOverview(
            @PathVariable UUID customerId
    ) {
        CustomerOverviewResponse response = userChatService.getCustomerOverview(customerId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/staff/driver/{driverId}/overview")
    @Operation(summary = "Get driver overview for staff")
    public ResponseEntity<DriverOverviewResponse> getDriverOverview(
            @PathVariable UUID driverId
    ) {
        DriverOverviewResponse response = userChatService.getDriverOverview(driverId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/staff/order/{orderId}/quick-view")
    @Operation(summary = "Get order quick view for staff")
    public ResponseEntity<OrderQuickViewResponse> getOrderQuickView(
            @PathVariable UUID orderId
    ) {
        OrderQuickViewResponse response = userChatService.getOrderQuickView(orderId);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/staff/vehicle-assignment/{vehicleAssignmentId}/quick-view")
    @Operation(summary = "Get vehicle assignment quick view for staff - comprehensive trip info with tabs")
    public ResponseEntity<VehicleAssignmentQuickViewResponse> getVehicleAssignmentQuickView(
            @PathVariable UUID vehicleAssignmentId
    ) {
        VehicleAssignmentQuickViewResponse response = userChatService.getVehicleAssignmentQuickView(vehicleAssignmentId);
        return ResponseEntity.ok(response);
    }
    
    // ==================== Statistics Endpoints ====================
    
    @GetMapping("/staff/statistics")
    @Operation(summary = "Get chat statistics")
    public ResponseEntity<ChatStatisticsResponse> getChatStatistics() {
        ChatStatisticsResponse response = userChatService.getChatStatistics();
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/staff/unread-count")
    @Operation(summary = "Get total unread message count")
    public ResponseEntity<Integer> getUnreadCount() {
        int count = userChatService.getUnreadCount();
        return ResponseEntity.ok(count);
    }
    
    // ==================== WebSocket Message Handlers ====================
    
    @MessageMapping("/user-chat.send/{conversationId}")
    public void handleChatMessage(
            @DestinationVariable String conversationId,
            @Payload SendMessageRequest request
    ) {
        log.info("WebSocket message received for conversation: {}", conversationId);
        request.setConversationId(UUID.fromString(conversationId));
        ChatUserMessageResponse response = userChatService.sendMessage(request);
        
        // Message is already broadcast by the service
        log.info("WebSocket message processed: {}", response.getId());
    }
    
    @MessageMapping("/user-chat.typing/{conversationId}")
    public void handleTypingIndicator(
            @DestinationVariable String conversationId,
            @Payload TypingIndicatorRequest request
    ) {
        log.info("Received typing indicator for conversation {}: senderId={}, senderName={}, isTyping={}", 
                conversationId, request.getSenderId(), request.getSenderName(), request.isTyping());
        
        // Broadcast typing indicator to conversation subscribers
        messagingTemplate.convertAndSend(
                "/topic/chat/conversation/" + conversationId + "/typing",
                request
        );
        
        log.info("Broadcasted typing indicator to /topic/chat/conversation/{}/typing", conversationId);
    }
}
