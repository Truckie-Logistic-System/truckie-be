package capstone_project.service.services.user.impl;

import capstone_project.common.enums.DriverLicenseClassEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.enums.VehicleTypeEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.dtos.request.user.LicenseRenewalRequest;
import capstone_project.dtos.request.user.UpdateDriverRequest;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.entity.auth.RoleEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.entityServices.auth.RoleEntityService;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.service.services.user.DriverService;
import capstone_project.service.services.user.PenaltyHistoryService;
import capstone_project.common.utils.UserContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import capstone_project.common.utils.VietnamTimeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final RoleEntityService roleEntityService;
    private final PasswordEncoder passwordEncoder;
    private final UserEntityService userEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;

    @Override
    public List<DriverResponse> getAllDrivers() {

        List<DriverEntity> driverEntities = driverEntityService.findAll();
        if (driverEntities.isEmpty()) {
            
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

        return driverResponse;
    }

    @Override
    public DriverResponse getDriverByUserId() {
        UUID userId = userContextUtils.getCurrentUserId();

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
                    // B2: Xe tải từ 3.5 tấn trở xuống (yêu cầu 18 tuổi trở lên)
                    return EnumSet.of(
                            VehicleTypeEnum.TRUCK_0_5_TON,
                            VehicleTypeEnum.TRUCK_1_25_TON,
                            VehicleTypeEnum.TRUCK_1_9_TON,
                            VehicleTypeEnum.TRUCK_2_4_TONN,
                            VehicleTypeEnum.TRUCK_3_5_TON
                    );
                case C:
                    // C: Tất cả các loại xe (yêu cầu 24 tuổi trở lên)
                    return EnumSet.allOf(VehicleTypeEnum.class);
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
        
        if (roleName == null || roleName.isBlank()) {
            log.error("Role name is null or blank");
            throw new BadRequestException(
                    "Role name cannot be null or blank",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        List<DriverEntity> driverEntities = driverEntityService.findByUser_Role_RoleName(roleName);
        if (driverEntities.isEmpty()) {
            
            throw new BadRequestException(
                    "No drivers found with role name: " + roleName,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return driverEntities.stream()
                .map(driverMapper::mapDriverResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<DriverResponse> generateBulkDrivers(Integer count) {

        // Prepare lists to store the results
        List<DriverEntity> createdDrivers = new ArrayList<>();
        List<DriverResponse> driverResponses = new ArrayList<>();

        // Find the highest existing driver number
        int startId = findHighestDriverNumber() + 1;

        // Get the DRIVER role
        RoleEntity role = roleEntityService.findByRoleName(RoleTypeEnum.DRIVER.name())
                .orElseThrow(() -> new BadRequestException(
                        "Role " + RoleTypeEnum.DRIVER.name() + " not found",
                        ErrorEnum.ROLE_NOT_FOUND.getErrorCode()
                ));

        // Last name options for generated drivers
        String[] lastNames = {"Nguyen", "Tran", "Le", "Pham", "Hoang", "Vo", "Dang"};

        // Generate sequential drivers
        for (int i = 0; i < count; i++) {
            int currentId = startId + i;

            // Create user and driver with sequential data
            String username = "driver" + currentId;
            String email = username + "@gmail.com";
            String password = username; // Password same as username
            String lastName = lastNames[i % lastNames.length];
            String fullName = "Driver " + currentId + " " + lastName;
            String phoneNumber = "0901" + String.format("%06d", currentId);

            int provinceCode = (currentId % 63) + 1;
            String identityNumber = String.format("%03d%06d%03d",
                    provinceCode,
                    900000 + (currentId % 100000),
                    currentId % 1000);

            String[] provinceCodes = {"HN", "HCM", "DN", "HP", "CT"};
            String[] issueCodes = {"CA", "CS", "PA", "GT", "QD"};
            String provinceLetterCode = provinceCodes[i % provinceCodes.length];
            String issueLetterCode = issueCodes[i % issueCodes.length];
            String cardSerialNumber = provinceLetterCode + "-" + issueLetterCode + "-" + String.format("%06d", currentId);

            String licenseNumber = "20ASX" + String.format("%02d", currentId % 100);

            // Create user entity
            UserEntity user = UserEntity.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .phoneNumber(phoneNumber)
                    .gender(true) // Default gender
                    .dateOfBirth(LocalDate.of(1990, 1, 1)) // Default DOB
                    .status(UserStatusEnum.ACTIVE.name())
                    .role(role)
                    .createdAt(VietnamTimeUtils.now())
                    .build();

            UserEntity savedUser = userEntityService.save(user);

            // Generate random dates for driver documents
            LocalDateTime dateOfIssue = VietnamTimeUtils.now().minusYears(3 + (i % 5));
            LocalDateTime dateOfExpiry = dateOfIssue.plusYears(5);
            LocalDateTime dateOfPassing = dateOfIssue.minusMonths(2);

            // Rotate through license classes and locations
            String[] licenseClasses = {"B2", "C"};
            String[] locations = {"HN", "HCM", "DN", "CT", "HP"};

            String licenseClass = licenseClasses[i % licenseClasses.length];
            String location = locations[i % locations.length];

            // Create driver entity
            DriverEntity driverEntity = DriverEntity.builder()
                    .driverLicenseNumber(licenseNumber)
                    .identityNumber(identityNumber)
                    .cardSerialNumber(cardSerialNumber)
                    .placeOfIssue(location)
                    .dateOfIssue(dateOfIssue)
                    .dateOfExpiry(dateOfExpiry)
                    .licenseClass(licenseClass)
                    .dateOfPassing(dateOfPassing)
                    .status(UserStatusEnum.ACTIVE.name())
                    .user(savedUser)
                    .createdAt(VietnamTimeUtils.now())
                    .build();

            DriverEntity savedDriver = driverEntityService.save(driverEntity);
            createdDrivers.add(savedDriver);

        }

        // Map the driver entities to responses
        for (DriverEntity driver : createdDrivers) {
            driverResponses.add(driverMapper.mapDriverResponse(driver));
        }

        return driverResponses;
    }

    /**
     * Find the highest driver number from existing usernames
     * Looks for usernames in format "driverXX" and returns the highest XX value
     * @return the highest driver number, or 20 as default if none found
     */
    private int findHighestDriverNumber() {
        int highestNumber = 20; // Default starting number if no drivers found

        try {
            // Get all users with username starting with "driver"
            List<UserEntity> drivers = userEntityService.findByUsernameStartingWith("driver");

            for (UserEntity user : drivers) {
                String username = user.getUsername();
                // Extract the number from the username (e.g., "driver21" -> 21)
                if (username != null && username.startsWith("driver")) {
                    try {
                        int driverNumber = Integer.parseInt(username.substring(6)); // Skip "driver" prefix
                        if (driverNumber > highestNumber) {
                            highestNumber = driverNumber;
                        }
                    } catch (NumberFormatException e) {
                        // Skip non-numeric usernames
                        log.warn("Skipping non-standard driver username: {}", username);
                    }
                }
            }

        } catch (Exception e) {
            log.error("Error finding highest driver number, using default: {}", highestNumber, e);
        }

        return highestNumber;
    }

    @Override
    public DriverResponse validateDriverByPhone(String phoneNumber) {

        // Validate phone number format
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            throw new BadRequestException(
                "⚠️ Vui lòng nhập số điện thoại hợp lệ (ít nhất 10 số)",
                ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }
        
        // Normalize phone number (remove spaces, dashes, etc.)
        String normalizedPhone = phoneNumber.replaceAll("[\\s-()]", "");
        
        if (normalizedPhone.length() < 10) {
            throw new BadRequestException(
                "⚠️ Vui lòng nhập số điện thoại hợp lệ (ít nhất 10 số)",
                ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }
        
        // Find driver by phone number
        DriverEntity driver = driverEntityService.findByPhoneNumber(normalizedPhone)
            .orElseThrow(() -> {
                log.error("Driver not found with phone number: {}", phoneNumber);
                return new NotFoundException(
                    "❌ Không tìm thấy tài xế với số điện thoại này",
                    ErrorEnum.NOT_FOUND.getErrorCode()
                );
            });
        
        // Check if driver is ACTIVE
        if (!CommonStatusEnum.ACTIVE.name().equals(driver.getStatus())) {
            log.error("Driver with phone {} is not ACTIVE, current status: {}", phoneNumber, driver.getStatus());
            throw new BadRequestException(
                "⚠️ Tài xế " + driver.getUser().getFullName() + " hiện không khả dụng (đã vô hiệu hóa)",
                ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Check if driver's license is expired
        if (isLicenseExpired(driver)) {
            log.error("Driver {} ({}) has expired license", driver.getUser().getFullName(), phoneNumber);
            throw new BadRequestException(
                "⚠️ Tài xế " + driver.getUser().getFullName() + " có bằng lái đã hết hạn, không thể phân công",
                ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }
        
        // Check if driver already has an active assignment
        if (vehicleAssignmentEntityService.existsActiveAssignmentForDriver(driver.getId())) {
            log.error("Driver {} ({}) already has an active assignment", driver.getUser().getFullName(), phoneNumber);
            throw new BadRequestException(
                "⚠️ Tài xế " + driver.getUser().getFullName() + " đã được phân công cho chuyến khác, không thể chọn",
                ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Map driver entity to response
        DriverResponse driverResponse = driverMapper.mapDriverResponse(driver);
        
        // Get penalty histories for this driver
        List<PenaltyHistoryResponse> penaltyHistories = penaltyHistoryService.getByDriverId(driver.getId());
        driverResponse.setPenaltyHistories(penaltyHistories);
        
        return driverResponse;
    }

    @Override
    @Transactional
    public DriverResponse renewDriverLicense(UUID driverId, LicenseRenewalRequest request) {
        log.info("🔄 Renewing driver license for driver ID: {}", driverId);

        // Find driver
        DriverEntity driver = driverEntityService.findEntityById(driverId)
                .orElseThrow(() -> {
                    log.error("Driver not found with ID: {}", driverId);
                    return new NotFoundException(
                            "Không tìm thấy tài xế với ID: " + driverId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        // Validate date order: dateOfPassing <= dateOfIssue < dateOfExpiry
        if (request.getDateOfIssue().isBefore(request.getDateOfPassing())) {
            throw new BadRequestException(
                    "Ngày cấp phải sau hoặc bằng ngày sát hạch",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        if (!request.getDateOfExpiry().isAfter(request.getDateOfIssue())) {
            throw new BadRequestException(
                    "Ngày hết hạn phải sau ngày cấp",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        if (!request.getDateOfExpiry().isAfter(LocalDate.now())) {
            throw new BadRequestException(
                    "Ngày hết hạn phải sau ngày hiện tại",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Check if driver was inactive due to expired license
        boolean wasInactiveDueToExpiredLicense = CommonStatusEnum.INACTIVE.name().equals(driver.getStatus()) 
                && isLicenseExpired(driver);

        // Update only date fields (license number, serial, class, place of issue remain unchanged)
        driver.setDateOfPassing(request.getDateOfPassing().atStartOfDay());
        driver.setDateOfIssue(request.getDateOfIssue().atStartOfDay());
        driver.setDateOfExpiry(request.getDateOfExpiry().atStartOfDay());

        // Reactivate driver if was inactive due to expired license
        if (wasInactiveDueToExpiredLicense) {
            driver.setStatus(CommonStatusEnum.ACTIVE.name());
            log.info("✅ Driver {} reactivated after license renewal", driver.getUser().getFullName());
        }

        // Save updated driver
        DriverEntity updatedDriver = driverEntityService.save(driver);
        log.info("✅ Driver license renewed successfully for driver: {}", updatedDriver.getUser().getFullName());

        // Map to response
        DriverResponse driverResponse = driverMapper.mapDriverResponse(updatedDriver);
        List<PenaltyHistoryResponse> penaltyHistories = penaltyHistoryService.getByDriverId(updatedDriver.getId());
        driverResponse.setPenaltyHistories(penaltyHistories);

        return driverResponse;
    }

    @Override
    public boolean isLicenseExpired(DriverEntity driver) {
        if (driver == null || driver.getDateOfExpiry() == null) {
            return false;
        }
        return driver.getDateOfExpiry().toLocalDate().isBefore(LocalDate.now());
    }
}
