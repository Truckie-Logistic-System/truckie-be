package capstone_project.service.entityServices.issue.impl;

import capstone_project.entity.issue.IssueEntity;
import capstone_project.repository.issue.IssueRepository;
import capstone_project.service.entityServices.issue.IssueEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class IssueEntityServiceImpl implements IssueEntityService {

    private final IssueRepository issueRepository;

    @Override
    public IssueEntity save(IssueEntity entity) {
        return issueRepository.save(entity);
    }

    @Override
    public Optional<IssueEntity> findById(UUID uuid) {
        return issueRepository.findById(uuid);
    }

    @Override
    public List<IssueEntity> findAll() {
        return issueRepository.findAll();
    }
}
