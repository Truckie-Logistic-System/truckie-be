package capstone_project.repository.entityServices.user.impl;

import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.repositories.user.DriverRepository;
import capstone_project.repository.entityServices.user.DriverEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
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

    @Override
    public Optional<DriverEntity> findByPhoneNumber(String phoneNumber) {
        // Check for duplicates first
        List<DriverEntity> drivers = driverRepository.findByUserPhoneNumber(phoneNumber);
        
        if (drivers.isEmpty()) {
            return Optional.empty();
        }
        
        if (drivers.size() > 1) {
            log.warn("⚠️ Found {} drivers with phone number {}. Using first ACTIVE driver.", 
                    drivers.size(), phoneNumber);
            
            // Prefer ACTIVE driver
            return drivers.stream()
                    .filter(d -> "ACTIVE".equals(d.getStatus()))
                    .findFirst()
                    .or(() -> Optional.of(drivers.get(0)));
        }
        
        return Optional.of(drivers.get(0));
    }
}
