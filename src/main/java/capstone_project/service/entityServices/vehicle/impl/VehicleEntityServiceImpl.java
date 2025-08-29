package capstone_project.service.entityServices.vehicle.impl;

import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.vehicle.VehicleRepository;
import capstone_project.service.entityServices.vehicle.VehicleEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleEntityServiceImpl implements VehicleEntityService {

    private final VehicleRepository vehicleRepository;

    @Override
    public VehicleEntity save(VehicleEntity entity) {
        return vehicleRepository.save(entity);
    }

    @Override
    public Optional<VehicleEntity> findContractRuleEntitiesById(UUID uuid) {
        return vehicleRepository.findById(uuid);
    }

    @Override
    public List<VehicleEntity> findAll() {
        return vehicleRepository.findAll();
    }

    @Override
    public Optional<VehicleEntity> findByLicensePlateNumber(String licensePlateNumber) {
        return vehicleRepository.findByLicensePlateNumber(licensePlateNumber);
    }

    @Override
    public Optional<VehicleEntity> findByVehicleId(UUID vehicleId) {
        return vehicleRepository.findById(vehicleId);
    }
}
