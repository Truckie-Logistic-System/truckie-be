package capstone_project.service.services.issue;

import capstone_project.dtos.request.issue.CreateIssueTypeRequest;
import capstone_project.dtos.request.issue.UpdateIssueTypeRequest;
import capstone_project.dtos.response.issue.GetIssueTypeResponse;

import java.util.List;
import java.util.UUID;

public interface IssueTypeService {
    List<GetIssueTypeResponse> getAllIssueType();

    GetIssueTypeResponse getIssueTypeById(UUID id);

    List<GetIssueTypeResponse> getIssueTypeContainNameResponseList(String name);

    GetIssueTypeResponse createIssueType(CreateIssueTypeRequest createIssueTypeRequest);

    GetIssueTypeResponse updateIssueType(UpdateIssueTypeRequest updateIssueTypeRequest);
}
