package capstone_project.controller.pricing;

import capstone_project.dtos.request.pricing.UpdateSizeRuleRequest;
import capstone_project.dtos.request.pricing.SizeRuleRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.pricing.FullSizeRuleResponse;
import capstone_project.dtos.response.pricing.SizeRuleResponse;
import capstone_project.service.services.pricing.SizeRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${size-rule.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SizeRuleController {

    private final SizeRuleService sizeRuleService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<SizeRuleResponse>>> getAllsizeRules() {
        final var result = sizeRuleService.getAllsizeRules();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/full")
    public ResponseEntity<ApiResponse<List<FullSizeRuleResponse>>> getAllFullsizeRules() {
        final var result = sizeRuleService.getAllFullsizeRules();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SizeRuleResponse>> getsizeRuleById(@PathVariable("id") UUID id) {
        final var result = sizeRuleService.getsizeRuleById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}/full")
    public ResponseEntity<ApiResponse<FullSizeRuleResponse>> getFullsizeRuleById(@PathVariable("id") UUID id) {
        final var result = sizeRuleService.getFullsizeRuleById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @PostMapping()
    public ResponseEntity<ApiResponse<SizeRuleResponse>> createsizeRule(@RequestBody @Valid SizeRuleRequest sizeRuleRequest) {
        final var result = sizeRuleService.createsizeRule(sizeRuleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SizeRuleResponse>> updatesizeRule(
            @PathVariable("id") UUID id,
            @RequestBody @Valid UpdateSizeRuleRequest updatesizeRuleRequest) {
        final var result = sizeRuleService.updateSizeRule(id, updatesizeRuleRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
