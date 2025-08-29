package capstone_project.repository.issue;

import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface IssueRepository extends BaseRepository<IssueEntity> {
    // Additional methods specific to IssueEntity can be defined here if needed
    IssueEntity findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentId);

    List<IssueEntity> findByStaff(UserEntity staffId);

    @Query("SELECT i FROM IssueEntity i " +
            "LEFT JOIN FETCH i.issueTypeEntity " +
            "LEFT JOIN FETCH i.staff " +
            "LEFT JOIN FETCH i.vehicleAssignmentEntity " +
            "WHERE i.status = :status")
    List<IssueEntity> findByStatus(@Param("status") String status);


    List<IssueEntity> findByIssueTypeEntity(IssueTypeEntity issueType);
}
