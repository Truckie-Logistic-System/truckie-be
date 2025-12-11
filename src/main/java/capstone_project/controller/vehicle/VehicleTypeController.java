package capstone_project.controller.vehicle;

import capstone_project.dtos.request.vehicle.VehicleTypeRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.vehicle.VehicleTypeResponse;
import capstone_project.service.services.vehicle.VehicleTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${vehicle-type.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class VehicleTypeController {
    private final VehicleTypeService vehicleTypeService;

    @GetMapping("")
    public ResponseEntity<ApiResponse<List<VehicleTypeResponse>>> getAllVehicleTypes() {
        final var result = vehicleTypeService.getAllVehicleTypes();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> getVehicleTypeById(@PathVariable("id") UUID id) {
        final var result = vehicleTypeService.getVehicleTypeById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> createVehicleType(@RequestBody @Valid VehicleTypeRequest vehicleTypeRequest) {
        final var result = vehicleTypeService.createVehicleType(vehicleTypeRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleTypeResponse>> updateVehicleType(
            @PathVariable("id") UUID id,
            @RequestBody @Valid VehicleTypeRequest vehicleTypeRequest) {
        final var result = vehicleTypeService.updateVehicleType(id, vehicleTypeRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PreAuthorize("hasAuthority('ADMIN')")
    @PostMapping("/assign-default-fuel-types")
    public ResponseEntity<ApiResponse<List<VehicleTypeResponse>>> assignDefaultFuelTypesForTruckRange() {
        final var result = vehicleTypeService.assignDefaultFuelTypesForTruckRange();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
