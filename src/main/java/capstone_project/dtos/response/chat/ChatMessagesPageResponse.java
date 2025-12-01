package capstone_project.dtos.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO for paginated chat messages
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessagesPageResponse {
    
    private List<ChatUserMessageResponse> messages;
    private UUID lastMessageId;
    private boolean hasMore;
    private int totalCount;
}
