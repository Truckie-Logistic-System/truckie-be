package capstone_project.repository.entityServices.vehicle.impl;

import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.repositories.vehicle.VehicleRepository;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.repository.EntityGraph;
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
    @EntityGraph(attributePaths = {"vehicleType", "vehicleAssignment", "vehicleMaintenance"})
    public Optional<VehicleEntity> findEntityById(UUID uuid) {
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

    @Override
    public Optional<VehicleEntity>  findVehicleDetailsById(UUID id) {
        return vehicleRepository.findVehicleWithJoinsById(id);
    }

    @Override
    public List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntity(VehicleTypeEntity vehicleTypeEntity) {
        return vehicleRepository.getVehicleEntitiesByVehicleTypeEntity(vehicleTypeEntity);
    }

    @Override
    public List<VehicleEntity> getVehicleEntitiesByVehicleTypeEntityAndStatus(VehicleTypeEntity vehicleTypeEntity, String status) {
        return vehicleRepository.getVehicleEntitiesByVehicleTypeEntityAndStatus(vehicleTypeEntity,status);
    }

}
