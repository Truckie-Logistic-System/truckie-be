package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.UUID;

public interface JourneyHistoryRepository extends BaseRepository<JourneyHistoryEntity> {
    List<JourneyHistoryEntity> findByVehicleAssignment_Id(UUID vehicleAssignmentId);
    // Additional methods specific to JourneyHistory can be defined here if needed
}
