package capstone_project.controller.order;

import capstone_project.dtos.request.order.JourneyHistoryRequest;
import capstone_project.dtos.request.order.UpdateJourneyHistoryRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.JourneyHistoryResponse;
import capstone_project.service.services.order.order.JourneyHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${journey-history.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class JourneyHistoryController {

    private final JourneyHistoryService journeyHistoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<JourneyHistoryResponse>>> getAllJourneyHistories() {
        final var result = journeyHistoryService.getAll();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{journeyHistoryId}")
    public ResponseEntity<ApiResponse<JourneyHistoryResponse>> getJourneyHistoryById(
            @PathVariable UUID journeyHistoryId) {
        final var result = journeyHistoryService.getById(journeyHistoryId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<JourneyHistoryResponse>> createJourneyHistory(
            @Valid @RequestBody JourneyHistoryRequest request) {
        final var result = journeyHistoryService.create(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{journeyHistoryId}")
    public ResponseEntity<ApiResponse<JourneyHistoryResponse>> updateJourneyHistory(
            @PathVariable UUID journeyHistoryId,
            @Valid @RequestBody UpdateJourneyHistoryRequest request) {
        final var result = journeyHistoryService.update(journeyHistoryId, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Xóa JourneyHistory
//    @DeleteMapping("/{journeyHistoryId}")
//    public ResponseEntity<ApiResponse<String>> deleteJourneyHistory(
//            @PathVariable UUID journeyHistoryId) {
//        journeyHistoryService.delete(journeyHistoryId);
//        return ResponseEntity.ok(ApiResponse.ok("JourneyHistory deleted successfully"));
//    }
}
