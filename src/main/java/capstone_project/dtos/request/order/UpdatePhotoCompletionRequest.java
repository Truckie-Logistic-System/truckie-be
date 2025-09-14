package capstone_project.dtos.request.order;

import java.util.UUID;

public record UpdatePhotoCompletionRequest(
        UUID id,
        String description
) {
}
