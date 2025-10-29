package capstone_project.repository.entityServices.order.order.impl;

import capstone_project.entity.order.order.SealEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.order.SealEntityService;
import capstone_project.repository.repositories.order.order.SealRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SealEntityServiceImpl implements SealEntityService {
    private final SealRepository sealRepository;

    @Override
    public SealEntity save(SealEntity entity) {
        return sealRepository.save(entity);
    }

    @Override
    public Optional<SealEntity> findEntityById(UUID uuid) {
        return sealRepository.findById(uuid);
    }

    @Override
    public List<SealEntity> findAll() {
        return sealRepository.findAll();
    }

    @Override
    public List<SealEntity> saveAll(List<SealEntity> sealEntities) {
        return sealRepository.saveAll(sealEntities);
    }

    @Override
    public List<SealEntity> findBySealCode(String sealCode) {
        return sealRepository.findBySealCode(sealCode);
    }

    @Override
    public SealEntity findByVehicleAssignment(VehicleAssignmentEntity vehicleAssignment, String status) {
        return sealRepository.findFirstByVehicleAssignmentAndStatus(vehicleAssignment, status);
    }

    @Override
    public List<SealEntity> findAllByVehicleAssignment(VehicleAssignmentEntity vehicleAssignment) {
        return sealRepository.findByVehicleAssignment(vehicleAssignment);
    }

    @Override
    public List<SealEntity> findAllByVehicleAssignmentAndStatus(VehicleAssignmentEntity vehicleAssignment, String status) {
        return sealRepository.findAllByVehicleAssignmentAndStatus(vehicleAssignment, status);
    }
}
