package capstone_project.controller.user;

import capstone_project.dtos.request.user.PenaltyHistoryRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.service.services.user.PenaltyHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${penalty.api.base-path}")
@RequiredArgsConstructor
public class PenaltyHistoryController {

    private final PenaltyHistoryService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PenaltyHistoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PenaltyHistoryResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getById(id)));
    }

    
    /**
     * Get predefined traffic violation reasons for driver penalty reporting
     * This endpoint is public as it provides static configuration data
     */
    @GetMapping("/traffic-violation-reasons")
    public ResponseEntity<ApiResponse<List<String>>> getTrafficViolationReasons() {
        List<String> reasons = service.getTrafficViolationReasons();
        return ResponseEntity.ok(ApiResponse.ok(reasons));
    }
}