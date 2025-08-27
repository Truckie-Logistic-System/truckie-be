package capstone_project.service.entityServices.vehicle.impl;

import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.vehicle.VehicleTypeRepository;
import capstone_project.service.entityServices.vehicle.VehicleTypeEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleTypeEntityServiceImpl implements VehicleTypeEntityService {

    private final VehicleTypeRepository vehicleTypeRepository;

    @Override
    public VehicleTypeEntity save(VehicleTypeEntity entity) {
        return vehicleTypeRepository.save(entity);
    }

    @Override
    public Optional<VehicleTypeEntity> findContractRuleEntitiesById(UUID uuid) {
        return vehicleTypeRepository.findById(uuid);
    }

    @Override
    public List<VehicleTypeEntity> findAll() {
        return vehicleTypeRepository.findAll();
    }

    @Override
    public Optional<VehicleTypeEntity> findByVehicleTypeName(String vehicleTypeName) {
        return vehicleTypeRepository.findByVehicleTypeName(vehicleTypeName);
    }
}
