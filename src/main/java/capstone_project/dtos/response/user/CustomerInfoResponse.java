package capstone_project.dtos.response.user;

import java.util.UUID;

/**
 * Response DTO for customer contact information
 */
public record CustomerInfoResponse(
        UUID customerId,
        
        String fullName,
        
        String email,
        
        String phoneNumber,
        
        String company
) {
}
