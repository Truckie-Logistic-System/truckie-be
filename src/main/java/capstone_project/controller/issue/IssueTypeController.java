package capstone_project.controller.issue;

import capstone_project.dtos.request.issue.CreateIssueTypeRequest;
import capstone_project.dtos.request.issue.UpdateIssueTypeRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.issue.GetIssueTypeResponse;
import capstone_project.service.services.issue.IssueTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${issue-type.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class IssueTypeController {
    private final IssueTypeService issueTypeService;

    // Get all IssueTypes
    @GetMapping("")
    public ResponseEntity<ApiResponse<List<GetIssueTypeResponse>>> getAllIssueTypes() {
        final var result = issueTypeService.getAllIssueType();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get IssueType by ID
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<GetIssueTypeResponse>> getIssueTypeById(@PathVariable("id") UUID id) {
        final var result = issueTypeService.getIssueTypeById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Search IssueType by name (contains)
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<GetIssueTypeResponse>>> getIssueTypeByName(@RequestParam String name) {
        final var result = issueTypeService.getIssueTypeContainNameResponseList(name);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Create new IssueType (only ADMIN)
    @PreAuthorize("hasAnyAuthority('ADMIN','STAFF')")
    @PostMapping("")
    public ResponseEntity<ApiResponse<GetIssueTypeResponse>> createIssueType(
            @RequestBody @Valid CreateIssueTypeRequest createIssueTypeRequest) {
        final var result = issueTypeService.createIssueType(createIssueTypeRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    // Update IssueType (only ADMIN)
    @PreAuthorize("hasAnyAuthority('ADMIN','STAFF')")
    @PutMapping("")
    public ResponseEntity<ApiResponse<GetIssueTypeResponse>> updateIssueType(
            @RequestBody @Valid UpdateIssueTypeRequest updateIssueTypeRequest) {
        final var result = issueTypeService.updateIssueType(updateIssueTypeRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

}
