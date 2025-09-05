package capstone_project.repository.repositories.issue;

import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;

public interface IssueTypeRepository extends BaseRepository<IssueTypeEntity> {
    List<IssueTypeEntity> findByIssueTypeNameContaining(String name);

    IssueTypeEntity findByIssueTypeName(String name);
}
