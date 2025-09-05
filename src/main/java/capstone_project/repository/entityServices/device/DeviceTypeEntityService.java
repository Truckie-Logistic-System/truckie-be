package capstone_project.repository.entityServices.device;

import capstone_project.entity.device.DeviceTypeEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceTypeEntityService extends BaseEntityService<DeviceTypeEntity, UUID> {
    Optional<DeviceTypeEntity> findByDeviceTypeName(String deviceTypeName);

    List<DeviceTypeEntity> getAllDeviceTypesByNameLike(String name);
}
