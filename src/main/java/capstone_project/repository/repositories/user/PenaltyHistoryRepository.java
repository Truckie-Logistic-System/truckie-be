package capstone_project.repository.repositories.user;

import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PenaltyHistoryRepository
        extends BaseRepository<PenaltyHistoryEntity> {

    @Query("SELECT ph FROM PenaltyHistoryEntity ph WHERE ph.vehicleAssignmentEntity.id = :vehicleAssignmentId")
    List<PenaltyHistoryEntity> findByVehicleAssignmentId(@Param("vehicleAssignmentId") UUID vehicleAssignmentId);

    @Query("SELECT ph FROM PenaltyHistoryEntity ph WHERE ph.issueBy.id = :driverId")
    List<PenaltyHistoryEntity> findByDriverId(@Param("driverId") UUID driverId);

    @Query("SELECT COUNT(ph) FROM PenaltyHistoryEntity ph WHERE ph.issueBy.id = :driverId")
    long countByDriverId(@Param("driverId") UUID driverId);
}