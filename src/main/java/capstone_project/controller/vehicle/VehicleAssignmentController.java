package capstone_project.controller.vehicle;

import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${vehicle-assignment.api.base-path}")
@RequiredArgsConstructor
@Validated
public class VehicleAssignmentController {

    private final VehicleAssignmentService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleAssignmentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllAssignments()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleAssignmentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAssignmentById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleAssignmentResponse>> create(
            @RequestBody @Valid VehicleAssignmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.createAssignment(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleAssignmentResponse>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateVehicleAssignmentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateAssignment(id, req)));
    }
}