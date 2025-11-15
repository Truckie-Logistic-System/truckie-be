package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request DTO for driver to report order rejection by recipient
 * Driver selects which order details (packages) need to be returned
 * Note: orderDetailIds are tracking codes (String), not UUIDs
 */
public record ReportOrderRejectionRequest(
        @NotNull(message = "Vehicle assignment ID is required")
        UUID vehicleAssignmentId,

        @NotNull(message = "Order detail IDs are required")
        @NotEmpty(message = "At least one order detail must be selected for return")
        List<String> orderDetailIds,

        Double locationLatitude,

        Double locationLongitude
) {
}
