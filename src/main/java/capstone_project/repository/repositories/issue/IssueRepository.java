package capstone_project.repository.repositories.issue;

import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IssueRepository extends BaseRepository<IssueEntity> {
    // Additional methods specific to IssueEntity can be defined here if needed
    IssueEntity findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentId);
    
    // Find ALL issues for a vehicle assignment (can have multiple issues)
    List<IssueEntity> findAllByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentEntity);

    @Query("SELECT i FROM IssueEntity i " +
            "LEFT JOIN FETCH i.issueTypeEntity " +
            "LEFT JOIN FETCH i.staff " +
            "LEFT JOIN FETCH i.vehicleAssignmentEntity " +
            "WHERE i.staff = :staff " +
            "ORDER BY i.reportedAt DESC")
    List<IssueEntity> findByStaff(@Param("staff") UserEntity staffId);

    @Query("SELECT i FROM IssueEntity i " +
            "LEFT JOIN FETCH i.issueTypeEntity " +
            "LEFT JOIN FETCH i.staff " +
            "LEFT JOIN FETCH i.vehicleAssignmentEntity " +
            "WHERE i.status = :status " +
            "ORDER BY i.reportedAt DESC")
    List<IssueEntity> findByStatus(@Param("status") String status);

    @Query("SELECT i FROM IssueEntity i " +
            "LEFT JOIN FETCH i.issueTypeEntity " +
            "LEFT JOIN FETCH i.staff " +
            "LEFT JOIN FETCH i.vehicleAssignmentEntity " +
            "ORDER BY i.reportedAt DESC")
    List<IssueEntity> findAllSortedByReportedAtDesc();

    @Query("SELECT DISTINCT i FROM IssueEntity i " +
            "LEFT JOIN FETCH i.issueTypeEntity " +
            "LEFT JOIN FETCH i.staff " +
            "LEFT JOIN FETCH i.vehicleAssignmentEntity va " +
            "LEFT JOIN FETCH va.vehicleEntity v " +
            "LEFT JOIN FETCH v.vehicleTypeEntity " +
            "LEFT JOIN FETCH va.driver1 " +
            "LEFT JOIN FETCH va.driver2 " +
            "LEFT JOIN FETCH i.affectedSegment " +
            "LEFT JOIN FETCH i.reroutedJourney " +
            "WHERE i.id = :id")
    Optional<IssueEntity> findByIdWithVehicle(@Param("id") UUID id);
    
    @Query("SELECT i FROM IssueEntity i " +
            "LEFT JOIN FETCH i.vehicleAssignmentEntity va " +
            "LEFT JOIN FETCH va.driver1 d1 " +
            "LEFT JOIN FETCH d1.user " +
            "WHERE i.id = :id")
    Optional<IssueEntity> findByIdWithDriver1(@Param("id") UUID id);
    
    @Query("SELECT i FROM IssueEntity i " +
            "LEFT JOIN FETCH i.vehicleAssignmentEntity va " +
            "LEFT JOIN FETCH va.driver2 d2 " +
            "LEFT JOIN FETCH d2.user " +
            "WHERE i.id = :id")
    Optional<IssueEntity> findByIdWithDriver2(@Param("id") UUID id);
    
    @Query("SELECT DISTINCT i FROM IssueEntity i " +
            "LEFT JOIN FETCH i.orderDetails od " +
            "WHERE i.id = :id")
    Optional<IssueEntity> findByIdWithOrderDetail(@Param("id") UUID id);

    @Query("SELECT i FROM IssueEntity i " +
            "LEFT JOIN FETCH i.issueTypeEntity " +
            "LEFT JOIN FETCH i.staff " +
            "LEFT JOIN FETCH i.vehicleAssignmentEntity " +
            "WHERE i.issueTypeEntity = :issueType " +
            "ORDER BY i.reportedAt DESC")
    List<IssueEntity> findByIssueTypeEntity(@Param("issueType") IssueTypeEntity issueType);
    
    @Query("SELECT i FROM IssueEntity i " +
            "JOIN FETCH i.issueTypeEntity it " +
            "WHERE i.status = 'IN_PROGRESS' " +
            "AND it.issueCategory = 'ORDER_REJECTION'")
    List<IssueEntity> findInProgressOrderRejections();
    
    @Query("SELECT DISTINCT i FROM IssueEntity i " +
            "LEFT JOIN FETCH i.reroutedJourney rj " +
            "LEFT JOIN FETCH rj.journeySegments " +
            "WHERE i.id = :id")
    Optional<IssueEntity> findByIdWithReroutedJourney(@Param("id") UUID id);
    
    @Query("SELECT COUNT(i) FROM IssueEntity i " +
            "WHERE i.vehicleAssignmentEntity.driver1.id = :driverId " +
            "OR i.vehicleAssignmentEntity.driver2.id = :driverId")
    long countByDriverId(@Param("driverId") UUID driverId);
}
