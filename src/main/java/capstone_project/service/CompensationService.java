package capstone_project.service;

import capstone_project.dtos.request.issue.CompensationAssessmentRequest;
import capstone_project.dtos.response.issue.CompensationAssessmentListResponse;
import capstone_project.dtos.response.issue.CompensationDetailResponse;

import java.util.List;
import java.util.UUID;

public interface CompensationService {
    
    /**
     * Get all compensation assessments for staff (sorted by createdAt DESC)
     * @return List of CompensationAssessmentListResponse
     */
    List<CompensationAssessmentListResponse> getAllCompensationAssessments();
    
    /**
     * Get compensation detail for an issue
     * @param issueId Issue ID
     * @return CompensationDetailResponse
     */
    CompensationDetailResponse getCompensationDetail(UUID issueId);
    
    /**
     * Resolve compensation for an issue
     * @param request CompensationAssessmentRequest
     * @return CompensationDetailResponse
     */
    CompensationDetailResponse resolveCompensation(CompensationAssessmentRequest request);
    
    /**
     * Upload document images for compensation assessment
     * @param issueId Issue ID
     * @param imageUrls List of image URLs
     * @return CompensationDetailResponse
     */
    CompensationDetailResponse uploadDocumentImages(UUID issueId, String[] imageUrls);
    
    /**
     * Calculate compensation preview without saving
     * Used for realtime UI updates when staff changes assessment fields
     * @param issueId Issue ID
     * @param previewRequest Assessment data for preview calculation
     * @return CompensationBreakdown with calculated values
     */
    CompensationDetailResponse.CompensationBreakdown calculatePreview(
            UUID issueId, 
            CompensationAssessmentRequest previewRequest);
}
