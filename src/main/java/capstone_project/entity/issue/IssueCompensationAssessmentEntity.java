package capstone_project.entity.issue;

import capstone_project.entity.auth.UserEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Unified compensation assessment entity for both DAMAGE and OFF_ROUTE issues.
 * 
 * Key design decisions:
 * - Use `assessmentRate` for both damage rate (DAMAGE) and loss rate (OFF_ROUTE)
 * - Include `estimatedMarketValue` for cases without documents (especially OFF_ROUTE)
 * - Separate notes fields for different purposes (staff notes vs adjustment reasons)
 * - Support both partial loss (DAMAGE) and full loss (OFF_ROUTE) scenarios
 */
@Entity
@Table(name = "issue_compensation_assessment")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueCompensationAssessmentEntity {
    
    @Id
    @GeneratedValue(generator = "UUID")
    @GenericGenerator(name = "UUID", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false, unique = true)
    private IssueEntity issue;
    
    // Issue type for context (DAMAGE, OFF_ROUTE, etc.)
    @Column(name = "issue_type", nullable = false, length = 50)
    private String issueType;
    
    // === DOCUMENT & VALUE ASSESSMENT ===
    
    @Builder.Default
    @Column(name = "has_documents", nullable = false)
    private Boolean hasDocuments = false;
    
    @Column(name = "document_value", precision = 15, scale = 2)
    private BigDecimal documentValue;
    
    // For cases without documents (especially important for OFF_ROUTE)
    @Column(name = "estimated_market_value", precision = 15, scale = 2)
    private BigDecimal estimatedMarketValue;
    
    // === LOSS/DAMAGE ASSESSMENT ===
    
    /**
     * Assessment rate (0.0 - 1.0):
     * - For DAMAGE: actual damage rate (e.g., 0.3 = 30% damaged)
     * - For OFF_ROUTE: typically 1.0 (100% loss due to runaway/theft)
     */
    @Builder.Default
    @Column(name = "assessment_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal assessmentRate = BigDecimal.ZERO;
    
    // === COMPENSATION CALCULATION ===
    
    @Column(name = "compensation_by_policy", nullable = false, precision = 15, scale = 2)
    private BigDecimal compensationByPolicy;
    
    @Column(name = "final_compensation", nullable = false, precision = 15, scale = 2)
    private BigDecimal finalCompensation;
    
    // === NOTES & DOCUMENTATION ===
    
    // General staff notes about the assessment
    @Column(name = "staff_notes", columnDefinition = "TEXT")
    private String staffNotes;
    
    // Specific reason for adjusting compensation amount (audit trail)
    @Column(name = "adjust_reason", columnDefinition = "TEXT")
    private String adjustReason;
    
    // Handler notes for operational context (contacts, evidence, etc.)
    @Column(name = "handler_notes", columnDefinition = "TEXT")
    private String handlerNotes;
    
    // === DOCUMENT EVIDENCE ===
    
    /**
     * List of document image URLs uploaded by staff as evidence
     * Stored as comma-separated string for simplicity
     */
    @Column(name = "document_images", columnDefinition = "TEXT")
    private String documentImages;
    
    // === FRAUD DETECTION ===
    
    @Builder.Default
    @Column(name = "fraud_detected", nullable = false)
    private Boolean fraudDetected = false;
    
    @Column(name = "fraud_reason", columnDefinition = "TEXT")
    private String fraudReason;
    
    // === AUDIT TRAIL ===
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private UserEntity createdBy;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // === HELPER METHODS ===
    
    /**
     * Check if this is a damage assessment (partial loss)
     */
    public boolean isDamageAssessment() {
        return "DAMAGE".equalsIgnoreCase(issueType);
    }
    
    /**
     * Check if this is an off-route assessment (typically full loss)
     */
    public boolean isOffRouteAssessment() {
        return "OFF_ROUTE".equalsIgnoreCase(issueType) || "RUNAWAY".equalsIgnoreCase(issueType);
    }
    
    /**
     * Get the assessment rate as percentage (for display)
     */
    public BigDecimal getAssessmentRatePercent() {
        return assessmentRate != null ? assessmentRate.multiply(new BigDecimal("100")) : BigDecimal.ZERO;
    }
    
    /**
     * Set assessment rate from percentage (0-100)
     */
    public void setAssessmentRateFromPercent(BigDecimal percent) {
        this.assessmentRate = percent != null ? percent.divide(new BigDecimal("100")) : BigDecimal.ZERO;
    }
}
