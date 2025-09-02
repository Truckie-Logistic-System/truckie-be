package capstone_project.service.entityServices.device;

import capstone_project.entity.device.DeviceEntity;
import capstone_project.service.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface DeviceEntityService extends BaseEntityService<DeviceEntity, UUID> {
    Optional<DeviceEntity> findByDeviceCode(String deviceCode);

    Optional<DeviceEntity> findByDeviceTypeAndVehicle(UUID deviceTypeId, UUID vehicleId);
}
