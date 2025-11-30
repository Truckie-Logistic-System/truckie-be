package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for driver to report REROUTE issue
 * Driver reports problem on a specific journey segment
 * Images are optional for REROUTE category
 */
public record ReportRerouteRequest(
        @NotBlank 
        String description,
        
        @NotNull 
        UUID vehicleAssignmentId,
        
        @NotNull 
        UUID issueTypeId,
        
        @NotNull 
        UUID affectedSegmentId, // Journey segment gặp sự cố
        
        BigDecimal locationLatitude, // Vị trí báo cáo sự cố
        
        BigDecimal locationLongitude
) {
}
