package capstone_project.controller.device;

import capstone_project.dtos.request.device.DeviceRequest;
import capstone_project.dtos.request.device.UpdateDeviceRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.device.DeviceResponse;
import capstone_project.dtos.response.device.DeviceBulkCreateForVehiclesResponse;
import capstone_project.service.services.device.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${device.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<DeviceResponse>>> getAllDevices() {
        final var result = deviceService.getAllDevices();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> getDeviceById(@PathVariable UUID id) {
        final var result = deviceService.getDeviceById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{deviceCode}/code")
    public ResponseEntity<ApiResponse<DeviceResponse>> getDeviceByDeviceCode(@PathVariable String deviceCode) {
        final var result = deviceService.getDeviceByDeviceCode(deviceCode);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<DeviceResponse>> createDevice(@RequestBody @Valid DeviceRequest deviceRequest) {
        final var result = deviceService.createDevice(deviceRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeviceResponse>> updateDevice(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateDeviceRequest deviceRequest) {
        final var result = deviceService.updateDevice(id, deviceRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * API tiện ích cho admin/staff:
     * - Đếm số lượng xe trong hệ thống
     * - Với mỗi xe, tự động tạo:
     *   + 1 thiết bị loại "Camera hành trình"
     *   + 1 thiết bị loại "Thiết bị GPS"
     * - Bỏ qua những thiết bị đã tồn tại (trùng loại + xe)
     */
    @PostMapping("/bulk-create-default-for-vehicles")
    public ResponseEntity<ApiResponse<DeviceBulkCreateForVehiclesResponse>> bulkCreateDefaultDevicesForVehicles() {
        final var result = deviceService.createDefaultDevicesForAllVehicles();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

}
