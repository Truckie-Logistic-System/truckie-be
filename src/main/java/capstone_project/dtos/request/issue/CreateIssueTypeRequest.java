package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotBlank;

public record CreateIssueTypeRequest(
        @NotBlank String name,
        @NotBlank String description,
        @NotBlank String issueCategory
) {
}
