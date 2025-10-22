package capstone_project.dtos.response.seal;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for OrderSeal entity
 */
public record OrderSealResponse(
    UUID id,
    UUID vehicleAssignmentId,
    String vehicleAssignmentTrackingCode,
    SealResponse seal,
    LocalDateTime sealDate,
    String description,
    String sealAttachedImage,
    LocalDateTime sealRemovalTime,
    String sealRemovalReason,
    String status,
    LocalDateTime createdAt
) {}
