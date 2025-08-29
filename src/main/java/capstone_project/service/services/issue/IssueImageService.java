package capstone_project.service.services.issue;

import capstone_project.dtos.request.issue.CreateIssueImageRequest;
import capstone_project.dtos.response.issue.GetIssueImageResponse;


import java.util.UUID;

public interface IssueImageService {
    GetIssueImageResponse createImage(CreateIssueImageRequest request);

    GetIssueImageResponse getImage(UUID issueId);
}
