package capstone_project.service.services.device;

import capstone_project.dtos.request.device.DeviceRequest;
import capstone_project.dtos.request.device.UpdateDeviceRequest;
import capstone_project.dtos.response.device.DeviceResponse;
import capstone_project.dtos.response.device.DeviceBulkCreateForVehiclesResponse;

import java.util.List;
import java.util.UUID;

public interface DeviceService {
    List<DeviceResponse> getAllDevices();

    DeviceResponse getDeviceById(UUID id);

    DeviceResponse getDeviceByDeviceCode(String deviceCode);

    DeviceResponse createDevice(DeviceRequest request);

    DeviceResponse updateDevice(UUID id, UpdateDeviceRequest request);

    /**
     * Tạo thiết bị mặc định cho tất cả xe trong hệ thống.
     * Mỗi xe sẽ cố gắng được gán:
     * - 1 thiết bị loại "Camera hành trình"
     * - 1 thiết bị loại "Thiết bị GPS"
     * Bỏ qua những thiết bị đã tồn tại (trùng loại + xe).
     */
    DeviceBulkCreateForVehiclesResponse createDefaultDevicesForAllVehicles();
}
