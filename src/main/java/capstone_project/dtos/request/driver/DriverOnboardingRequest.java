package capstone_project.dtos.request.driver;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.io.Serial;
import java.io.Serializable;

/**
 * Request DTO for driver first-time onboarding.
 * Submitted when driver logs in for the first time (status = INACTIVE).
 * Contains new password and face image URL.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DriverOnboardingRequest implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The temporary password provided by admin (current password)
     */
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    /**
     * New password chosen by the driver
     */
    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 50, message = "Password must be between 6 and 50 characters")
    private String newPassword;

    /**
     * Confirmation of new password
     */
    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;

    /**
     * URL of the face image uploaded to Cloudinary.
     * This will be set as the driver's avatar/profile image.
     * NOTE: This field is deprecated - use faceImageFile instead.
     */
    @Deprecated
    private String faceImageUrl;
}
