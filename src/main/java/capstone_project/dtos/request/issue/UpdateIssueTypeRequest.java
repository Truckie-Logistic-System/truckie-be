package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.UUID;

public record UpdateIssueTypeRequest(
        @NotNull @UUID String id,
        @NotBlank String name,
        @NotBlank String description,
        @NotNull Boolean status
) {
}
