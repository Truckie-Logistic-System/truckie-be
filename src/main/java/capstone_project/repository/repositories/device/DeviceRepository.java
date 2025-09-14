package capstone_project.repository.repositories.device;

import capstone_project.entity.device.DeviceEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends BaseRepository<DeviceEntity> {
    Optional<DeviceEntity> findDeviceEntityByDeviceCode(String deviceCode);

    Optional<DeviceEntity> findDeviceEntityByDeviceTypeEntityIdAndVehicleEntityId(UUID deviceTypeId, UUID vehicleId);
}
