package capstone_project.controller.seal;

import capstone_project.dtos.request.order.seal.SealRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.seal.GetSealResponse;
import capstone_project.service.services.order.seal.SealService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("${seal.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class SealController {
    private final SealService sealService;

    @PostMapping(value = "/confirm-seal-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<GetSealResponse>> confirmSealAttachment(
            @RequestParam("vehicleAssignmentId") UUID vehicleAssignmentId,
            @RequestParam("sealImage") MultipartFile sealImage,
            @RequestParam("sealCode") String sealCode) {
        SealRequest request = new SealRequest(vehicleAssignmentId, sealImage, sealCode);
        final var result = sealService.confirmSealAttachment(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/{sealId}")
    public ResponseEntity<ApiResponse<GetSealResponse>> removeSealBySealId(
            @PathVariable UUID sealId) {
        final var result = sealService.removeSealBySealId(sealId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-all/{sealId}")
    public ResponseEntity<ApiResponse<GetSealResponse>> getAllBySealId(
            @PathVariable UUID sealId) {
        final var result = sealService.getAllBySealId(sealId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-active-by-assignment-id/{vehicleAssignmentId}")
    public ResponseEntity<ApiResponse<GetSealResponse>> getSealByVehicleAssignmentId(
            @PathVariable UUID vehicleAssignmentId) {
        final var result = sealService.getSealByVehicleAssignmentId(vehicleAssignmentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
