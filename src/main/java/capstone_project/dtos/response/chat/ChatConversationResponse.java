package capstone_project.dtos.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for chat conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatConversationResponse {
    
    private UUID id;
    private String conversationType;
    
    // Initiator info
    private UUID initiatorId;
    private String initiatorType;
    private String initiatorName;
    private String initiatorImageUrl;
    
    // Guest info
    private String guestSessionId;
    private String guestName;
    
    // Context info
    private UUID currentOrderId;
    private String currentOrderCode;
    private UUID currentVehicleAssignmentId;
    private String currentTrackingCode;
    
    // Status
    private String status;
    private Integer unreadCount;
    
    // Last message info
    private LocalDateTime lastMessageAt;
    private String lastMessagePreview;
    private String lastMessageSenderType;
    
    // Timestamps
    private LocalDateTime createdAt;
    private LocalDateTime closedAt;
    
    // Active orders for context (for staff view)
    private java.util.List<ActiveOrderInfo> activeOrders;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActiveOrderInfo {
        private UUID orderId;
        private String orderCode;
        private String status;
        private String receiverName;
        private LocalDateTime createdAt;
        private LocalDateTime modifiedAt;
    }
}
