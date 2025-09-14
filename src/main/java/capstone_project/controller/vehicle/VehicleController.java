package capstone_project.controller.vehicle;

import capstone_project.dtos.request.vehicle.UpdateVehicleRequest;
import capstone_project.dtos.request.vehicle.VehicleRequest;
import capstone_project.dtos.response.vehicle.VehicleGetDetailsResponse;
import capstone_project.dtos.response.vehicle.VehicleResponse;
import capstone_project.service.services.vehicle.VehicleService;
import capstone_project.dtos.response.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

//    @DeleteMapping("/{id}")
//    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
//        service.deleteVehicle(id);
//        return ResponseEntity.ok(ApiResponse.ok());
//    }
}