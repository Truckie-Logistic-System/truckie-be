package capstone_project.service.entityServices.vehicle.impl;

import capstone_project.entity.vehicle.MaintenanceTypeEntity;
import capstone_project.repository.vehicle.MaintenanceTypeRepository;
import capstone_project.service.entityServices.vehicle.MaintenanceTypeEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MaintenanceTypeEntityServiceImpl implements MaintenanceTypeEntityService {

    private final MaintenanceTypeRepository maintenanceTypeRepository;

    @Override
    public MaintenanceTypeEntity save(MaintenanceTypeEntity entity) {
        return maintenanceTypeRepository.save(entity);
    }

    @Override
    public Optional<MaintenanceTypeEntity> findById(UUID uuid) {
        return maintenanceTypeRepository.findById(uuid);
    }

    @Override
    public List<MaintenanceTypeEntity> findAll() {
        return maintenanceTypeRepository.findAll();
    }
}
