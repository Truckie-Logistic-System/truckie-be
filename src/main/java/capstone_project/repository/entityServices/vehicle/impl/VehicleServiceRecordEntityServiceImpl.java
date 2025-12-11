package capstone_project.repository.entityServices.vehicle.impl;

import capstone_project.entity.vehicle.VehicleServiceRecordEntity;
import capstone_project.repository.repositories.vehicle.VehicleServiceRecordRepository;
import capstone_project.repository.entityServices.vehicle.VehicleServiceRecordEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@RequiredArgsConstructor
public class VehicleServiceRecordEntityServiceImpl implements VehicleServiceRecordEntityService {

    private final VehicleServiceRecordRepository repository;

    @Override
    public VehicleServiceRecordEntity save(VehicleServiceRecordEntity entity) {
        return repository.save(entity);
    }

    @Override
    public Optional<VehicleServiceRecordEntity> findEntityById(UUID uuid) {
        return repository.findById(uuid);
    }

    @Override
    public List<VehicleServiceRecordEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public List<VehicleServiceRecordEntity> findByVehicleEntityId(UUID vehicleId) {
        return repository.findByVehicleEntityId(vehicleId);
    }
    
    @Override
    public Optional<VehicleServiceRecordEntity> findByIdWithVehicleAndVehicleType(UUID recordId) {
        return repository.findByIdWithVehicleAndVehicleType(recordId);
    }
}
