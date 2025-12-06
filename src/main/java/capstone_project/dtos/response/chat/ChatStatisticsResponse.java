package capstone_project.dtos.response.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for chat statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatStatisticsResponse {
    
    private int totalActiveConversations;
    private int customerSupportCount;
    private int driverSupportCount;
    private int guestSupportCount;
    private int totalUnreadMessages;
    private int closedToday;
    private double averageResponseTimeMinutes;
}
