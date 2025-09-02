package capstone_project.service.services.device;

import capstone_project.dtos.request.device.DeviceRequest;
import capstone_project.dtos.request.device.UpdateDeviceRequest;
import capstone_project.dtos.response.device.DeviceResponse;

import java.util.List;
import java.util.UUID;

public interface DeviceService {
    List<DeviceResponse> getAllDevices();

    DeviceResponse getDeviceById(UUID id);

    DeviceResponse getDeviceByDeviceCode(String deviceCode);

    DeviceResponse createDevice(DeviceRequest request);

    DeviceResponse updateDevice(UUID id, UpdateDeviceRequest request);
}
