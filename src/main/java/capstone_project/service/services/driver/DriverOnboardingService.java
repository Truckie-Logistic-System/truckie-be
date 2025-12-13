package capstone_project.service.services.driver;

import capstone_project.dtos.request.driver.DriverOnboardingRequest;
import capstone_project.dtos.request.user.AdminCreateDriverRequest;
import capstone_project.dtos.response.user.DriverCreatedResponse;
import capstone_project.dtos.response.user.DriverResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Service for driver onboarding operations.
 * Handles admin driver creation and driver first-time setup.
 */
public interface DriverOnboardingService {

    /**
     * Admin creates a new driver account.
     * Generates random temporary password and sets status to INACTIVE.
     *
     * @param request Driver creation request from admin
     * @return Created driver info with temporary password
     */
    DriverCreatedResponse createDriverByAdmin(AdminCreateDriverRequest request);

    /**
     * Driver completes first-time onboarding.
     * Changes password, sets face image, and activates account.
     *
     * @param request Onboarding request with new password and face image URL
     * @return Updated driver info with ACTIVE status
     * @deprecated Use completeOnboardingWithImage instead
     */
    @Deprecated
    DriverResponse completeOnboarding(DriverOnboardingRequest request);

    /**
     * Driver completes first-time onboarding with face image file.
     * Changes password, uploads face image to Cloudinary, and activates account.
     *
     * @param request Onboarding request with new password
     * @param faceImage Face image file to be uploaded to Cloudinary
     * @return Updated driver info with ACTIVE status
     */
    DriverResponse completeOnboardingWithImage(DriverOnboardingRequest request, MultipartFile faceImage);

    /**
     * Check if current driver needs to complete onboarding.
     *
     * @return true if driver status is INACTIVE
     */
    boolean needsOnboarding();
}
