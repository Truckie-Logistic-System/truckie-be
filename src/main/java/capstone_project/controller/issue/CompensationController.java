package capstone_project.controller.issue;

import capstone_project.dtos.request.issue.CompensationAssessmentRequest;
import capstone_project.dtos.response.issue.CompensationAssessmentListResponse;
import capstone_project.dtos.response.issue.CompensationDetailResponse;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.service.CompensationService;
import capstone_project.service.services.cloudinary.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/compensation")
@RequiredArgsConstructor
@Slf4j
public class CompensationController {

    private final CompensationService compensationService;
    private final CloudinaryService cloudinaryService;

    /**
     * Get all compensation assessments for staff (sorted by createdAt DESC)
     */
    @GetMapping("/staff/list")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<CompensationAssessmentListResponse>>> getAllCompensationAssessments() {
        List<CompensationAssessmentListResponse> result = compensationService.getAllCompensationAssessments();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Get compensation detail for an issue
     */
    @GetMapping("/{issueId}")
    public ResponseEntity<CompensationDetailResponse> getCompensationDetail(@PathVariable UUID issueId) {
        CompensationDetailResponse response = compensationService.getCompensationDetail(issueId);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Calculate compensation preview (không lưu, chỉ tính toán)
     * Dùng để realtime update UI khi staff thay đổi các field
     */
    @PostMapping("/{issueId}/preview")
    public ResponseEntity<CompensationDetailResponse.CompensationBreakdown> calculatePreview(
            @PathVariable UUID issueId,
            @RequestBody CompensationAssessmentRequest previewRequest) {
        
        CompensationDetailResponse.CompensationBreakdown breakdown = 
            compensationService.calculatePreview(issueId, previewRequest);
        
        return ResponseEntity.ok(breakdown);
    }

    /**
     * Resolve compensation for an issue (with file upload support)
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompensationDetailResponse> resolveCompensation(
            @RequestPart("data") CompensationAssessmentRequest request,
            @RequestParam(value = "documentFiles", required = false) MultipartFile[] documentFiles,
            @RequestParam(value = "refundProofFile", required = false) MultipartFile refundProofFile) {
        
        try {
            // Upload document files if provided
            if (documentFiles != null && documentFiles.length > 0) {
                List<String> documentUrls = new ArrayList<>();
                for (MultipartFile file : documentFiles) {
                    if (!file.isEmpty()) {
                        Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                                file.getBytes(),
                                file.getOriginalFilename(),
                                "issue-documents");
                        String url = (String) uploadResult.get("url");
                        documentUrls.add(url);
                    }
                }
                // Set uploaded URLs to request (List<String> matches DTO field type)
                request.setDocumentImages(documentUrls);
            }
            
            // Upload refund proof file if provided
            if (refundProofFile != null && !refundProofFile.isEmpty()) {
                Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                        refundProofFile.getBytes(), 
                        refundProofFile.getOriginalFilename(), 
                        "refund-proofs");
                String refundUrl = (String) uploadResult.get("url");
                
                // Set refund URL in the nested refund object
                if (request.getRefund() != null) {
                    request.getRefund().setBankTransferImage(refundUrl);
                }
            }
            
            CompensationDetailResponse response = compensationService.resolveCompensation(request);
            return ResponseEntity.ok(response);
            
        } catch (IOException e) {
            log.error("Error processing file uploads in resolveCompensation: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Upload document images for compensation assessment
     */
    @PostMapping(value = "/upload-documents/{issueId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<CompensationDetailResponse> uploadDocumentImages(
            @PathVariable UUID issueId,
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            List<String> uploadedUrls = new ArrayList<>();
            
            // Upload each file to Cloudinary
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    byte[] fileBytes = file.getBytes();
                    
                    // Upload to Cloudinary in the "issue-documents" folder
                    Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                            fileBytes, 
                            originalFilename, 
                            "issue-documents");
                    
                    // Get the URL from the result
                    String url = (String) uploadResult.get("url");
                    uploadedUrls.add(url);
                }
            }
            
            // Update the compensation assessment with the uploaded URLs
            String[] urlArray = uploadedUrls.toArray(new String[0]);
            CompensationDetailResponse response = compensationService.uploadDocumentImages(issueId, urlArray);
            
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            log.error("Error uploading document images: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Upload refund proof images (bank transfer receipts, etc.)
     */
    @PostMapping(value = "/upload-refund-images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, List<String>>> uploadRefundImages(
            @RequestParam("files") MultipartFile[] files) {
        
        try {
            List<String> uploadedUrls = new ArrayList<>();
            
            // Upload each file to Cloudinary
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String originalFilename = file.getOriginalFilename();
                    byte[] fileBytes = file.getBytes();
                    
                    // Upload to Cloudinary in the "refund-proofs" folder
                    Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                            fileBytes, 
                            originalFilename, 
                            "refund-proofs");
                    
                    // Get the URL from the result
                    String url = (String) uploadResult.get("url");
                    uploadedUrls.add(url);
                }
            }
            
            return ResponseEntity.ok(Map.of("urls", uploadedUrls));
        } catch (IOException e) {
            log.error("Error uploading refund images: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        }
    }
}
