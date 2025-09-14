package capstone_project.controller.issue;

import capstone_project.dtos.request.issue.AssignStaffForIssueRequest;
import capstone_project.dtos.request.issue.CreateBasicIssueRequest;
import capstone_project.dtos.request.issue.UpdateBasicIssueRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import capstone_project.service.services.issue.IssueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${issue.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class IssuesController {
    private final IssueService issueService;

    // Get Issue by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> getIssueById(@PathVariable("id") UUID id) {
        final var result = issueService.getBasicIssue(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get Issue by Vehicle Assignment
    @GetMapping("/vehicle/{vehicleAssignmentId}")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> getByVehicleAssignment(
            @PathVariable("vehicleAssignmentId") UUID vehicleAssignmentId) {
        final var result = issueService.getByVehicleAssignment(vehicleAssignmentId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get Issues by Staff
    @GetMapping("/staff/{staffId}")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getByStaff(@PathVariable("staffId") UUID staffId) {
        final var result = issueService.getByStaffId(staffId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get Issues by active status (OPEN)
    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getActiveIssues() {
        final var result = issueService.getByActiveStatus();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get Issue by IssueType ID
    @GetMapping("/type/{issueTypeId}")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getByIssueType(@PathVariable("issueTypeId") UUID issueTypeId) {
        final var result = issueService.getIssueType(issueTypeId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Create Issue
    @PostMapping("")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> createIssue(@RequestBody CreateBasicIssueRequest request) {
        final var result = issueService.createIssue(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Update Issue
    @PutMapping("")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> updateIssue(@RequestBody UpdateBasicIssueRequest request) {
        final var result = issueService.updateIssue(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/assign-staff")
    public ResponseEntity<ApiResponse<GetBasicIssueResponse>> assignStaffForIssue(@RequestBody AssignStaffForIssueRequest request) {
        final var result = issueService.updateStaffForIssue(request.staffId(),request.issueId());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/inactive")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getInactiveIssues() {
        final var result = issueService.getInactiveStatus();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/get-all")
    public ResponseEntity<ApiResponse<List<GetBasicIssueResponse>>> getAll() {
        final var result = issueService.getAllIssues();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
