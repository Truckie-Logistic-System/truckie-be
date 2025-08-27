package capstone_project.service.entityServices.user.impl;

import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.user.DriverRepository;
import capstone_project.service.entityServices.user.DriverEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DriverEntityServiceImpl implements DriverEntityService {

    private final DriverRepository driverRepository;

    @Override
    public DriverEntity save(DriverEntity entity) {
        return driverRepository.save(entity);
    }

    @Override
    public Optional<DriverEntity> findContractRuleEntitiesById(UUID uuid) {
        return driverRepository.findById(uuid);
    }

    @Override
    public List<DriverEntity> findAll() {
        return driverRepository.findAll();
    }
}
