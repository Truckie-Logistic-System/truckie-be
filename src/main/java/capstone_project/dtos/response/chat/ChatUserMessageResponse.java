package capstone_project.dtos.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for user-to-user chat message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatUserMessageResponse {
    
    private UUID id;
    private UUID conversationId;
    
    // Sender info
    private UUID senderId;
    private String senderType;
    private String senderName;
    private String senderImageUrl;
    
    // Message content
    private String content;
    private String messageType;
    private String imageUrl;
    
    // Read status
    private Boolean isRead;
    private LocalDateTime readAt;
    
    // Timestamp
    private LocalDateTime createdAt;
}
