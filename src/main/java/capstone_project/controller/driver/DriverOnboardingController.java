package capstone_project.controller.driver;

import capstone_project.dtos.request.driver.DriverOnboardingRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.service.services.driver.DriverOnboardingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

/**
 * Controller for driver onboarding operations.
 * Handles first-time login setup (password change + face upload).
 */
@RestController
@RequestMapping("${driver.api.base-path}/onboarding")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasAuthority('DRIVER')")
public class DriverOnboardingController {

    private final DriverOnboardingService driverOnboardingService;

    /**
     * Submit driver onboarding.
     * Changes password, sets face image, and activates account.
     * 
     * @param request Contains current password, new password, confirm password
     * @param faceImage Face image file to be uploaded to Cloudinary
     * @return Updated driver info with ACTIVE status
     */
    @PostMapping(
        value = "/submit",
        consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<ApiResponse<DriverResponse>> submitOnboarding(
        @RequestPart("data") @Valid DriverOnboardingRequest request,
        @RequestPart("faceImage") MultipartFile faceImage) {
        log.info("[submitOnboarding] Processing driver onboarding submission with face image: {}", 
                faceImage.getOriginalFilename());
        
        // Validate file
        validateFaceImageFile(faceImage);
        
        DriverResponse response = driverOnboardingService.completeOnboardingWithImage(request, faceImage);
        
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Check if current driver needs to complete onboarding.
     * 
     * @return true if driver status is INACTIVE and needs onboarding
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Boolean>> checkOnboardingStatus() {
        boolean needsOnboarding = driverOnboardingService.needsOnboarding();
        return ResponseEntity.ok(ApiResponse.ok(needsOnboarding));
    }
    
    /**
     * Validate the uploaded face image file.
     */
    private void validateFaceImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Face image file is required");
        }
        
        // Check file size (max 5MB)
        if (file.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("Face image file size must be less than 5MB");
        }
        
        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png"))) {
            throw new IllegalArgumentException("Face image must be JPEG or PNG format");
        }
    }
}
