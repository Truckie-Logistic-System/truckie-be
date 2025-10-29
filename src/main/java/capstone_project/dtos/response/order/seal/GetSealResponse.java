package capstone_project.dtos.response.order.seal;

import java.time.LocalDateTime;
import java.util.UUID;

public record GetSealResponse(
        UUID id,
        String description,
        LocalDateTime sealDate,
        String status,
        UUID sealId,
        // Added additional fields for more complete seal information
        String sealCode,
        String sealAttachedImage,
        LocalDateTime sealRemovalTime,
        String sealRemovalReason
) {

}
