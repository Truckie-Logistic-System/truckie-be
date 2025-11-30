package capstone_project.dtos.request.order.contract;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for generating contract PDF on backend
 * Unlike ContractFileUploadRequest, this doesn't require a file upload
 * as the PDF will be generated server-side
 */
public record GenerateContractPdfRequest(
        @NotNull(message = "Contract ID is required")
        UUID contractId,
        @NotNull(message = "Contract name is required")
        @Size(max = 255)
        String contractName,
        @NotNull(message = "Effective date is required")
        LocalDateTime effectiveDate,
        @NotNull(message = "Expiration date is required")
        LocalDateTime expirationDate,
        @NotNull(message = "Adjusted value is required")
        @Min(value = 0)
        BigDecimal adjustedValue,
        @Size(max = 1000)
        String description
) {
}
