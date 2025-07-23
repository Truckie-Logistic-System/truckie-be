package capstone_project.service.entityServices.device.impl;

import capstone_project.entity.device.DeviceTypeEntity;
import capstone_project.repository.device.DeviceTypeRepository;
import capstone_project.service.entityServices.device.DeviceTypeEntityService;
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
    public Optional<DeviceTypeEntity> findById(UUID uuid) {
        return deviceTypeRepository.findById(uuid);
    }

    @Override
    public List<DeviceTypeEntity> findAll() {
        return deviceTypeRepository.findAll();
    }
}
