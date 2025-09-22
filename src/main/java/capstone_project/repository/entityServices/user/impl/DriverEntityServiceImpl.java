package capstone_project.repository.entityServices.user.impl;

import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.repositories.user.DriverRepository;
import capstone_project.repository.entityServices.user.DriverEntityService;
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
    public Optional<DriverEntity> findEntityById(UUID uuid) {
        return driverRepository.findById(uuid);
    }

    @Override
    public List<DriverEntity> findAll() {
        return driverRepository.findAll();
    }

    @Override
    public List<DriverEntity> findByUser_Role_RoleName(String userRoleRoleName) {
        return driverRepository.findByUser_Role_RoleName(userRoleRoleName);
    }

    @Override
    public Optional<DriverEntity> findByUserId(UUID userId) {
        return driverRepository.findByUserId(userId);
    }

    @Override
    public List<DriverEntity> findByStatus(String status) {
        return driverRepository.findByStatus(status);
    }
}
