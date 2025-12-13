package capstone_project.service.services.user;

import capstone_project.dtos.request.user.PenaltyHistoryRequest;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;
import java.util.List;

public interface PenaltyHistoryService {
    List<PenaltyHistoryResponse> getAll();
    PenaltyHistoryResponse getById(UUID id);
    List<PenaltyHistoryResponse> getByDriverId(UUID driverId);
    
    /**
     * Get predefined traffic violation reasons for driver penalty reporting
     * @return List of violation reasons in Vietnamese
     */
    List<String> getTrafficViolationReasons();
}
