package capstone_project.controller.user;

import capstone_project.dtos.request.user.PenaltyHistoryRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.service.services.user.PenaltyHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping
    public ResponseEntity<ApiResponse<PenaltyHistoryResponse>> create(
            @RequestBody @Valid PenaltyHistoryRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.create(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PenaltyHistoryResponse>> update(
            @PathVariable UUID id,
            @RequestBody @Valid PenaltyHistoryRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.update(id, req)));
    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
//        service.delete(id);
//        return ResponseEntity.ok(ApiResponse.ok());
//    }
}