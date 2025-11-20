package capstone_project.repository.repositories.order.order;

import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JourneyHistoryRepository extends BaseRepository<JourneyHistoryEntity> {
    /**
     * Find all journey histories for a vehicle assignment (unsorted)
     * @deprecated Use findByVehicleAssignment_IdOrderByCreatedAtDesc instead
     */
    List<JourneyHistoryEntity> findByVehicleAssignment_Id(UUID vehicleAssignmentId);
    
    /**
     * ✅ Find all journey histories for a vehicle assignment, sorted by creation date DESC (newest first)
     * Use this for display or when you need the most recent journey
     */
    List<JourneyHistoryEntity> findByVehicleAssignment_IdOrderByCreatedAtDesc(UUID vehicleAssignmentId);
    
    /**
     * ✅ Find ACTIVE or COMPLETED journey histories, sorted by creation date DESC
     * Use this to get the current/latest active journey for route calculation
     */
    List<JourneyHistoryEntity> findByVehicleAssignment_IdAndStatusInOrderByCreatedAtDesc(
            UUID vehicleAssignmentId, 
            List<String> statuses
    );
    
    /**
     * ✅ Find the most recent journey history for a vehicle assignment
     * Returns the newest journey regardless of status
     */
    Optional<JourneyHistoryEntity> findFirstByVehicleAssignment_IdOrderByCreatedAtDesc(UUID vehicleAssignmentId);
    
    /**
     * ✅ Find the most recent ACTIVE or COMPLETED journey
     * Returns the newest active/completed journey only
     */
    Optional<JourneyHistoryEntity> findFirstByVehicleAssignment_IdAndStatusInOrderByCreatedAtDesc(
            UUID vehicleAssignmentId,
            List<String> statuses
    );
}
