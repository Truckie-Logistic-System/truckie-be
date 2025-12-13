package capstone_project.dtos.request.issue;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Unified request DTO for both DAMAGE and OFF_ROUTE compensation assessments
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationAssessmentRequest {
    
    @NotNull(message = "Issue ID is required")
    @JsonProperty("issueId")
    private UUID issueId;
    
    @NotNull(message = "Issue type is required")
    @JsonProperty("issueType")
    private String issueType;
    
    // Document assessment
    @JsonProperty("hasDocuments")
    private Boolean hasDocuments;
    
    @JsonProperty("documentValue")
    private BigDecimal documentValue;
    
    @JsonProperty("estimatedMarketValue")
    private BigDecimal estimatedMarketValue;
    
    // Document evidence
    @JsonProperty("documentImages")
    private List<String> documentImages;
    
    // Assessment rate (damage rate for DAMAGE, typically 1.0 for OFF_ROUTE)
    @DecimalMin(value = "0.0", message = "Assessment rate must be between 0 and 1")
    @DecimalMax(value = "1.0", message = "Assessment rate must be between 0 and 1")
    @JsonProperty("assessmentRate")
    private BigDecimal assessmentRate;
    
    // For DAMAGE: assessmentRate is provided as percentage (0-100)
    @JsonProperty("assessmentRatePercent")
    private BigDecimal assessmentRatePercent;
    
    // Compensation
    @JsonProperty("finalCompensation")
    private BigDecimal finalCompensation;
    
    // Notes
    @JsonProperty("staffNotes")
    private String staffNotes;
    
    @JsonProperty("adjustReason")
    private String adjustReason;
    
    @JsonProperty("handlerNotes")
    private String handlerNotes;
    
    // Fraud detection
    @JsonProperty("fraudDetected")
    private Boolean fraudDetected;
    
    @JsonProperty("fraudReason")
    private String fraudReason;
    
    // Refund information (if applicable)
    @JsonProperty("refund")
    private RefundRequest refund;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RefundRequest {
        @JsonProperty("createOrUpdate")
        private Boolean createOrUpdate;
        
        @JsonProperty("refundAmount")
        private BigDecimal refundAmount;
        
        @JsonProperty("bankName")
        private String bankName;
        
        @JsonProperty("accountNumber")
        private String accountNumber;
        
        @JsonProperty("accountHolderName")
        private String accountHolderName;
        
        @JsonProperty("transactionCode")
        private String transactionCode;
        
        @JsonProperty("bankTransferImage")
        private String bankTransferImage;
        
        @JsonProperty("notes")
        private String notes;
    }
}
