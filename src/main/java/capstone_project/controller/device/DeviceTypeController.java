package capstone_project.controller.device;

import capstone_project.dtos.request.device.DeviceTypeRequest;
import capstone_project.dtos.request.device.UpdateDeviceTypeRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.device.DeviceTypeResponse;
import capstone_project.service.services.device.DeviceTypeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${device-type.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DeviceTypeController {

    private final DeviceTypeService deviceTypeService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<DeviceTypeResponse>>> getAllDeviceTypes() {
        final var result = deviceTypeService.getAllDeviceTypes();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<DeviceTypeResponse>>> getAllDeviceTypesByNameLike(@RequestParam String name) {
        final var result = deviceTypeService.getListDeviceTypesByNameLike(name);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceTypeResponse>> getDeviceTypeById(@PathVariable UUID id) {
        final var result = deviceTypeService.getDeviceTypeById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<DeviceTypeResponse>> createDeviceType(@RequestBody @Valid DeviceTypeRequest request) {
        final var result = deviceTypeService.createDeviceType(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceTypeResponse>> updateDeviceType(@PathVariable UUID id, @RequestBody @Valid UpdateDeviceTypeRequest request) {
        final var result = deviceTypeService.updateDeviceType(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
