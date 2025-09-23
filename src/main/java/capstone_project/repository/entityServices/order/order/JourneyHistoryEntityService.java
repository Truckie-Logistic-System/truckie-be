package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.UUID;

public interface JourneyHistoryEntityService extends BaseEntityService<JourneyHistoryEntity, UUID> {
    // Additional methods specific to JourneyHistoryEntity can be defined here if needed
    List<JourneyHistoryEntity> findByVehicleAssignmentId(UUID vehicleAssignmentId);
}
