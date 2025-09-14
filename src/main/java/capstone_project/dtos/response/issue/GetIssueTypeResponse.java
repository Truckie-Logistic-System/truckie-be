package capstone_project.dtos.response.issue;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record GetIssueTypeResponse(
        UUID id,
        String issueTypeName,
        String description,
        Boolean isActive
) {
}
