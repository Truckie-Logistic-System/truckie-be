package capstone_project.service.services.issue;

import capstone_project.dtos.request.issue.*;
import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import capstone_project.dtos.response.issue.GetIssueTypeResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface IssueService {
    GetBasicIssueResponse getBasicIssue(UUID issueId);

    GetBasicIssueResponse getByVehicleAssignment(UUID vehicleAssignmentId);

    List<GetBasicIssueResponse> getByStaffId(UUID staffId);

    List<GetBasicIssueResponse> getByActiveStatus();

    List<GetBasicIssueResponse> getIssueType(UUID issueTypeId);

    GetBasicIssueResponse createIssue(CreateBasicIssueRequest request);

    GetBasicIssueResponse updateIssue(UpdateBasicIssueRequest request);

    GetBasicIssueResponse updateStaffForIssue(UUID staffId, UUID issueId);

    List<GetBasicIssueResponse> getInactiveStatus();

    List<GetBasicIssueResponse> getAllIssues();

    /**
     * Resolve issue and restore order detail statuses
     * @param issueId Issue ID to resolve
     * @return Updated issue
     */
    GetBasicIssueResponse resolveIssue(UUID issueId);

    /**
     * Update issue status directly (for PENALTY and other simple issues)
     * @param issueId Issue ID
     * @param status New status (OPEN, IN_PROGRESS, RESOLVED)
     * @return Updated issue
     */
    GetBasicIssueResponse updateIssueStatus(UUID issueId, String status);

    /**
     * Report seal removal issue (Driver)
     * @param request Report seal issue request
     * @return Created issue
     */
    GetBasicIssueResponse reportSealIssue(ReportSealIssueRequest request);

    /**
     * Assign new seal to replace removed seal (Staff)
     * @param request Assign new seal request
     * @return Updated issue
     */
    GetBasicIssueResponse assignNewSeal(AssignNewSealRequest request);

    /**
     * Confirm new seal attachment (Driver)
     * @param request Confirm new seal request
     * @return Updated issue
     */
    GetBasicIssueResponse confirmNewSeal(ConfirmNewSealRequest request);

    /**
     * Get IN_USE seal for a vehicle assignment (for driver to report seal removal)
     * @param vehicleAssignmentId Vehicle assignment ID
     * @return Seal that is currently IN_USE
     */
    capstone_project.dtos.response.order.seal.GetSealResponse getInUseSealByVehicleAssignment(UUID vehicleAssignmentId);

    /**
     * Get available ACTIVE seals for a vehicle assignment (for staff to choose)
     * @param vehicleAssignmentId Vehicle assignment ID
     * @return List of ACTIVE seals
     */
    List<capstone_project.dtos.response.order.seal.GetSealResponse> getActiveSealsByVehicleAssignment(UUID vehicleAssignmentId);

    /**
     * Get pending seal replacement issues for a vehicle assignment (for driver to confirm)
     * Returns issues with status IN_PROGRESS, category SEAL_REPLACEMENT, and newSeal assigned
     * @param vehicleAssignmentId Vehicle assignment ID
     * @return List of pending seal replacement issues
     */
    List<GetBasicIssueResponse> getPendingSealReplacementsByVehicleAssignment(UUID vehicleAssignmentId);

    /**
     * Report damaged goods issue (Driver)
     * @param request Report damage issue request
     * @return Created issue
     */
    GetBasicIssueResponse reportDamageIssue(ReportDamageIssueRequest request);

    /**
     * Report traffic penalty violation issue (Driver)
     * @param vehicleAssignmentId Vehicle assignment ID
     * @param issueTypeId Issue type ID
     * @param violationType Type of traffic violation
     * @param violationImage Traffic violation record image
     * @param locationLatitude Location latitude
     * @param locationLongitude Location longitude
     * @return Created issue
     */
    GetBasicIssueResponse reportPenaltyIssue(
            UUID vehicleAssignmentId,
            UUID issueTypeId,
            String violationType,
            MultipartFile violationImage,
            Double locationLatitude,
            Double locationLongitude
    );

    // ===== ORDER_REJECTION flow methods =====

    /**
     * Report order rejection by recipient (Driver)
     * @param request Report order rejection request
     * @return Created issue
     */
    GetBasicIssueResponse reportOrderRejection(capstone_project.dtos.request.issue.ReportOrderRejectionRequest request);

    /**
     * Calculate return shipping fee for ORDER_REJECTION issue (Staff)
     * @param issueId Issue ID
     * @return Return shipping fee details
     */
    capstone_project.dtos.response.issue.ReturnShippingFeeResponse calculateReturnShippingFee(UUID issueId);

    /**
     * Calculate return shipping fee with custom distance (from route with intermediate points)
     * @param issueId Issue ID
     * @param actualDistanceKm Actual distance from delivery to pickup including intermediate points
     * @return Return shipping fee details
     */
    capstone_project.dtos.response.issue.ReturnShippingFeeResponse calculateReturnShippingFee(UUID issueId, java.math.BigDecimal actualDistanceKm);

    /**
     * Process ORDER_REJECTION issue: create transaction, set up new route (Staff)
     * @param request Process order rejection request
     * @return Order rejection detail
     */
    capstone_project.dtos.response.issue.OrderRejectionDetailResponse processOrderRejection(
            capstone_project.dtos.request.issue.ProcessOrderRejectionRequest request
    );

    /**
     * Get ORDER_REJECTION issue detail (Staff/Customer)
     * @param issueId Issue ID
     * @return Order rejection detail
     */
    capstone_project.dtos.response.issue.OrderRejectionDetailResponse getOrderRejectionDetail(UUID issueId);

    /**
     * Driver confirms return delivery at pickup location with photos
     * Updates issue status to RESOLVED and journey to COMPLETED
     * @param request Confirm return delivery request
     * @return Updated issue
     */
    GetBasicIssueResponse confirmReturnDelivery(capstone_project.dtos.request.issue.ConfirmReturnDeliveryRequest request);

    /**
     * Customer creates return shipping payment transaction
     * Called when customer clicks "Pay" button for return shipping fee
     * @param issueId Issue ID
     * @return Transaction response with PayOS checkout URL
     */
    capstone_project.dtos.response.order.transaction.TransactionResponse createReturnPaymentTransaction(UUID issueId);

}
