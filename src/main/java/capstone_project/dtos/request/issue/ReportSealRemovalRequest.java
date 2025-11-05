package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.UUID;

/**
 * Request DTO for driver to report seal removal
 * Step 1 of seal replacement flow
 */
public record ReportSealRemovalRequest(
        @NotNull @UUID String vehicleAssignmentId,
        @NotNull @UUID String issueTypeId, // Must be SEAL_REPLACEMENT category
        @NotNull @UUID String oldSealId, // Seal bị gỡ
        @NotBlank String description, // Lý do gỡ seal
        @NotBlank String sealRemovalImage, // Ảnh chứng minh seal đã gỡ
        Double locationLatitude,
        Double locationLongitude
) {
}
