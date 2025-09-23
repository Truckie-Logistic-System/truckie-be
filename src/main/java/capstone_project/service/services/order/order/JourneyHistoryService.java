package capstone_project.service.services.order.order;

import capstone_project.dtos.request.order.JourneyHistoryRequest;
import capstone_project.dtos.request.order.UpdateJourneyHistoryRequest;
import capstone_project.dtos.response.order.JourneyHistoryResponse;

import java.util.List;
import java.util.UUID;

public interface JourneyHistoryService {
    List<JourneyHistoryResponse> getAll();
    JourneyHistoryResponse getById(UUID id);
    JourneyHistoryResponse create(JourneyHistoryRequest req);
    JourneyHistoryResponse update(UUID id, UpdateJourneyHistoryRequest req);
    void delete(UUID id);
    List<JourneyHistoryResponse> getByVehicleAssignmentId(UUID vehicleAssignmentId);
}
