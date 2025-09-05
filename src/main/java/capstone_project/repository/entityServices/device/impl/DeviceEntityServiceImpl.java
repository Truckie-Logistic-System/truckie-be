package capstone_project.repository.entityServices.device.impl;

import capstone_project.entity.device.DeviceEntity;
import capstone_project.repository.repositories.device.DeviceRepository;
import capstone_project.repository.entityServices.device.DeviceEntityService;
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
    public Optional<DeviceEntity> findEntityById(UUID uuid) {
        return deviceRepository.findById(uuid);
    }

    @Override
    public List<DeviceEntity> findAll() {
        return deviceRepository.findAll();
    }

    @Override
    public Optional<DeviceEntity> findByDeviceCode(String deviceCode) {
        return  deviceRepository.findDeviceEntityByDeviceCode(deviceCode);
    }

    @Override
    public Optional<DeviceEntity> findByDeviceTypeAndVehicle(UUID deviceTypeId, UUID vehicleId) {
        return deviceRepository.findDeviceEntityByDeviceTypeEntityIdAndVehicleEntityId(deviceTypeId, vehicleId);
    }
}
