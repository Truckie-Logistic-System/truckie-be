package capstone_project.repository.entityServices.issue;

import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueEntityService extends BaseEntityService<IssueEntity, UUID> {
    IssueEntity findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentId);
    
    // Find ALL issues for a vehicle assignment
    List<IssueEntity> findAllByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentEntity);

    List<IssueEntity> findByStaff(UserEntity staffId);

    List<IssueEntity> findByStatus(String status);

    List<IssueEntity> findByIssueTypeEntity(IssueTypeEntity issueType);

    List<IssueEntity> findAllSortedByReportedAtDesc();

    Optional<IssueEntity> findByIdWithDetails(UUID id);
    
    /**
     * Find all ORDER_REJECTION issues that are IN_PROGRESS
     * Optimized query for ReturnPaymentTimeoutScheduler
     */
    List<IssueEntity> findInProgressOrderRejections();
}
