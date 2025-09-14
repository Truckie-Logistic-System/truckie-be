package capstone_project.dtos.request.order;

import java.util.UUID;

public record CreatePhotoCompletionRequest(
        String description,
        UUID vehicleAssignmentId,
        UUID deviceId
) {
}
