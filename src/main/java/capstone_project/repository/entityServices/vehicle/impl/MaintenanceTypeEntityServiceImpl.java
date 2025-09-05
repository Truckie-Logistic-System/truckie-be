package capstone_project.repository.entityServices.vehicle.impl;

import capstone_project.entity.vehicle.MaintenanceTypeEntity;
import capstone_project.repository.repositories.vehicle.MaintenanceTypeRepository;
import capstone_project.repository.entityServices.vehicle.MaintenanceTypeEntityService;
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
    public Optional<MaintenanceTypeEntity> findEntityById(UUID uuid) {
        return maintenanceTypeRepository.findById(uuid);
    }

    @Override
    public List<MaintenanceTypeEntity> findAll() {
        return maintenanceTypeRepository.findAll();
    }
}
