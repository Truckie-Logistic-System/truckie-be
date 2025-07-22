package capstone_project.service.entityServices.impl;

import capstone_project.entity.DriverEntity;
import capstone_project.repository.DriverRepository;
import capstone_project.service.entityServices.DriversEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriversEntityServiceImpl implements DriversEntityService {

    private final DriverRepository driverRepository;

    @Override
    public DriverEntity save(DriverEntity entity) {
        return driverRepository.save(entity);
    }

    @Override
    public Optional<DriverEntity> findById(UUID uuid) {
        return driverRepository.findById(uuid);
    }

    @Override
    public List<DriverEntity> findAll() {
        return driverRepository.findAll();
    }
}
