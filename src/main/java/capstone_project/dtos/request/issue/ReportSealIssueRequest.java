package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Request DTO for reporting seal removal issue
 * Driver reports that seal was removed (e.g., by traffic police)
 */
public record ReportSealIssueRequest(
        @NotNull(message = "Vehicle assignment ID is required")
        UUID vehicleAssignmentId,

        @NotNull(message = "Issue type ID is required")
        UUID issueTypeId,

        @NotNull(message = "Seal ID is required")
        UUID sealId,

        @NotBlank(message = "Description is required")
        String description,

        Double locationLatitude,

        Double locationLongitude,

        @NotNull(message = "Seal removal image is required")
        MultipartFile sealRemovalImage // Image file of removed seal
) {
}
