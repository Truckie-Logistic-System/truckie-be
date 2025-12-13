package capstone_project.controller.vehicle;

import capstone_project.dtos.request.vehicle.GroupedAssignmentRequest;
import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.StaffVehicleAssignmentFullResponse;
import capstone_project.dtos.response.vehicle.GroupedVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.SampleVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.SimplifiedVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
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

    /**
     * Get full vehicle assignment details for staff view
     * Includes: vehicle, drivers, penalties, fuel consumption, seals, journey histories, photo completions, issues
     */
    @GetMapping("/{id}/full")
    public ResponseEntity<ApiResponse<StaffVehicleAssignmentFullResponse>> getFullById(@PathVariable UUID id) {
        log.info("🔍 DEBUG: Getting full vehicle assignment details for ID: {}", id);
        StaffVehicleAssignmentFullResponse response = service.getFullAssignmentById(id);
        log.info("🔍 DEBUG: Response contains {} devices", response.devices().size());
        if (!response.devices().isEmpty()) {
            log.info("🔍 DEBUG: Device codes in response: {}", 
                response.devices().stream().map(d -> d.deviceCode()).toList());
        }
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleAssignmentResponse>> create(
            @RequestBody @Valid VehicleAssignmentRequest req) {
        log.info("🔍 DEBUG: Creating new vehicle assignment");
        VehicleAssignmentResponse response = service.createAssignment(req);
        log.info("🔍 DEBUG: Created vehicle assignment with ID: {}", response.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleAssignmentResponse>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateVehicleAssignmentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateAssignment(id, req)));
    }

    @GetMapping("/{vehicleTypeId}/get-all-with-order")
    public ResponseEntity<ApiResponse<List<VehicleAssignmentResponse>>> getByIdWithOrder(
            @PathVariable UUID vehicleTypeId) {

        List<VehicleAssignmentResponse> response = service.getAllAssignmentsWithOrder(vehicleTypeId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{orderId}/get-all-by-order-id")
    public ResponseEntity<ApiResponse<List<VehicleAssignmentResponse>>> getByOrderId(
            @PathVariable UUID orderId) {

        List<VehicleAssignmentResponse> response = service.getListVehicleAssignmentByOrderID(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

//    @GetMapping("/{orderId}/suggest-drivers-and-vehicle-for-details")
//    public ResponseEntity<ApiResponse<SimplifiedVehicleAssignmentResponse>> getSimplifiedSuggestions(
//            @PathVariable UUID orderId) {
//        SimplifiedVehicleAssignmentResponse response = service.getSimplifiedSuggestionsForOrder(orderId);
//        return ResponseEntity.ok(ApiResponse.ok(response));
//    }

    @GetMapping("/{orderId}/grouped-suggestions")
    public ResponseEntity<ApiResponse<GroupedVehicleAssignmentResponse>> getGroupedSuggestions(
            @PathVariable UUID orderId) {
        GroupedVehicleAssignmentResponse response = service.getGroupedSuggestionsForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/create-grouped-assignments")
    public ResponseEntity<ApiResponse<List<VehicleAssignmentResponse>>> createGroupedAssignments(
            @RequestBody @Valid GroupedAssignmentRequest request) {
        List<VehicleAssignmentResponse> responses = service.createGroupedAssignments(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(responses));
    }
}