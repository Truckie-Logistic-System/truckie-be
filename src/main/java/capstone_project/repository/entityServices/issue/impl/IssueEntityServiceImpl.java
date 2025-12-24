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
    public List<IssueEntity> findAllSortedByReportedAtDesc() {
        return issueRepository.findAllSortedByReportedAtDesc();
    }

    @Override
    public IssueEntity findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentId) {
        return issueRepository.findByVehicleAssignmentEntity(vehicleAssignmentId);
    }
    
    @Override
    public List<IssueEntity> findAllByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentEntity) {
        return issueRepository.findAllByVehicleAssignmentEntity(vehicleAssignmentEntity);
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

    @Override
    public Optional<IssueEntity> findByIdWithDetails(UUID id) {
        // Fetch with vehicle, drivers, and issueImages in one query
        Optional<IssueEntity> issueOpt = issueRepository.findByIdWithVehicle(id);
        if (issueOpt.isEmpty()) {
            return Optional.empty();
        }
        
        // Fetch driver1 with user (separate query to avoid MultipleBagFetchException)
        Optional<IssueEntity> driver1Opt = issueRepository.findByIdWithDriver1(id);
        
        // Fetch driver2 with user
        Optional<IssueEntity> driver2Opt = issueRepository.findByIdWithDriver2(id);
        
        // Fetch order detail
        Optional<IssueEntity> orderDetailOpt = issueRepository.findByIdWithOrderDetail(id);
        
        // ✅ Fetch reroutedJourney for REROUTE issues (includes journey segments)
        Optional<IssueEntity> rerouteJourneyOpt = issueRepository.findByIdWithReroutedJourney(id);
        
        // Return the issue - issueImages is now loaded in the main query (findByIdWithVehicle)
        // Other related entities are loaded in Hibernate session cache and will be accessible
        return issueOpt;
    }
    
    @Override
    public Optional<IssueEntity> findByIdWithIssueImages(UUID id) {
        return issueRepository.findByIdWithIssueImages(id);
    }
    
    @Override
    public List<IssueEntity> findInProgressOrderRejections() {
        return issueRepository.findInProgressOrderRejections();
    }
}
