package capstone_project.dtos.request.order.contract;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ContractFileUploadRequest(
        @NotNull(message = "File is required")
        MultipartFile file,
        @NotNull(message = "Contract ID is required")
        UUID contractId,
        @NotNull(message = "Contract name is required")
        @Size(max = 255)
        String contractName,
        @NotNull(message = "Effective date is required")
        LocalDateTime effectiveDate,
        @NotNull(message = "Expiration date is required")
        LocalDateTime  expirationDate,
        @NotNull(message = "Supported value is required")
        @Min(value = 0)
        BigDecimal adjustedValue,
        @Size(max = 1000)
        String description
        ) {
}