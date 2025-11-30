package capstone_project.repository.entityServices.order.order;

import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JourneyHistoryEntityService extends BaseEntityService<JourneyHistoryEntity, UUID> {
    /**
     * Find all journey histories for a vehicle assignment (unsorted)
     * Used by service layer for FE/Mobile mappers - they handle filtering themselves
     */
    List<JourneyHistoryEntity> findByVehicleAssignmentId(UUID vehicleAssignmentId);
    
    /**
     * ✅ Find all journey histories for a vehicle assignment, sorted DESC (newest first)
     */
    List<JourneyHistoryEntity> findByVehicleAssignmentIdSorted(UUID vehicleAssignmentId);
    
    /**
     * ✅ Find the most recent ACTIVE or COMPLETED journey
     * This is the preferred method for fuel consumption calculation and route display
     */
    Optional<JourneyHistoryEntity> findLatestActiveJourney(UUID vehicleAssignmentId);
    
    /**
     * ✅ Find the most recent journey regardless of status
     */
    Optional<JourneyHistoryEntity> findLatestJourney(UUID vehicleAssignmentId);
    
    /**
     * Delete a journey history by entity
     */
    void delete(JourneyHistoryEntity entity);
    
    /**
     * Delete a journey history by ID
     */
    void deleteById(UUID id);
}
