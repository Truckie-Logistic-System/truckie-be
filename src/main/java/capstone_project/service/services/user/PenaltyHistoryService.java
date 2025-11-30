package capstone_project.service.services.user;

import capstone_project.dtos.request.user.PenaltyHistoryRequest;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

import java.util.List;

public interface PenaltyHistoryService {
    List<PenaltyHistoryResponse> getAll();
    PenaltyHistoryResponse getById(UUID id);
    PenaltyHistoryResponse create(PenaltyHistoryRequest req);
    PenaltyHistoryResponse update(UUID id, PenaltyHistoryRequest req);
    void delete(UUID id);
    List<PenaltyHistoryResponse> getByDriverId(UUID driverId);
}
