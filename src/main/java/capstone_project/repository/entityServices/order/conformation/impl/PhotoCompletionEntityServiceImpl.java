package capstone_project.repository.entityServices.order.conformation.impl;

import capstone_project.entity.order.conformation.PhotoCompletionEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.repositories.order.conformation.PhotoCompletionRepository;
import capstone_project.repository.entityServices.order.conformation.PhotoCompletionEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PhotoCompletionEntityServiceImpl implements PhotoCompletionEntityService {

    private final PhotoCompletionRepository photoCompletionRepository;

    @Override
    public PhotoCompletionEntity save(PhotoCompletionEntity entity) {
        return photoCompletionRepository.save(entity);
    }

    @Override
    public Optional<PhotoCompletionEntity> findEntityById(UUID uuid) {
        return photoCompletionRepository.findById(uuid);
    }

    @Override
    public List<PhotoCompletionEntity> findAll() {
        return photoCompletionRepository.findAll();
    }

    @Override
    public List<PhotoCompletionEntity> findByVehicleAssignmentEntity(VehicleAssignmentEntity vehicleAssignmentEntity) {
        return photoCompletionRepository.findByVehicleAssignmentEntity(vehicleAssignmentEntity);
    }
}
