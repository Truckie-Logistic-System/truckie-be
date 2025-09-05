package capstone_project.repository.entityServices.vehicle.impl;

import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.repository.repositories.vehicle.VehicleMaintenanceRepository;
import capstone_project.repository.entityServices.vehicle.VehicleMaintenanceEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class VehicleMaintenanceEntityServiceImpl implements VehicleMaintenanceEntityService {

    private final VehicleMaintenanceRepository vehicleMaintenanceRepository;

    @Override
    public VehicleMaintenanceEntity save(VehicleMaintenanceEntity entity) {
        return vehicleMaintenanceRepository.save(entity);
    }

    @Override
    public Optional<VehicleMaintenanceEntity> findEntityById(UUID uuid) {
        return vehicleMaintenanceRepository.findById(uuid);
    }


    @Override
    public List<VehicleMaintenanceEntity> findAll() {
        return vehicleMaintenanceRepository.findAll();
    }
}
