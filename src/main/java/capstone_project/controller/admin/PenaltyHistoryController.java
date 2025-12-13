package capstone_project.controller.admin;

import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.service.services.penalty.PenaltyHistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController("adminPenaltyHistoryController")
@RequestMapping("${penalty.api.base-path}/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Penalty Management", description = "APIs for managing traffic violations")
@PreAuthorize("hasRole('ADMIN')")
public class PenaltyHistoryController {

    private final PenaltyHistoryService penaltyHistoryService;

    @GetMapping
    @Operation(summary = "Get all traffic violations", description = "Retrieve list of all traffic violations in the system")
    public ResponseEntity<List<PenaltyHistoryResponse>> getAllPenalties() {
        log.info("Admin requesting all traffic violations");
        List<PenaltyHistoryResponse> penalties = penaltyHistoryService.getAllPenalties();
        return ResponseEntity.ok(penalties);
    }

    @GetMapping("/{penaltyId}")
    @Operation(summary = "Get traffic violation by ID", description = "Retrieve detailed information about a specific traffic violation")
    public ResponseEntity<PenaltyHistoryResponse> getPenaltyById(
            @Parameter(description = "ID of the traffic violation") 
            @PathVariable UUID penaltyId) {
        log.info("Admin requesting traffic violation details for ID: {}", penaltyId);
        PenaltyHistoryResponse penalty = penaltyHistoryService.getPenaltyById(penaltyId);
        return ResponseEntity.ok(penalty);
    }

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get traffic violations by driver", description = "Retrieve all traffic violations for a specific driver")
    public ResponseEntity<List<PenaltyHistoryResponse>> getPenaltiesByDriver(
            @Parameter(description = "ID of the driver") 
            @PathVariable UUID driverId) {
        log.info("Admin requesting traffic violations for driver: {}", driverId);
        List<PenaltyHistoryResponse> penalties = penaltyHistoryService.getPenaltiesByDriver(driverId);
        return ResponseEntity.ok(penalties);
    }

    @GetMapping("/vehicle-assignment/{vehicleAssignmentId}")
    @Operation(summary = "Get traffic violations by vehicle assignment", description = "Retrieve all traffic violations for a specific vehicle assignment")
    public ResponseEntity<List<PenaltyHistoryResponse>> getPenaltiesByVehicleAssignment(
            @Parameter(description = "ID of the vehicle assignment") 
            @PathVariable UUID vehicleAssignmentId) {
        log.info("Admin requesting traffic violations for vehicle assignment: {}", vehicleAssignmentId);
        List<PenaltyHistoryResponse> penalties = penaltyHistoryService.getPenaltiesByVehicleAssignment(vehicleAssignmentId);
        return ResponseEntity.ok(penalties);
    }

    @PostMapping
    @Operation(summary = "Create new traffic violation", description = "Add a new traffic violation record to the system")
    public ResponseEntity<PenaltyHistoryResponse> createPenalty(
            @Valid @RequestBody PenaltyHistoryEntity penalty) {
        log.info("Admin creating new traffic violation for driver: {}", penalty.getIssueBy().getId());
        PenaltyHistoryResponse createdPenalty = penaltyHistoryService.createPenalty(penalty);
        return ResponseEntity.ok(createdPenalty);
    }

    @PutMapping("/{penaltyId}")
    @Operation(summary = "Update traffic violation", description = "Update information about an existing traffic violation")
    public ResponseEntity<PenaltyHistoryResponse> updatePenalty(
            @Parameter(description = "ID of the traffic violation to update") 
            @PathVariable UUID penaltyId,
            @Valid @RequestBody PenaltyHistoryEntity penalty) {
        log.info("Admin updating traffic violation: {}", penaltyId);
        PenaltyHistoryResponse updatedPenalty = penaltyHistoryService.updatePenalty(penaltyId, penalty);
        return ResponseEntity.ok(updatedPenalty);
    }

    @DeleteMapping("/{penaltyId}")
    @Operation(summary = "Delete traffic violation", description = "Remove a traffic violation record from the system")
    public ResponseEntity<Void> deletePenalty(
            @Parameter(description = "ID of the traffic violation to delete") 
            @PathVariable UUID penaltyId) {
        log.info("Admin deleting traffic violation: {}", penaltyId);
        penaltyHistoryService.deletePenalty(penaltyId);
        return ResponseEntity.noContent().build();
    }
}
