package capstone_project.dtos.response.seal;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for Seal information
 * Contains seal code and description
 */
public record SealResponse(
        UUID id,
        String sealCode,
        String description,
        String status,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
