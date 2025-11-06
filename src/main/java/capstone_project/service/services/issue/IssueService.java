package capstone_project.service.services.issue;

import capstone_project.dtos.request.issue.*;
import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import capstone_project.dtos.response.issue.GetIssueTypeResponse;

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

}
