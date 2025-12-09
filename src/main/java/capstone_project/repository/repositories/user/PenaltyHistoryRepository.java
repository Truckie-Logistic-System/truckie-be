package capstone_project.repository.repositories.user;

import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PenaltyHistoryRepository
        extends BaseRepository<PenaltyHistoryEntity> {

    @Query("SELECT ph FROM PenaltyHistoryEntity ph WHERE ph.vehicleAssignmentEntity.id = :vehicleAssignmentId")
    List<PenaltyHistoryEntity> findByVehicleAssignmentId(@Param("vehicleAssignmentId") UUID vehicleAssignmentId);

    @Query("SELECT ph FROM PenaltyHistoryEntity ph WHERE ph.issueBy.id = :driverId")
    List<PenaltyHistoryEntity> findByDriverId(@Param("driverId") UUID driverId);

    @Query("SELECT COUNT(ph) FROM PenaltyHistoryEntity ph WHERE ph.issueBy.id = :driverId")
    long countByDriverId(@Param("driverId") UUID driverId);

    @Query("SELECT COUNT(ph) FROM PenaltyHistoryEntity ph WHERE ph.createdAt BETWEEN :from AND :to")
    long countByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT ph FROM PenaltyHistoryEntity ph WHERE ph.createdAt BETWEEN :from AND :to")
    List<PenaltyHistoryEntity> findByCreatedAtBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("""
        SELECT ph FROM PenaltyHistoryEntity ph
        JOIN FETCH ph.vehicleAssignmentEntity va
        LEFT JOIN FETCH va.vehicleEntity v
        LEFT JOIN FETCH va.driver1 d1
        LEFT JOIN FETCH d1.user u1
        LEFT JOIN FETCH va.driver2 d2
        LEFT JOIN FETCH d2.user u2
        LEFT JOIN FETCH ph.issueBy issueBy
        LEFT JOIN FETCH issueBy.user issueByUser
        WHERE ph.id = :id
        """)
    Optional<PenaltyHistoryEntity> findDetailById(@Param("id") UUID id);
}