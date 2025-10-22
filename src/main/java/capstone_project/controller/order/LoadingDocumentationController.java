package capstone_project.controller.order;

import capstone_project.dtos.request.order.LoadingDocumentationRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.LoadingDocumentationResponse;
import capstone_project.service.services.order.order.LoadingDocumentationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${loading-documentation.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class LoadingDocumentationController {
    private final LoadingDocumentationService loadingDocumentationService;

    /**
     * API tổng hợp để tài xế:
     * 1. Upload hình ảnh bằng chứng đóng gói (packing proof)
     * 2. Xác nhận đã gắn seal bằng cách chụp ảnh và cung cấp mã seal
     *
     * Được gọi khi tài xế đến địa điểm lấy hàng, hoàn tất việc đóng gói và gắn seal
     */
    @PostMapping(path = "/document-loading-and-seal", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<LoadingDocumentationResponse>> documentLoading(
            @RequestPart("packingProofImages") List<MultipartFile> packingProofImages,
            @RequestPart("sealImage") MultipartFile sealImage,
            @ModelAttribute LoadingDocumentationRequest request) throws IOException {
        final var result = loadingDocumentationService.documentLoading(packingProofImages, sealImage, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Get all loading documentation for a vehicle assignment
     */
    @GetMapping("/{vehicleAssignmentId}")
    public ResponseEntity<ApiResponse<LoadingDocumentationResponse>> getLoadingDocumentation(
            @PathVariable UUID vehicleAssignmentId) {
        final var result = loadingDocumentationService.getLoadingDocumentation(vehicleAssignmentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
