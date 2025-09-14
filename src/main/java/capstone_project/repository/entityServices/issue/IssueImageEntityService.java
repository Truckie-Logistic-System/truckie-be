package capstone_project.repository.entityServices.issue;

import capstone_project.entity.issue.IssueImageEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface IssueImageEntityService extends BaseEntityService<IssueImageEntity, UUID> {
    List<IssueImageEntity> findByIssueEntity_Id(UUID issueId);

    List<IssueImageEntity> saveAll(List<IssueImageEntity> issueImageEntities);
}
