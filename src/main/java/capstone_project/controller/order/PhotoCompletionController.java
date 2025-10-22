package capstone_project.controller.order;

import capstone_project.dtos.request.order.CreatePhotoCompletionRequest;
import capstone_project.dtos.request.order.UpdatePhotoCompletionRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.PhotoCompletionResponse;
import capstone_project.service.services.order.order.PhotoCompletionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${photo-completion.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class PhotoCompletionController {
    private final PhotoCompletionService photoCompletionService;


    /**
     * Upload và lưu ảnh hoàn thành
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PhotoCompletionResponse>> uploadAndSavePhoto(
            @RequestPart("file") MultipartFile file,
            @RequestPart("request") CreatePhotoCompletionRequest request) throws IOException {
        final var result = photoCompletionService.uploadAndSavePhoto(file, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Upload và lưu nhiều ảnh hoàn thành cùng lúc
     */
    @PostMapping(value = "/upload-multiple", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<PhotoCompletionResponse>>> uploadAndSaveMultiplePhotos(
            @RequestPart("files") List<MultipartFile> files,
            @RequestPart("request") CreatePhotoCompletionRequest request) throws IOException {
        final var result = photoCompletionService.uploadAndSaveMultiplePhotos(files, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Cập nhật mô tả ảnh
     */
    @PutMapping
    public ResponseEntity<ApiResponse<PhotoCompletionResponse>> updatePhoto(
            @RequestBody UpdatePhotoCompletionRequest request) {
        final var result = photoCompletionService.updatePhoto(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Lấy ảnh theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PhotoCompletionResponse>> getPhoto(@PathVariable UUID id) {
        final var result = photoCompletionService.getPhoto(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Lấy tất cả ảnh
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<PhotoCompletionResponse>>> getAllPhotos() {
        final var result = photoCompletionService.getAllPhotos();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

}
