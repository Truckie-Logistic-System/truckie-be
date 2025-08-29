package capstone_project.controller.issue;

import capstone_project.dtos.request.issue.CreateIssueImageRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.issue.GetIssueImageResponse;
import capstone_project.service.services.issue.IssueImageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${issue-image.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class IssueImageController {
    private final IssueImageService issueImageService;

    @PostMapping
    public ResponseEntity<ApiResponse<GetIssueImageResponse>> createImage(
            @RequestBody CreateIssueImageRequest request) {
        final var result = issueImageService.createImage(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    // Get Issue Images by IssueId
    @GetMapping("/{issueId}")
    public ResponseEntity<ApiResponse<GetIssueImageResponse>> getImages(
            @PathVariable UUID issueId) {
        final var result = issueImageService.getImage(issueId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
