package capstone_project.repository.entityServices.device.impl;

import capstone_project.entity.device.DeviceTypeEntity;
import capstone_project.repository.repositories.device.DeviceTypeRepository;
import capstone_project.repository.entityServices.device.DeviceTypeEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceTypeEntityServiceImpl implements DeviceTypeEntityService {

    private final DeviceTypeRepository deviceTypeRepository;

    @Override
    public DeviceTypeEntity save(DeviceTypeEntity entity) {
        return deviceTypeRepository.save(entity);
    }

    @Override
    public Optional<DeviceTypeEntity> findEntityById(UUID uuid) {
        return deviceTypeRepository.findById(uuid);
    }

    @Override
    public List<DeviceTypeEntity> findAll() {
        return deviceTypeRepository.findAll();
    }

    @Override
    public Optional<DeviceTypeEntity> findByDeviceTypeName(String deviceTypeName) {
        return deviceTypeRepository.findByDeviceTypeName(deviceTypeName);
    }

    @Override
    public List<DeviceTypeEntity> getAllDeviceTypesByNameLike(String name) {
        return deviceTypeRepository.findAllByDeviceTypeNameContainingIgnoreCase(name);
    }
}
