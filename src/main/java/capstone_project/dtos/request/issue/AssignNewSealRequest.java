package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for staff to assign new seal to replace removed seal
 */
public record AssignNewSealRequest(
        @NotNull(message = "Issue ID is required")
        UUID issueId,

        @NotNull(message = "New seal ID is required")
        UUID newSealId,

        @NotNull(message = "Staff ID is required")
        UUID staffId
) {
}
