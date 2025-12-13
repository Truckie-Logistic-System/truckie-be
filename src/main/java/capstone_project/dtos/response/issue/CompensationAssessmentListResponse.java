package capstone_project.dtos.response.issue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for listing compensation assessments for staff
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompensationAssessmentListResponse {
    private UUID id;
    private String issueType;
    private Boolean hasDocuments;
    private BigDecimal documentValue;
    private BigDecimal estimatedMarketValue;
    private BigDecimal assessmentRate;
    private BigDecimal compensationByPolicy;
    private BigDecimal finalCompensation;
    private Boolean fraudDetected;
    private String fraudReason;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Issue info
    private IssueInfo issue;
    
    // Staff who created
    private StaffInfo createdByStaff;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IssueInfo {
        private UUID id;
        private String issueTypeName;
        private String issueCategory;
        private String status;
        private String description;
        private LocalDateTime reportedAt;
        private LocalDateTime resolvedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffInfo {
        private UUID id;
        private String fullName;
        private String email;
    }
}
