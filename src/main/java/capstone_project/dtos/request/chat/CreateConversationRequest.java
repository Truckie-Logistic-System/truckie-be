package capstone_project.dtos.request.chat;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Request DTO for creating a new chat conversation
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {
    
    // For logged-in users, this will be set from JWT token
    private UUID userId;
    
    // For guest users
    private String guestSessionId;
    private String guestName;
    
    // Optional context - linked order
    private UUID orderId;
    
    // Optional context - linked vehicle assignment (for drivers)
    private UUID vehicleAssignmentId;
    
    // Initial message (optional)
    private String initialMessage;
}
