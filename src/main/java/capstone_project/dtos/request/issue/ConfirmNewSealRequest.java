package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for driver to confirm new seal attachment
 */
public record ConfirmNewSealRequest(
        @NotNull(message = "Issue ID is required")
        UUID issueId,

        @NotBlank(message = "New seal attached image is required")
        String newSealAttachedImage // URL of new seal image
) {
}
