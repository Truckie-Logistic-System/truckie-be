package capstone_project.repository.device;

import capstone_project.entity.device.DeviceTypeEntity;
import capstone_project.repository.common.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTypeRepository extends BaseRepository<DeviceTypeEntity> {
    Optional<DeviceTypeEntity> findByDeviceTypeName(String deviceTypeName);

    List<DeviceTypeEntity> findAllByDeviceTypeNameContainingIgnoreCase(String name);
}
