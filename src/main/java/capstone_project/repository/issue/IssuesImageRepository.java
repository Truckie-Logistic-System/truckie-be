package capstone_project.repository.issue;

import capstone_project.entity.issue.IssueImageEntity;
import capstone_project.repository.common.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface IssuesImageRepository extends BaseRepository<IssueImageEntity> {
    List<IssueImageEntity> findByIssueEntity_Id(UUID issueId);
}
