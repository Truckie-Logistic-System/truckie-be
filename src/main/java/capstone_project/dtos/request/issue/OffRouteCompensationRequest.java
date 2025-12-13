package capstone_project.dtos.request.issue;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for updating OFF_ROUTE compensation assessment
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OffRouteCompensationRequest {
    
    @NotNull(message = "Issue ID is required")
    @JsonProperty("issueId")
    private UUID issueId;
    
    // Document assessment fields
    @NotNull(message = "hasDocuments is required")
    @JsonProperty("hasDocuments")
    private Boolean hasDocuments;
    
    @JsonProperty("documentValue")
    @DecimalMin(value = "0.0", inclusive = false, message = "Document value must be greater than 0")
    private BigDecimal documentValue;
    
    @JsonProperty("estimatedMarketValue")
    @DecimalMin(value = "0.0", inclusive = false, message = "Estimated market value must be greater than 0")
    private BigDecimal estimatedMarketValue;
    
    // Assessment rate (for partial loss - typically 100% for runaway cases)
    @NotNull(message = "Assessment rate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Assessment rate must be at least 0")
    @DecimalMax(value = "1.0", inclusive = true, message = "Assessment rate must not exceed 1")
    @JsonProperty("assessmentRate")
    private BigDecimal assessmentRate;
    
    // Final compensation decision
    @NotNull(message = "Final compensation is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Final compensation must be greater than 0")
    @JsonProperty("finalCompensation")
    private BigDecimal finalCompensation;
    
    // Staff notes
    @JsonProperty("adjustReason")
    private String adjustReason;
    
    @JsonProperty("handlerNotes")
    private String handlerNotes;
    
    // Fraud detection
    @JsonProperty("fraudDetected")
    private Boolean fraudDetected;
    
    @JsonProperty("fraudReason")
    private String fraudReason;
    
    // Refund information
    @JsonProperty("refund")
    private RefundRequest refund;
    
    /**
     * Nested class for refund information
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRequest {
        @NotNull(message = "Refund amount is required")
        @JsonProperty("refundAmount")
        @DecimalMin(value = "0.0", inclusive = false, message = "Refund amount must be greater than 0")
        private BigDecimal refundAmount;
        
        @JsonProperty("bankName")
        @NotBlank(message = "Bank name is required")
        @Size(max = 200, message = "Bank name must not exceed 200 characters")
        private String bankName;
        
        @JsonProperty("accountNumber")
        @NotBlank(message = "Account number is required")
        @Size(max = 100, message = "Account number must not exceed 100 characters")
        private String accountNumber;
        
        @JsonProperty("accountHolderName")
        @NotBlank(message = "Account holder name is required")
        @Size(max = 200, message = "Account holder name must not exceed 200 characters")
        private String accountHolderName;
        
        @JsonProperty("transactionCode")
        @Size(max = 100, message = "Transaction code must not exceed 100 characters")
        private String transactionCode;
        
        @JsonProperty("bankTransferImage")
        @Size(max = 500, message = "Bank transfer image URL must not exceed 500 characters")
        private String bankTransferImage;
        
        @JsonProperty("notes")
        @Size(max = 500, message = "Notes must not exceed 500 characters")
        private String notes;
    }
}
