package capstone_project.controller.pricing;

import capstone_project.dtos.request.pricing.UpdateVehicleRuleRequest;
import capstone_project.dtos.request.pricing.VehicleRuleRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.pricing.FullVehicleRuleResponse;
import capstone_project.dtos.response.pricing.VehicleRuleResponse;
import capstone_project.service.services.pricing.VehicleRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${vehicle-rule.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class VehicleRuleController {

    private final VehicleRuleService vehicleRuleService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<VehicleRuleResponse>>> getAllVehicleRules() {
        final var result = vehicleRuleService.getAllVehicleRules();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/full")
    public ResponseEntity<ApiResponse<List<FullVehicleRuleResponse>>> getAllFullVehicleRules() {
        final var result = vehicleRuleService.getAllFullVehicleRules();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleRuleResponse>> getVehicleRuleById(@PathVariable("id") UUID id) {
        final var result = vehicleRuleService.getVehicleRuleById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}/full")
    public ResponseEntity<ApiResponse<FullVehicleRuleResponse>> getFullVehicleRuleById(@PathVariable("id") UUID id) {
        final var result = vehicleRuleService.getFullVehicleRuleById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping()
    public ResponseEntity<ApiResponse<VehicleRuleResponse>> createVehicleRule(@RequestBody @Valid VehicleRuleRequest vehicleRuleRequest) {
        final var result = vehicleRuleService.createVehicleRule(vehicleRuleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleRuleResponse>> updateVehicleRule(
            @PathVariable("id") UUID id,
            @RequestBody @Valid UpdateVehicleRuleRequest updateVehicleRuleRequest) {
        final var result = vehicleRuleService.updateVehicleRule(id, updateVehicleRuleRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
