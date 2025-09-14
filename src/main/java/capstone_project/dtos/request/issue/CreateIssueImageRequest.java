package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateIssueImageRequest(
        @NotNull
        UUID issueId,
        @NotNull
        List<String> imageUrl
) {
}
