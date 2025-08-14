package capstone_project.controller.vehicle;

import capstone_project.dtos.request.vehicle.UpdateVehicleMaintenanceRequest;
import capstone_project.dtos.request.vehicle.VehicleMaintenanceRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.vehicle.VehicleMaintenanceResponse;
import capstone_project.service.services.vehicle.VehicleMaintenanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${vehicle-maintenance.api.base-path}")
@RequiredArgsConstructor
@Validated
public class VehicleMaintenanceController {

    private final VehicleMaintenanceService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleMaintenanceResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllMaintenance()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleMaintenanceResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getMaintenanceById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleMaintenanceResponse>> create(
            @RequestBody @Valid VehicleMaintenanceRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.createMaintenance(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleMaintenanceResponse>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateVehicleMaintenanceRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateMaintenance(id, req)));
    }
}