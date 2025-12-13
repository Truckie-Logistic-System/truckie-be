package capstone_project.controller.vehicle;

import capstone_project.dtos.request.vehicle.CreateFuelTypeRequest;
import capstone_project.dtos.request.vehicle.UpdateFuelTypeRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.vehicle.FuelTypeResponse;
import capstone_project.service.services.vehicle.FuelTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fuel-types")
@RequiredArgsConstructor
@Slf4j
public class FuelTypeController {

    private final FuelTypeService fuelTypeService;

    /**
     * Get all fuel types (sorted by createdAt DESC)
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FuelTypeResponse>>> getAllFuelTypes() {
        log.info("[FuelTypeController] Getting all fuel types");
        List<FuelTypeResponse> result = fuelTypeService.getAllFuelTypes();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Get fuel type by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<FuelTypeResponse>> getFuelTypeById(@PathVariable UUID id) {
        log.info("[FuelTypeController] Getting fuel type by ID: {}", id);
        FuelTypeResponse result = fuelTypeService.getFuelTypeById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Search fuel types by name
     */
    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<List<FuelTypeResponse>>> searchFuelTypesByName(
            @RequestParam String name) {
        log.info("[FuelTypeController] Searching fuel types by name: {}", name);
        List<FuelTypeResponse> result = fuelTypeService.searchFuelTypesByName(name);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Create a new fuel type
     */
    @PostMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<FuelTypeResponse>> createFuelType(
            @Valid @RequestBody CreateFuelTypeRequest request) {
        log.info("[FuelTypeController] Creating new fuel type: {}", request.getName());
        FuelTypeResponse result = fuelTypeService.createFuelType(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Update an existing fuel type
     */
    @PutMapping
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<FuelTypeResponse>> updateFuelType(
            @Valid @RequestBody UpdateFuelTypeRequest request) {
        log.info("[FuelTypeController] Updating fuel type: {}", request.getId());
        FuelTypeResponse result = fuelTypeService.updateFuelType(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * Delete a fuel type
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('STAFF', 'ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteFuelType(@PathVariable UUID id) {
        log.info("[FuelTypeController] Deleting fuel type: {}", id);
        fuelTypeService.deleteFuelType(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
