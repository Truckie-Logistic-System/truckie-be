package capstone_project.service.entityServices.impl;

import capstone_project.entity.DriversEntity;
import capstone_project.repository.DriverRepository;
import capstone_project.service.entityServices.DriversEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DriversEntityServiceImpl implements DriversEntityService {

    private final DriverRepository driverRepository;

    @Override
    public DriversEntity createDriver(DriversEntity driversEntity) {
        return driverRepository.save(driversEntity);
    }
}
