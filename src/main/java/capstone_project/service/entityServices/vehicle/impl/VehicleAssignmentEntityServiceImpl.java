package capstone_project.service.entityServices.vehicle.impl;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.vehicle.VehicleAssignmentRepository;
import capstone_project.service.entityServices.vehicle.VehicleAssignmentEntityService;
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
    public Optional<VehicleAssignmentEntity> findById(UUID uuid) {
        return vehicleAssignmentRepository.findById(uuid);
    }

    @Override
    public List<VehicleAssignmentEntity> findAll() {
        return vehicleAssignmentRepository.findAll();
    }
}
