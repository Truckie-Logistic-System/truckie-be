package capstone_project.dtos.response.order;

import java.util.UUID;

public record PhotoCompletionResponse(
        UUID id,
        String imageUrl,
        String description,
        UUID vehicleAssignmentId,
        UUID deviceId
) {
}
