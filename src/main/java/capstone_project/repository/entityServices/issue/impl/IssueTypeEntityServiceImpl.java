package capstone_project.repository.entityServices.issue.impl;

import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.repository.repositories.issue.IssueTypeRepository;
import capstone_project.repository.entityServices.issue.IssueTypeEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueTypeEntityServiceImpl implements IssueTypeEntityService {

    private final IssueTypeRepository issueTypeRepository;

    @Override
    public IssueTypeEntity save(IssueTypeEntity entity) {
        return issueTypeRepository.save(entity);
    }

    @Override
    public Optional<IssueTypeEntity> findEntityById(UUID uuid) {
        return issueTypeRepository.findById(uuid);
    }

    @Override
    public List<IssueTypeEntity> findAll() {
        return issueTypeRepository.findAll();
    }

    @Override
    public List<IssueTypeEntity> findByIssueTypeNameContaining(String name) {
        return issueTypeRepository.findByIssueTypeNameContaining(name);
    }

    @Override
    public IssueTypeEntity findByIssueTypeName(String name) {
        return issueTypeRepository.findByIssueTypeName(name);
    }
}
