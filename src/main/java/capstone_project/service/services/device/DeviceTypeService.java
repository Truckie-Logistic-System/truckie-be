package capstone_project.service.services.device;

import capstone_project.dtos.request.device.DeviceTypeRequest;
import capstone_project.dtos.request.device.UpdateDeviceTypeRequest;
import capstone_project.dtos.response.device.DeviceTypeResponse;

import java.util.List;
import java.util.UUID;

public interface DeviceTypeService {
    List<DeviceTypeResponse> getAllDeviceTypes();

    List<DeviceTypeResponse> getListDeviceTypesByNameLike(String name);

    DeviceTypeResponse getDeviceTypeById(UUID id);

    DeviceTypeResponse createDeviceType(DeviceTypeRequest request);

    DeviceTypeResponse updateDeviceType(UUID id, UpdateDeviceTypeRequest request);

    void deleteDeviceType(UUID id);
}