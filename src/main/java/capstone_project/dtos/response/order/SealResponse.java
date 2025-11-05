package capstone_project.dtos.response.order;

import java.time.LocalDateTime;
import java.util.UUID;

public record SealResponse(
    UUID id,
    String sealCode,
    String description,
    LocalDateTime sealDate,
    String sealAttachedImage,
    String status
) {}
