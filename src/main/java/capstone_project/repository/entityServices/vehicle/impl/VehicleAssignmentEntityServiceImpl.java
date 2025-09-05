package capstone_project.repository.entityServices.vehicle.impl;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.vehicle.VehicleAssignmentRepository;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleAssignmentEntityServiceImpl implements VehicleAssignmentEntityService {

    private final VehicleAssignmentRepository vehicleAssignmentRepository;

    @Override
    public VehicleAssignmentEntity save(VehicleAssignmentEntity entity) {
        return vehicleAssignmentRepository.save(entity);
    }

    @Override
    public Optional<VehicleAssignmentEntity> findEntityById(UUID uuid) {
        return vehicleAssignmentRepository.findById(uuid);
    }

    @Override
    public List<VehicleAssignmentEntity> findAll() {
        return vehicleAssignmentRepository.findAll();
    }

    @Override
    public List<VehicleAssignmentEntity> findByStatus(String status) {
        return vehicleAssignmentRepository.findByStatus(status);
    }
}
