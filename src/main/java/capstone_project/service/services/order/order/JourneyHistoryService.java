package capstone_project.service.services.order.order;

import capstone_project.dtos.request.order.JourneyHistoryRequest;
import capstone_project.dtos.request.order.UpdateJourneyHistoryRequest;
import capstone_project.dtos.response.order.JourneyHistoryResponse;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JourneyHistoryService {
    List<JourneyHistoryResponse> getAll();
    JourneyHistoryResponse getById(UUID id);
    JourneyHistoryResponse create(JourneyHistoryRequest req);
    JourneyHistoryResponse update(UUID id, UpdateJourneyHistoryRequest req);
    void delete(UUID id);
    
    /**
     * Get all journey histories for a vehicle assignment (unsorted)
     * Used by FE/Mobile mappers - they handle filtering themselves
     */
    List<JourneyHistoryResponse> getByVehicleAssignmentId(UUID vehicleAssignmentId);
    
    /**
     * Get all journey histories for a vehicle assignment, sorted DESC (newest first)
     * This should be used by mappers and controllers
     */
    List<JourneyHistoryResponse> getByVehicleAssignmentIdSorted(UUID vehicleAssignmentId);
    
    /**
     * Get the most recent ACTIVE or COMPLETED journey only
     * Returns Optional.empty() if no active journey exists
     */
    Optional<JourneyHistoryResponse> getLatestActiveJourney(UUID vehicleAssignmentId);
}
