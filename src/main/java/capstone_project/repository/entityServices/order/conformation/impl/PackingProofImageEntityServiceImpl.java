package capstone_project.repository.entityServices.order.conformation.impl;

import capstone_project.entity.order.confirmation.PackingProofImageEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.order.conformation.PackingProofImageRepository;
import capstone_project.repository.entityServices.order.conformation.PackingProofImageEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PackingProofImageEntityServiceImpl implements PackingProofImageEntityService {

    private final PackingProofImageRepository packingProofImageRepository;

    @Override
    public PackingProofImageEntity save(PackingProofImageEntity entity) {
        return packingProofImageRepository.save(entity);
    }

    @Override
    public Optional<PackingProofImageEntity> findEntityById(UUID uuid) {
        return packingProofImageRepository.findById(uuid);
    }

    @Override
    public List<PackingProofImageEntity> findAll() {
        return packingProofImageRepository.findAll();
    }

    @Override
    public List<PackingProofImageEntity> findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentEntity) {
        return packingProofImageRepository.findByVehicleAssignmentEntity(vehicleAssignmentEntity);
    }
}
