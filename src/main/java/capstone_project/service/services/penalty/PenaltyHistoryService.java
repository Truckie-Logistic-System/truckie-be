package capstone_project.service.services.penalty;

import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;

import java.util.List;
import java.util.UUID;

public interface PenaltyHistoryService {
    List<PenaltyHistoryResponse> getAllPenalties();
    PenaltyHistoryResponse getPenaltyById(UUID penaltyId);
    List<PenaltyHistoryResponse> getPenaltiesByDriver(UUID driverId);
    List<PenaltyHistoryResponse> getPenaltiesByVehicleAssignment(UUID vehicleAssignmentId);
    PenaltyHistoryResponse createPenalty(PenaltyHistoryEntity penalty);
    PenaltyHistoryResponse updatePenalty(UUID penaltyId, PenaltyHistoryEntity penalty);
    void deletePenalty(UUID penaltyId);
}
