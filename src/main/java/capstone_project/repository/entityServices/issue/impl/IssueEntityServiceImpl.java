package capstone_project.repository.entityServices.issue.impl;

import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.issue.IssueRepository;
import capstone_project.repository.entityServices.issue.IssueEntityService;
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
    public Optional<IssueEntity> findEntityById(UUID uuid) {
        return issueRepository.findById(uuid);
    }

    @Override
    public List<IssueEntity> findAll() {
        return issueRepository.findAll();
    }

    @Override
    public IssueEntity findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentId) {
        return issueRepository.findByVehicleAssignmentEntity(vehicleAssignmentId);
    }

    @Override
    public List<IssueEntity> findByStaff(UserEntity staffId) {
        return issueRepository.findByStaff(staffId);
    }

    @Override
    public List<IssueEntity> findByStatus(String status) {
        return issueRepository.findByStatus(status);
    }

    @Override
    public List<IssueEntity> findByIssueTypeEntity(IssueTypeEntity issueType) {
        return issueRepository.findByIssueTypeEntity(issueType);
    }
}
