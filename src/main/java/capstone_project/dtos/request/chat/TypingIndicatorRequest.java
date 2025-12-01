package capstone_project.dtos.request.chat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for typing indicators in chat
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypingIndicatorRequest {
    
    private String senderId;
    private String senderName;
    private String senderType;
    
    @JsonProperty("isTyping")
    private boolean isTyping;
}
