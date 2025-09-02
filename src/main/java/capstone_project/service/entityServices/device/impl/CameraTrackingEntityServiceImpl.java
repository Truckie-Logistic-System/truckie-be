package capstone_project.service.entityServices.device.impl;

import capstone_project.entity.device.CameraTrackingEntity;
import capstone_project.repository.device.CameraTrackingRepository;
import capstone_project.service.entityServices.device.CameraTrackingEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CameraTrackingEntityServiceImpl implements CameraTrackingEntityService {

    private final CameraTrackingRepository cameraTrackingRepository;

    @Override
    public CameraTrackingEntity save(CameraTrackingEntity entity) {
        return cameraTrackingRepository.save(entity);
    }

    @Override
    public Optional<CameraTrackingEntity> findEntityById(UUID uuid) {
        return cameraTrackingRepository.findById(uuid);
    }

    @Override
    public List<CameraTrackingEntity> findAll() {
        return cameraTrackingRepository.findAll();
    }
}
