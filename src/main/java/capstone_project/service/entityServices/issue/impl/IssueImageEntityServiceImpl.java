package capstone_project.service.entityServices.issue.impl;

import capstone_project.entity.issue.IssueImageEntity;
import capstone_project.repository.issue.IssuesImageRepository;
import capstone_project.service.entityServices.issue.IssueImageEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueImageEntityServiceImpl implements IssueImageEntityService {

    private final IssuesImageRepository issuesImageRepository;

    @Override
    public IssueImageEntity save(IssueImageEntity entity) {
        return issuesImageRepository.save(entity);
    }

    @Override
    public Optional<IssueImageEntity> findContractRuleEntitiesById(UUID uuid) {
        return issuesImageRepository.findById(uuid);
    }

    @Override
    public List<IssueImageEntity> findAll() {
        return issuesImageRepository.findAll();
    }

    @Override
    public List<IssueImageEntity> findByIssueEntity_Id(UUID issueId) {
        return issuesImageRepository.findByIssueEntity_Id(issueId);
    }

    @Override
    public List<IssueImageEntity> saveAll(List<IssueImageEntity> issueImageEntities) {
        return issuesImageRepository.saveAll(issueImageEntities);
    }
}
