package capstone_project.dtos.request.issue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateBasicIssueRequest(
        @NotBlank String description,
        BigDecimal locationLatitude,
        BigDecimal locationLongitude,
        @NotNull UUID vehicleAssignmentId,
        @NotNull UUID issueTypeId
) { }

