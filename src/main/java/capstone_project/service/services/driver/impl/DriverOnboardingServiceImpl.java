package capstone_project.service.services.driver.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.dtos.request.user.AdminCreateDriverRequest;
import capstone_project.dtos.response.user.DriverCreatedResponse;
import capstone_project.dtos.request.driver.DriverOnboardingRequest;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.auth.RoleEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.service.services.driver.DriverOnboardingService;
import capstone_project.service.services.notification.NotificationService;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.auth.RoleEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.service.services.auth.RoleService;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.common.enums.NotificationTypeEnum;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.context.event.EventListener;
import org.springframework.web.multipart.MultipartFile;
import capstone_project.common.enums.LicenseClassEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;
import java.util.Map;
import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class DriverOnboardingServiceImpl implements DriverOnboardingService {

    private final UserEntityService userEntityService;
    private final DriverEntityService driverEntityService;
    private final RoleEntityService roleEntityService;
    private final RoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final DriverMapper driverMapper;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher applicationEventPublisher;
    private final CloudinaryService cloudinaryService;

    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$%";
    private static final int TEMP_PASSWORD_LENGTH = 10;

    @Override
    @Transactional
    public DriverCreatedResponse createDriverByAdmin(AdminCreateDriverRequest request) {
        log.info("[createDriverByAdmin] Creating driver account for username: {}", request.getUsername());

        // Check if username or email already exists
        userEntityService.getUserByUserNameOrEmail(request.getUsername(), request.getEmail())
                .ifPresent(user -> {
                    throw new BadRequestException(
                            ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getMessage(),
                            ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getErrorCode()
                    );
                });

        // Get DRIVER role
        RoleEntity role = roleEntityService.findByRoleName(RoleTypeEnum.DRIVER.name())
                .orElseThrow(() -> new BadRequestException(
                        "Role DRIVER not found",
                        ErrorEnum.ROLE_NOT_FOUND.getErrorCode()
                ));

        // Generate random temporary password
        String tempPassword = generateTemporaryPassword();
        log.info("[createDriverByAdmin] Generated temporary password for driver: {}", request.getUsername());

        // Parse and validate dates
        LocalDateTime dob = validateDateFormat(request.getDateOfBirth());
        LocalDateTime dateOfIssue = validateDateFormat(request.getDateOfIssue());
        LocalDateTime dateOfExpiry = validateDateFormat(request.getDateOfExpiry());
        LocalDateTime dateOfPassing = validateDateFormat(request.getDateOfPassing());

        validateDriverBusinessRules(dob, dateOfIssue, dateOfExpiry, dateOfPassing, request.getLicenseClass());

        // Create UserEntity with INACTIVE status
        UserEntity user = UserEntity.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(tempPassword))
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .gender(request.getGender())
                .dateOfBirth(dob.toLocalDate())
                .status(UserStatusEnum.INACTIVE.name()) // INACTIVE until onboarding complete
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity savedUser = userEntityService.save(user);

        // Create DriverEntity with INACTIVE status
        DriverEntity driverEntity = DriverEntity.builder()
                .driverLicenseNumber(request.getDriverLicenseNumber())
                .identityNumber(request.getIdentityNumber())
                .cardSerialNumber(request.getCardSerialNumber())
                .placeOfIssue(request.getPlaceOfIssue())
                .dateOfIssue(dateOfIssue)
                .dateOfExpiry(dateOfExpiry)
                .licenseClass(request.getLicenseClass())
                .dateOfPassing(dateOfPassing)
                .createdAt(LocalDateTime.now())
                .status(UserStatusEnum.INACTIVE.name()) // INACTIVE until onboarding complete
                .user(savedUser)
                .build();

        DriverEntity savedDriver = driverEntityService.save(driverEntity);

        log.info("[createDriverByAdmin] Driver created successfully with INACTIVE status. Username: {}", request.getUsername());

        // Send login credentials to driver's email after transaction commits
        applicationEventPublisher.publishEvent(new DriverCreatedEvent(savedUser, tempPassword));

        return DriverCreatedResponse.builder()
                .driver(driverMapper.mapDriverResponse(savedDriver))
                .temporaryPassword(tempPassword)
                .loginInstructions("Tài xế sử dụng tên đăng nhập '" + request.getUsername() + 
                        "' và mật khẩu tạm thời để đăng nhập lần đầu. " +
                        "Khi đăng nhập, tài xế sẽ được yêu cầu đổi mật khẩu và chụp ảnh khuôn mặt để kích hoạt tài khoản.")
                .build();
    }

    @Override
    @Transactional
    public DriverResponse completeOnboarding(DriverOnboardingRequest request) {
        // Get current user from security context
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("[completeOnboarding] Processing onboarding for driver: {}", username);

        UserEntity user = userEntityService.getUserByUserName(username)
                .orElseThrow(() -> new NotFoundException(
                        "User not found",
                        ErrorEnum.USER_BY_ID_NOT_FOUND.getErrorCode()
                ));

        // Verify user is a driver
        if (!RoleTypeEnum.DRIVER.name().equals(user.getRole().getRoleName())) {
            throw new BadRequestException(
                    "Only drivers can complete onboarding",
                    ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
            );
        }

        // Verify user is INACTIVE (needs onboarding)
        if (!UserStatusEnum.INACTIVE.name().equals(user.getStatus())) {
            throw new BadRequestException(
                    "Driver has already completed onboarding",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Verify current password matches
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException(
                    "Current password is incorrect",
                    ErrorEnum.LOGIN_WRONG_PASSWORD.getErrorCode()
            );
        }

        // Validate new password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException(
                    "New password and confirm password do not match",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        if (request.getNewPassword().equals(request.getCurrentPassword())) {
            throw new BadRequestException(
                    "New password must be different from current password",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Get driver entity
        DriverEntity driver = driverEntityService.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Driver not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Update user: password, imageUrl, status
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setImageUrl(request.getFaceImageUrl());
        user.setStatus(UserStatusEnum.ACTIVE.name());
        // Note: updatedAt is managed by JPA auditing
        userEntityService.save(user);

        // Update driver status
        driver.setStatus(UserStatusEnum.ACTIVE.name());
        // Note: updatedAt is managed by JPA auditing
        DriverEntity updatedDriver = driverEntityService.save(driver);

        log.info("[completeOnboarding] Driver onboarding completed successfully. Username: {}", username);

        return driverMapper.mapDriverResponse(updatedDriver);
    }

    @Override
    @Transactional
    public DriverResponse completeOnboardingWithImage(DriverOnboardingRequest request, MultipartFile faceImage) {
        // Get current user from security context
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        log.info("[completeOnboardingWithImage] Processing onboarding for driver: {}", username);

        UserEntity user = userEntityService.getUserByUserName(username)
                .orElseThrow(() -> new NotFoundException(
                        "User not found",
                        ErrorEnum.USER_BY_ID_NOT_FOUND.getErrorCode()
                ));

        // Verify user is a driver
        if (!RoleTypeEnum.DRIVER.name().equals(user.getRole().getRoleName())) {
            throw new BadRequestException(
                    "Only drivers can complete onboarding",
                    ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
            );
        }

        // Verify user is INACTIVE (needs onboarding)
        if (!UserStatusEnum.INACTIVE.name().equals(user.getStatus())) {
            throw new BadRequestException(
                    "Driver has already completed onboarding",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Verify current password matches
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new BadRequestException(
                    "Current password is incorrect",
                    ErrorEnum.LOGIN_WRONG_PASSWORD.getErrorCode()
            );
        }

        // Validate new password
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new BadRequestException(
                    "New password and confirm password do not match",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Get driver entity
        DriverEntity driver = driverEntityService.findByUserId(user.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Driver not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        try {
            // Upload face image to Cloudinary
            String fileName = "driver_face_" + user.getId() + "_" + UUID.randomUUID();
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                    faceImage.getBytes(), 
                    fileName, 
                    "driver_faces"
            );
            
            // Extract secure URL from upload result
            String imageUrl = (String) uploadResult.get("secure_url");
            if (imageUrl == null || imageUrl.isEmpty()) {
                throw new BadRequestException(
                        "Failed to upload face image to Cloudinary",
                        ErrorEnum.INVALID.getErrorCode()
                );
            }
            
            log.info("[completeOnboardingWithImage] Face image uploaded successfully: {}", imageUrl);

            // Update user: password, imageUrl, status
            user.setPassword(passwordEncoder.encode(request.getNewPassword()));
            user.setImageUrl(imageUrl);
            user.setStatus(UserStatusEnum.ACTIVE.name());
            // Note: updatedAt is managed by JPA auditing
            userEntityService.save(user);

            // Update driver status
            driver.setStatus(UserStatusEnum.ACTIVE.name());
            // Note: updatedAt is managed by JPA auditing
            DriverEntity updatedDriver = driverEntityService.save(driver);

            log.info("[completeOnboardingWithImage] Driver onboarding completed successfully. Username: {}", username);

            return driverMapper.mapDriverResponse(updatedDriver);
            
        } catch (IOException e) {
            log.error("[completeOnboardingWithImage] Failed to process face image file: {}", e.getMessage());
            throw new BadRequestException(
                    "Failed to process uploaded image file",
                    ErrorEnum.INVALID.getErrorCode()
            );
        } catch (Exception e) {
            log.error("[completeOnboardingWithImage] Unexpected error during onboarding: {}", e.getMessage());
            throw new BadRequestException(
                    "Failed to complete onboarding",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }
    }

    @Override
    public boolean needsOnboarding() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        
        return userEntityService.getUserByUserName(username)
                .map(user -> UserStatusEnum.INACTIVE.name().equals(user.getStatus()) 
                        && RoleTypeEnum.DRIVER.name().equals(user.getRole().getRoleName()))
                .orElse(false);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleDriverCreatedEvent(DriverCreatedEvent event) {
        log.info("[handleDriverCreatedEvent] Sending driver credentials email after transaction commit");
        sendDriverCredentialsEmail(event.getDriver(), event.getTempPassword());
    }

    /**
     * Generate a random temporary password
     */
    private String generateTemporaryPassword() {
        SecureRandom random = new SecureRandom();
        StringBuilder password = new StringBuilder(TEMP_PASSWORD_LENGTH);
        
        for (int i = 0; i < TEMP_PASSWORD_LENGTH; i++) {
            password.append(TEMP_PASSWORD_CHARS.charAt(random.nextInt(TEMP_PASSWORD_CHARS.length())));
        }
        
        return password.toString();
    }

    private LocalDateTime validateDateFormat(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new BadRequestException("Date is required", ErrorEnum.INVALID.getErrorCode());
        }

        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("dd-MM-yyyy")
        };

        for (DateTimeFormatter formatter : formatters) {
            try {
                return java.time.LocalDate.parse(dateStr, formatter).atStartOfDay();
            } catch (DateTimeParseException ignored) {
            }
        }

        throw new BadRequestException(
                "Invalid date format. Expected: yyyy-MM-dd, dd/MM/yyyy, or dd-MM-yyyy",
                ErrorEnum.INVALID.getErrorCode()
        );
    }

    private void validateDriverBusinessRules(LocalDateTime dob, LocalDateTime dateOfIssue,
                                             LocalDateTime dateOfExpiry, LocalDateTime dateOfPassing,
                                             String licenseClassStr) {
        if (dateOfIssue.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Date of issue cannot be in the future", ErrorEnum.INVALID.getErrorCode());
        }
        if (dateOfPassing.isAfter(dateOfIssue)) {
            throw new BadRequestException("Date of passing cannot be after date of issue", ErrorEnum.INVALID.getErrorCode());
        }
        if (dateOfExpiry.isBefore(dateOfIssue)) {
            throw new BadRequestException("Date of expiry cannot be before date of issue", ErrorEnum.INVALID.getErrorCode());
        }
        if (dateOfExpiry.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Driver license has expired", ErrorEnum.INVALID.getErrorCode());
        }

        LicenseClassEnum licenseClass;
        try {
            licenseClass = LicenseClassEnum.valueOf(licenseClassStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid license class. Allowed values: B2, C",
                    ErrorEnum.INVALID.getErrorCode());
        }

        int ageAtPassing = dob.toLocalDate().until(dateOfPassing.toLocalDate()).getYears();
        switch (licenseClass) {
            case B2 -> {
                if (ageAtPassing < 18)
                    throw new BadRequestException("B2 license requires minimum age of 18", ErrorEnum.INVALID.getErrorCode());
            }
            case C -> {
                if (ageAtPassing < 21)
                    throw new BadRequestException("C license requires minimum age of 21", ErrorEnum.INVALID.getErrorCode());
            }
        }
    }

    /**
     * Send driver login credentials via email
     */
    private void sendDriverCredentialsEmail(UserEntity driver, String tempPassword) {
        try {
            String title = "Tài khoản Truckie đã được tạo - Thông tin đăng nhập";
            String description = String.format(
                "Chào bạn %s,%n%n" +
                "Tài khoản tài xế của bạn đã được tạo thành công trên hệ thống Truckie.%n%n" +
                "Thông tin đăng nhập:%n" +
                "• Tên đăng nhập: %s%n" +
                "• Mật khẩu tạm thời: %s%n%n" +
                "Vui lòng đăng nhập và hoàn tất kích hoạt tài khoản bằng cách:%n" +
                "1. Đăng nhập với thông tin trên%n" +
                "2. Đổi mật khẩu mới%n" +
                "3. Chụp ảnh khuôn mặt để xác minh danh tính%n%n" +
                "Sau khi hoàn tất, tài khoản của bạn sẽ được kích hoạt và bạn có thể nhận đơn hàng.%n%n" +
                "Trân trọng,%n" +
                "Đội ngũ Truckie",
                driver.getFullName(),
                driver.getUsername(),
                tempPassword
            );

            CreateNotificationRequest notificationRequest = CreateNotificationRequest.builder()
                    .userId(driver.getId())
                    .recipientRole("DRIVER")
                    .notificationType(NotificationTypeEnum.DRIVER_CREATED)
                    .title(title)
                    .description(description)
                    .build();

            notificationService.createNotification(notificationRequest);
            
            log.info("[sendDriverCredentialsEmail] Login credentials sent to driver email: {}", driver.getEmail());
        } catch (Exception e) {
            log.error("[sendDriverCredentialsEmail] Failed to send credentials email to driver: {}", driver.getEmail(), e);
            // Don't throw exception - driver creation should still succeed even if email fails
        }
    }
    
    // Event class for driver creation
    private static class DriverCreatedEvent {
        private final UserEntity driver;
        private final String tempPassword;
        
        public DriverCreatedEvent(UserEntity driver, String tempPassword) {
            this.driver = driver;
            this.tempPassword = tempPassword;
        }
        
        public UserEntity getDriver() {
            return driver;
        }
        
        public String getTempPassword() {
            return tempPassword;
        }
    }
}
