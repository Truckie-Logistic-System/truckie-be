package capstone_project.controller.vehicle;

import capstone_project.dtos.request.vehicle.MaintenanceTypeRequest;
import capstone_project.dtos.request.vehicle.UpdateMaintenanceTypeRequest;
import capstone_project.dtos.response.vehicle.MaintenanceTypeResponse;
import capstone_project.service.services.vehicle.MaintenanceTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${maintenance-type.api.base-path}")
@RequiredArgsConstructor
public class MaintenanceTypeController {

    private final MaintenanceTypeService service;

    @GetMapping
    public ResponseEntity<List<MaintenanceTypeResponse>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MaintenanceTypeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.getById(id));
    }

    @PostMapping
    public ResponseEntity<MaintenanceTypeResponse> create(@Valid @RequestBody MaintenanceTypeRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MaintenanceTypeResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody UpdateMaintenanceTypeRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
