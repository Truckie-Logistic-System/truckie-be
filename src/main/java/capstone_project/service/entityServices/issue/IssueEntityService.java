package capstone_project.service.entityServices.issue;

import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.service.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface IssueEntityService extends BaseEntityService<IssueEntity, UUID> {
    IssueEntity findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentId);

    List<IssueEntity> findByStaff(UserEntity staffId);

    List<IssueEntity> findByStatus(String status);

    List<IssueEntity> findByIssueTypeEntity(IssueTypeEntity issueType);
}
