package capstone_project.dtos.response.vehicle;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record MaintenanceTypeResponse(
        UUID id,
        String maintenanceTypeName,
        String description,
        Boolean isActive,
        LocalDateTime createdAt,
        LocalDateTime modifiedAt
) {}
