package capstone_project.controller.pricing;

import capstone_project.dtos.request.pricing.DistanceRuleRequest;
import capstone_project.dtos.request.pricing.UpdateDistanceRuleRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.pricing.DistanceRuleResponse;
import capstone_project.service.services.pricing.DistanceRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${distance-rule.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DistanceRuleController {

    private final DistanceRuleService distanceRuleService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<DistanceRuleResponse>>> getAllPricingTiers() {
        final var result = distanceRuleService.getAllDistanceRules();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DistanceRuleResponse>> getPricingTierById(@PathVariable("id") UUID id) {
        final var result = distanceRuleService.getDistanceRuleById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping()
    public ResponseEntity<ApiResponse<DistanceRuleResponse>> createPricingRule(@RequestBody @Valid DistanceRuleRequest distanceRuleRequest) {
        final var result = distanceRuleService.createDistanceRule(distanceRuleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DistanceRuleResponse>> updatePricingRule(@PathVariable("id") UUID id, @RequestBody @Valid UpdateDistanceRuleRequest distanceRuleRequest) {
        final var result = distanceRuleService.updateDistanceRule(id, distanceRuleRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePricingRule(@PathVariable("id") UUID id) {
        distanceRuleService.deleteDistanceRule(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

}
