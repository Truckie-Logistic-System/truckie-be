package capstone_project.dtos.response.issue;

import java.util.UUID;

public record GetIssueTypeResponse(
        UUID id,
        String issueTypeName,
        String description,
        String issueCategory,
        Boolean isActive
) {
}
