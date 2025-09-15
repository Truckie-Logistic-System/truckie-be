package capstone_project.dtos.response.issue;

import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueImageEntity;

import java.util.List;

public record GetIssueImageResponse(
        GetBasicIssueResponse issue,
        List<String> imageUrl
) {
}
