package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignStaffForIssueRequest(
        @NotNull
        UUID issueId,
        @NotNull
        UUID staffId
) {
}
