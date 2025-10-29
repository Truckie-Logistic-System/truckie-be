package capstone_project.controller.pricing;

import capstone_project.dtos.request.pricing.UpdateVehicleTypeRuleRequest;
import capstone_project.dtos.request.pricing.VehicleTypeRuleRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.pricing.FullVehicleTypeRuleResponse;
import capstone_project.dtos.response.pricing.VehicleTypeRuleResponse;
import capstone_project.service.services.pricing.VehicleTypeRuleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${vehicle-type-rule.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class VehicleTypeRuleController {

    private final VehicleTypeRuleService vehicleTypeRuleService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<VehicleTypeRuleResponse>>> getAllVehicleTypeRules() {
        final var result = vehicleTypeRuleService.getAllVehicleTypeRules();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/full")
    public ResponseEntity<ApiResponse<List<FullVehicleTypeRuleResponse>>> getAllFullVehicleTypeRules() {
        final var result = vehicleTypeRuleService.getAllFullVehicleTypeRules();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleTypeRuleResponse>> getVehicleTypeRuleById(@PathVariable("id") UUID id) {
        final var result = vehicleTypeRuleService.getVehicleTypeRuleById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}/full")
    public ResponseEntity<ApiResponse<FullVehicleTypeRuleResponse>> getFullVehicleTypeRuleById(@PathVariable("id") UUID id) {
        final var result = vehicleTypeRuleService.getFullVehicleTypeRuleById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping()
    public ResponseEntity<ApiResponse<VehicleTypeRuleResponse>> createVehicleTypeRule(@RequestBody @Valid VehicleTypeRuleRequest vehicleTypeRuleRequest) {
        final var result = vehicleTypeRuleService.createVehicleTypeRule(vehicleTypeRuleRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleTypeRuleResponse>> updateVehicleTypeRule(
            @PathVariable("id") UUID id,
            @RequestBody @Valid UpdateVehicleTypeRuleRequest updateVehicleTypeRuleRequest) {
        final var result = vehicleTypeRuleService.updateVehicleTypeRule(id, updateVehicleTypeRuleRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
