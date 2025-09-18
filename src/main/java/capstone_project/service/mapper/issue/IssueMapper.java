package capstone_project.service.mapper.issue;

import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import capstone_project.dtos.response.issue.GetIssueImageResponse;
import capstone_project.dtos.response.issue.GetIssueTypeResponse;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface IssueMapper {
    GetIssueTypeResponse toIssueTypeResponse(IssueTypeEntity issueType);

    List<GetIssueTypeResponse> toIssueTypeResponses(List<IssueTypeEntity> issueTypes);

    GetBasicIssueResponse toIssueBasicResponse(IssueEntity issue);

    List<GetBasicIssueResponse> toIssueBasicResponses(List<IssueEntity> issues);

}
