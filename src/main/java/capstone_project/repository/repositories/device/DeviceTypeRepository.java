package capstone_project.repository.repositories.device;

import capstone_project.entity.device.DeviceTypeEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTypeRepository extends BaseRepository<DeviceTypeEntity> {
    Optional<DeviceTypeEntity> findByDeviceTypeName(String deviceTypeName);

    List<DeviceTypeEntity> findAllByDeviceTypeNameContainingIgnoreCase(String name);
}
