package capstone_project.controller.vehicle;

import capstone_project.dtos.request.vehicle.BatchUpdateLocationRequest;
import capstone_project.dtos.request.vehicle.BulkVehicleGenerationRequest;
import capstone_project.dtos.request.vehicle.UpdateLocationRequest;
import capstone_project.dtos.request.vehicle.UpdateVehicleRequest;
import capstone_project.dtos.request.vehicle.VehicleRequest;
import capstone_project.dtos.response.vehicle.VehicleGetDetailsResponse;
import capstone_project.dtos.response.vehicle.VehicleResponse;
import capstone_project.service.services.vehicle.VehicleService;
import capstone_project.dtos.response.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${vehicle.api.base-path}")
@RequiredArgsConstructor
@Validated
public class VehicleController {

    private final VehicleService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllVehicles()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleGetDetailsResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getVehicleById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleResponse>> create(
            @RequestBody @Validated VehicleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.createVehicle(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleResponse>> update(
            @PathVariable UUID id,
            @RequestBody @Validated UpdateVehicleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateVehicle(id, req)));
    }

    /**
     * Update vehicle location. This is the basic update method.
     */
    @PutMapping("/{id}/location")
    public ResponseEntity<ApiResponse<Void>> updateLocation(
            @PathVariable UUID id,
            @RequestBody @Validated UpdateLocationRequest req) {
        service.updateVehicleLocation(id, req);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    /**
     * Update vehicle location with rate limiting - ensures updates aren't processed
     * too frequently for a single vehicle.
     *
     * @param id Vehicle ID
     * @param seconds Minimum seconds between updates (default: 5)
     */
    @PutMapping("/{id}/location/rate-limited")
    public ResponseEntity<ApiResponse<Boolean>> updateLocationWithRateLimit(
            @PathVariable UUID id,
            @RequestBody @Validated UpdateLocationRequest req,
            @RequestParam(defaultValue = "5") int seconds) {
        boolean updated = service.updateVehicleLocationWithRateLimit(id, req, seconds);
        return ResponseEntity.ok(ApiResponse.ok(updated));
    }

    /**
     * Update locations of multiple vehicles in a single request.
     * More efficient for clients that need to update multiple vehicle positions at once.
     */
    @PutMapping("/locations/batch")
    public ResponseEntity<ApiResponse<Integer>> updateLocationsInBatch(
            @RequestBody @Validated BatchUpdateLocationRequest req) {
        int updatedCount = service.updateVehicleLocationsInBatch(req);
        return ResponseEntity.ok(ApiResponse.ok(updatedCount));
    }

    /**
     * Generate multiple vehicles in a single operation
     *
     * @param request The request containing the count of vehicles to generate
     * @return List of created vehicle responses
     */
    @PostMapping("/generate-bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<VehicleResponse>>> generateBulkVehicles(@RequestBody @Validated BulkVehicleGenerationRequest request) {
        final var vehicles = service.generateBulkVehicles(request.getCount());
        return ResponseEntity.ok(ApiResponse.ok(vehicles));
    }

//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
//        service.deleteVehicle(id);
//        return ResponseEntity.ok(ApiResponse.ok());
//    }
}