package capstone_project.dtos.request.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for sending a chat message
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SendMessageRequest {
    
    @NotNull(message = "Conversation ID is required")
    private UUID conversationId;
    
    // For logged-in users, set from JWT token
    private UUID senderId;
    
    // For guest users
    private String guestSessionId;
    private String senderName;
    
    @NotBlank(message = "Message content is required")
    private String content;
    
    // Message type: TEXT, IMAGE
    private String messageType;
    
    // For image messages
    private String imageUrl;
}
