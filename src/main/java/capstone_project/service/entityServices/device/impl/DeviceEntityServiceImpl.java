package capstone_project.service.entityServices.device.impl;

import capstone_project.entity.device.DeviceEntity;
import capstone_project.repository.device.DeviceRepository;
import capstone_project.service.entityServices.device.DeviceEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DeviceEntityServiceImpl implements DeviceEntityService {

    private final DeviceRepository deviceRepository;

    @Override
    public DeviceEntity save(DeviceEntity entity) {
        return deviceRepository.save(entity);
    }

    @Override
    public Optional<DeviceEntity> findById(UUID uuid) {
        return deviceRepository.findById(uuid);
    }

    @Override
    public List<DeviceEntity> findAll() {
        return deviceRepository.findAll();
    }
}
