package capstone_project.service.services.user.impl;

import capstone_project.common.enums.DriverLicenseClassEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.enums.VehicleTypeEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.dtos.request.user.UpdateDriverRequest;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.service.services.user.DriverService;
import capstone_project.service.services.user.PenaltyHistoryService;
import capstone_project.common.utils.UserContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverServiceImpl implements DriverService {

    private final DriverEntityService driverEntityService;
    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final DriverMapper driverMapper;
    private final PenaltyHistoryService penaltyHistoryService;
    private final UserContextUtils userContextUtils;

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

        // Map driver entity to response
        DriverResponse driverResponse = driverMapper.mapDriverResponse(driverEntity);

        // Get penalty histories for this driver
        List<PenaltyHistoryResponse> penaltyHistories = penaltyHistoryService.getByDriverId(id);
        driverResponse.setPenaltyHistories(penaltyHistories);

        log.info("Found {} penalty histories for driver ID: {}", penaltyHistories.size(), id);

        return driverResponse;
    }

    @Override
    public DriverResponse getDriverByUserId() {
        UUID userId = userContextUtils.getCurrentUserId();
        log.info("Getting driver by current authenticated User ID: {}", userId);

        DriverEntity driverEntity = driverEntityService.findByUserId(userId)
                .orElseThrow(() -> {
                    log.error("Driver not found with User ID: {}", userId);
                    return new BadRequestException(
                            "Driver not found for current user",
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
    public boolean isCheckClassDriverLicenseForVehicleType(DriverEntity driver, VehicleTypeEnum vehicleType) {
        return getEligibleVehicleTypes(driver).contains(vehicleType);
    }

    private Set<VehicleTypeEnum> getEligibleVehicleTypes(DriverEntity driver) {
        if (driver.getLicenseClass() == null) {
            return Set.of(); // Không có bằng lái
        }

        try {
            DriverLicenseClassEnum licenseClassEnum =
                    DriverLicenseClassEnum.valueOf(driver.getLicenseClass().toUpperCase());
            switch (licenseClassEnum) {
                case B2:
                    return EnumSet.of(
                            VehicleTypeEnum.TRUCK_0_5_TON,
                            VehicleTypeEnum.TRUCK_1_25_TON,
                            VehicleTypeEnum.TRUCK_1_9_TON,
                            VehicleTypeEnum.TRUCK_2_4_TONN,
                            VehicleTypeEnum.TRUCK_3_5_TON,
                            VehicleTypeEnum.TRUCK_5_TON,
                            VehicleTypeEnum.TRUCK_7_TON
                    );
                case C:
                    return EnumSet.allOf(VehicleTypeEnum.class); // full quyền
                default:
                    return Set.of();
            }
        } catch (IllegalArgumentException e) {
            // Trường hợp DB lưu rác, không mapping được enum
            return Set.of();
        }
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
