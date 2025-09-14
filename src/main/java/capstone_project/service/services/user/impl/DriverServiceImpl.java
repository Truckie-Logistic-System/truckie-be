package capstone_project.service.services.user.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.dtos.request.user.UpdateDriverRequest;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.service.services.user.DriverService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverServiceImpl implements DriverService {

    private final DriverEntityService driverEntityService;
    private final DriverMapper driverMapper;

    @Override
    public List<DriverResponse> getAllDrivers() {
        log.info("Getting all drivers");

        List<DriverEntity> driverEntities = driverEntityService.findAll();
        if (driverEntities.isEmpty()) {
            log.info("No drivers found");
            throw new BadRequestException(
                    "No drivers found",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return driverEntities.stream()
                .map(driverMapper::mapDriverResponse)
                .toList();
    }

    @Override
    public DriverResponse getDriverById(UUID id) {
        log.info("Getting driver by ID: {}", id);

        if (id == null) {
            log.error("Driver ID is null");
            throw new BadRequestException(
                    "Driver ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        DriverEntity driverEntity = driverEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.error("Driver not found with ID: {}", id);
                    return new BadRequestException(
                            "Driver not found with ID: " + id,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        return driverMapper.mapDriverResponse(driverEntity);
    }

    @Override
    public DriverResponse getDriverByUserId(UUID userId) {
        log.info("Getting driver by User ID: {}", userId);

        if (userId == null) {
            log.error("User ID is null");
            throw new BadRequestException(
                    "User ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        DriverEntity driverEntity = driverEntityService.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Driver not found with User ID: {}", userId);
                    return new BadRequestException(
                            "Driver not found with User ID: " + userId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        return driverMapper.mapDriverResponse(driverEntity);
    }

    @Override
    public DriverResponse updateDriver(UUID driverId, UpdateDriverRequest updateDriverRequest) {
        log.info("Updating driver: {}", driverId);
        if (driverId == null) {
            log.error("Driver ID is null");
            throw new BadRequestException(
                    "Driver ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        DriverEntity driverEntity = driverEntityService.findEntityById(driverId)
                .orElseThrow(() -> {
                    log.error("Driver not found with ID: {}", driverId);
                    return new BadRequestException(
                            "Driver not found with ID: " + driverId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        driverMapper.toDriverEntity(updateDriverRequest, driverEntity);
        DriverEntity updatedDriver = driverEntityService.save(driverEntity);
        return driverMapper.mapDriverResponse(updatedDriver);
    }

    @Override
    public DriverResponse updateDriverStatus(UUID driverId, String status) {
        log.info("Updating driver status: {}", driverId);
        if (driverId == null) {
            log.error("Driver ID is null");
            throw new BadRequestException(
                    "Driver ID cannot be null",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        DriverEntity driverEntity = driverEntityService.findEntityById(driverId)
                .orElseThrow(() -> {
                    log.error("Driver not found with ID: {}", driverId);
                    return new BadRequestException(
                            "Driver not found with ID: " + driverId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        UserStatusEnum userStatusEnum;
        try {
            userStatusEnum = UserStatusEnum.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            log.error("[updateUserStatusById] - Invalid status: {}", status);
            throw new BadRequestException(
                    "Invalid status: " + status,
                    ErrorEnum.ENUM_INVALID.getErrorCode()
            );
        }

        driverEntity.setStatus(userStatusEnum.name());
        DriverEntity updatedDriver = driverEntityService.save(driverEntity);
        return driverMapper.mapDriverResponse(updatedDriver);
    }

    @Override
    public List<DriverResponse> getAllDriversByUserRoleName(String roleName) {
        log.info("Getting drivers by role name: {}", roleName);
        if (roleName == null || roleName.isBlank()) {
            log.error("Role name is null or blank");
            throw new BadRequestException(
                    "Role name cannot be null or blank",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        List<DriverEntity> driverEntities = driverEntityService.findByUser_Role_RoleName(roleName);
        if (driverEntities.isEmpty()) {
            log.info("No drivers found with role name: {}", roleName);
            throw new BadRequestException(
                    "No drivers found with role name: " + roleName,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return driverEntities.stream()
                .map(driverMapper::mapDriverResponse)
                .toList();
    }
}
