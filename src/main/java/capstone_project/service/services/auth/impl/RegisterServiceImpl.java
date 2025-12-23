package capstone_project.service.services.auth.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.LicenseClassEnum;
import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.common.exceptions.dto.UnauthorizedException;
import capstone_project.common.utils.JWTUtil;
import capstone_project.dtos.request.auth.*;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
import capstone_project.dtos.response.auth.CustomerRegisterResponse;
import capstone_project.dtos.request.user.RegisterDriverRequest;
import capstone_project.dtos.response.auth.ChangePasswordResponse;
import capstone_project.dtos.response.auth.LoginResponse;
import capstone_project.dtos.response.auth.RefreshTokenResponse;
import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.demo.DemoUsersGenerationResponse;
import capstone_project.dtos.response.demo.UpdateUsernamesResponse;
import capstone_project.entity.auth.RefreshTokenEntity;
import capstone_project.entity.auth.RoleEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.entityServices.auth.RefreshTokenEntityService;
import capstone_project.repository.entityServices.auth.RoleEntityService;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import java.util.HashSet;
import java.util.Set;
import java.util.List;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.service.mapper.user.CustomerMapper;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.service.mapper.user.UserMapper;
import capstone_project.service.services.auth.RegisterService;
import capstone_project.service.services.email.EmailProtocolService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegisterServiceImpl implements RegisterService {

    private final UserEntityService userEntityService;
    private final CustomerEntityService customerEntityService;
    private final DriverEntityService driverEntityService;
    private final RoleEntityService roleEntityService;
    private final RefreshTokenEntityService refreshTokenEntityService;
    private final EmailProtocolService emailProtocolService;
    private final capstone_project.service.auth.JwtCacheService jwtCacheService;
    private final capstone_project.service.services.notification.NotificationService notificationService;
    private final capstone_project.service.services.email.EmailNotificationService emailNotificationService;

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final DriverMapper driverMapper;

    private static final String NO_PASSWORD = "NO_PASSWORD";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String TOKEN_TYPE = "Bearer";
    private static final String TEMP_PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789@#$%";
    private static final int TEMP_PASSWORD_LENGTH = 10;

    // Register staff with temporary password and email notification
    @Override
    @Transactional
    public UserResponse register(final RegisterUserRequest registerUserRequest, RoleTypeEnum roleTypeEnum) {

        final String username = registerUserRequest.getUsername();
        final String email = registerUserRequest.getEmail();

        userEntityService.getUserByUserNameOrEmail(username, email).ifPresent(user -> {
            
            throw new BadRequestException(
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getMessage(),
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getErrorCode()
            );
        });

        RoleEntity role = roleEntityService.findByRoleName(roleTypeEnum.name())
                .orElseThrow(() -> new BadRequestException(
                        "Role " + roleTypeEnum.name() + " not found",
                        ErrorEnum.ROLE_NOT_FOUND.getErrorCode()
                ));

        LocalDateTime validatedDob = validateDateFormat(registerUserRequest.getDateOfBirth());
        
        // Generate temporary password for staff accounts
        String password;
        boolean isStaff = roleTypeEnum == RoleTypeEnum.STAFF;
        String userStatus = UserStatusEnum.ACTIVE.name();
        
        if (isStaff) {
            // For staff, generate temporary password and set status to INACTIVE
            password = generateTemporaryPassword();
            userStatus = UserStatusEnum.INACTIVE.name();
            log.info("[register] Generated temporary password for staff: {}", username);
        } else {
            // For other roles, use provided password and set status to ACTIVE
            password = registerUserRequest.getPassword();
        }

        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .fullName(registerUserRequest.getFullName())
                .phoneNumber(registerUserRequest.getPhoneNumber())
                .gender(registerUserRequest.getGender())
                .imageUrl(registerUserRequest.getImageUrl())
                .dateOfBirth(validatedDob.toLocalDate())
                .status(userStatus)
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity savedUser = userEntityService.save(user);
        
        // Send email notification for staff accounts
        if (isStaff) {
            sendStaffCredentialsEmail(savedUser, password);
        }

        return userMapper.mapUserResponse(savedUser);
    }

    @Override
    @Transactional
    public CustomerRegisterResponse registerCustomer(final RegisterCustomerRequest registerCustomerRequest) {

        final String username = registerCustomerRequest.getUsername();
        final String email = registerCustomerRequest.getEmail();

        userEntityService.getUserByUserNameOrEmail(username, email).ifPresent(user -> {
            
            throw new BadRequestException(
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getMessage(),
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getErrorCode()
            );
        });

        RoleEntity role = roleEntityService.findByRoleName(RoleTypeEnum.CUSTOMER.name())
                .orElseThrow(() -> new BadRequestException(
                        "Role " + RoleTypeEnum.CUSTOMER.name() + " not found",
                        ErrorEnum.ROLE_NOT_FOUND.getErrorCode()
                ));

        LocalDateTime validatedDob = validateDateFormat(registerCustomerRequest.getDateOfBirth());

        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(registerCustomerRequest.getPassword()))
                .fullName(registerCustomerRequest.getFullName())
                .phoneNumber(registerCustomerRequest.getPhoneNumber())
                .gender(registerCustomerRequest.getGender())
                .imageUrl(registerCustomerRequest.getImageUrl())
                .dateOfBirth(validatedDob.toLocalDate())
                .status(UserStatusEnum.ACTIVE.name())
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity savedUser = userEntityService.save(user);

        CustomerEntity customer = CustomerEntity.builder()
                .companyName(registerCustomerRequest.getCompanyName())
                .representativeName(registerCustomerRequest.getRepresentativeName())
                .representativePhone(registerCustomerRequest.getRepresentativePhone())
                .businessLicenseNumber(registerCustomerRequest.getBusinessLicenseNumber())
                .businessAddress(registerCustomerRequest.getBusinessAddress())
                .createdAt(LocalDateTime.now())
                .status(UserStatusEnum.OTP_PENDING.name())
                .user(savedUser)
                .build();

        CustomerEntity savedCustomer = customerEntityService.save(customer);

        String otp = generateOtp();

        // Gửi OTP qua email - sử dụng @Async nên không cần try-catch ở đây
        // Vì phương thức sendOtpEmail đã được đánh dấu @Async, nó sẽ chạy trong thread riêng
        // và không block luồng chính
        emailProtocolService.sendOtpEmail(savedCustomer.getUser().getEmail(), otp);
        log.info("[registerCustomer] OTP request initiated for email: {}", email);

        // Trả về response với thông tin OTP để frontend biết chuyển đến trang nhập OTP
        CustomerResponse customerResponse = customerMapper.mapCustomerResponse(savedCustomer);
        return CustomerRegisterResponse.withOtpRequired(customerResponse, email);
    }

    @Override
    @Transactional
    public DriverResponse registerDriver(RegisterDriverRequest registerDriverRequest) {

        if (registerDriverRequest == null) {
            log.error("[registerDriver] RegisterDriverRequest is null");
            throw new BadRequestException(ErrorEnum.INVALID.getMessage(), ErrorEnum.INVALID.getErrorCode());
        }

        final String username = registerDriverRequest.getUsername();
        final String email = registerDriverRequest.getEmail();

        userEntityService.getUserByUserNameOrEmail(username, email)
                .ifPresent(user -> {
                    
                    throw new BadRequestException(ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getMessage(),
                            ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getErrorCode());
                });

        RoleEntity role = roleEntityService.findByRoleName(RoleTypeEnum.DRIVER.name())
                .orElseThrow(() -> new BadRequestException(
                        "Role " + RoleTypeEnum.DRIVER.name() + " not found",
                        ErrorEnum.ROLE_NOT_FOUND.getErrorCode()
                ));

        LocalDateTime dob = validateDateFormat(registerDriverRequest.getDateOfBirth());
        LocalDateTime dateOfIssue = validateDateFormat(registerDriverRequest.getDateOfIssue());
        LocalDateTime dateOfExpiry = validateDateFormat(registerDriverRequest.getDateOfExpiry());
        LocalDateTime dateOfPassing = validateDateFormat(registerDriverRequest.getDateOfPassing());

        validateDriverBusinessRules(dob, dateOfIssue, dateOfExpiry, dateOfPassing, registerDriverRequest.getLicenseClass());

        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(registerDriverRequest.getPassword()))
                .fullName(registerDriverRequest.getFullName())
                .phoneNumber(registerDriverRequest.getPhoneNumber())
                .gender(registerDriverRequest.getGender())
                .imageUrl(registerDriverRequest.getImageUrl())
                .dateOfBirth(dob.toLocalDate())
                .status(UserStatusEnum.ACTIVE.name())
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity savedUser = userEntityService.save(user);

        DriverEntity driverEntity = DriverEntity.builder()
                .driverLicenseNumber(registerDriverRequest.getDriverLicenseNumber())
                .identityNumber(registerDriverRequest.getIdentityNumber())
                .cardSerialNumber(registerDriverRequest.getCardSerialNumber())
                .placeOfIssue(registerDriverRequest.getPlaceOfIssue())
                .dateOfIssue(dateOfIssue)
                .dateOfExpiry(dateOfExpiry)
                .licenseClass(registerDriverRequest.getLicenseClass())
                .dateOfPassing(dateOfPassing)
                .createdAt(LocalDateTime.now())
                .status(UserStatusEnum.ACTIVE.name())
                .user(savedUser)
                .build();

        DriverEntity savedDriver = driverEntityService.save(driverEntity);
        return driverMapper.mapDriverResponse(savedDriver);
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
    
    /**
     * Send staff login credentials via email
     */
    private void sendStaffCredentialsEmail(UserEntity staff, String tempPassword) {
        try {
            log.info("[sendStaffCredentialsEmail] Sending login credentials to staff email: {}", staff.getEmail());
            
            // Send email directly using EmailNotificationService
            emailNotificationService.sendStaffCredentialsEmail(
                staff.getEmail(),
                staff.getFullName(),
                staff.getUsername(),
                tempPassword
            );
            
            // Create notification record for tracking
            capstone_project.dtos.request.notification.CreateNotificationRequest notificationRequest = 
                capstone_project.dtos.request.notification.CreateNotificationRequest.builder()
                    .userId(staff.getId())
                    .recipientRole("STAFF")
                    .notificationType(capstone_project.common.enums.NotificationTypeEnum.STAFF_CREATED)
                    .title("Tài khoản Truckie đã được tạo - Thông tin đăng nhập")
                    .description(String.format(
                        "Chào %s,%n%n" +
                        "Tài khoản nhân viên của bạn đã được tạo thành công trên hệ thống Truckie.%n%n" +
                        "Thông tin đăng nhập:%n" +
                        "• Tên đăng nhập: %s%n" +
                        "• Mật khẩu tạm thời: %s%n%n" +
                        "Vui lòng đăng nhập và đổi mật khẩu mới.%n%n" +
                        "Trân trọng,%n" +
                        "Đội ngũ Truckie",
                        staff.getFullName(),
                        staff.getUsername(),
                        tempPassword
                    ))
                    .build();

            notificationService.createNotification(notificationRequest);
            
            log.info("[sendStaffCredentialsEmail] Login credentials sent to staff email: {}", staff.getEmail());
        } catch (Exception e) {
            log.error("[sendStaffCredentialsEmail] Failed to send credentials email to staff: {}", staff.getEmail(), e);
            // Don't throw exception - staff creation should still succeed even if email fails
        }
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

    @Override
    public LoginResponse login(LoginWithoutEmailRequest loginRequest) {
        
        final var username = loginRequest.getUsername();
        final var userByUserName = userEntityService.getUserByUserName(username);

        if (userByUserName.isPresent()) {
            final var usersEntity = userByUserName.get();

            if (passwordEncoder.matches(loginRequest.getPassword(), usersEntity.getPassword())) {

                // Check if user is INACTIVE or OTP_PENDING
                boolean isInactive = Objects.equals(usersEntity.getStatus(), UserStatusEnum.INACTIVE.name());
                boolean isOtpPending = Objects.equals(usersEntity.getStatus(), UserStatusEnum.OTP_PENDING.name());
                boolean isDriver = usersEntity.getRole() != null 
                        && RoleTypeEnum.DRIVER.name().equals(usersEntity.getRole().getRoleName());
                boolean isStaff = usersEntity.getRole() != null 
                        && RoleTypeEnum.STAFF.name().equals(usersEntity.getRole().getRoleName());
                boolean isCustomer = usersEntity.getRole() != null 
                        && RoleTypeEnum.CUSTOMER.name().equals(usersEntity.getRole().getRoleName());

                // Block INACTIVE customers (waiting for admin activation)
                if (isInactive && isCustomer) {
                    log.warn("[login] Customer {} attempted to login but account is INACTIVE (waiting for admin activation)", username);
                    throw new BadRequestException(
                            "Tài khoản của bạn đang chờ được kích hoạt bởi quản trị viên",
                            ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
                    );
                }
                
                // Block OTP_PENDING customers (need to verify OTP first)
                if (isOtpPending && isCustomer) {
                    log.warn("[login] Customer {} attempted to login but account is OTP_PENDING (needs OTP verification)", username);
                    throw new BadRequestException(
                            "Vui lòng xác thực OTP trước khi đăng nhập",
                            ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
                    );
                }
                
                // Allow INACTIVE drivers and staff to login for onboarding, block other INACTIVE users
                if (isInactive && !isDriver && !isStaff) {
                    throw new BadRequestException(
                            ErrorEnum.USER_PERMISSION_DENIED.getMessage(),
                            ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
                    );
                }

                // Block DELETED, BANNED users
                if (Objects.equals(usersEntity.getStatus(), UserStatusEnum.DELETED.name()) 
                        || Objects.equals(usersEntity.getStatus(), UserStatusEnum.BANNED.name())) {
                    throw new BadRequestException(
                            ErrorEnum.USER_PERMISSION_DENIED.getMessage(),
                            ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
                    );
                }

                List<RefreshTokenEntity> oldTokens = refreshTokenEntityService.findByUserIdAndRevokedFalse(usersEntity.getId());

                oldTokens.forEach(refreshTokenEntity -> {
                    refreshTokenEntity.setRevoked(true);
                });

                refreshTokenEntityService.saveAll(oldTokens);

                final var accessToken = JWTUtil.generateToken(usersEntity);
                final var refreshTokenString = JWTUtil.generateRefreshToken(usersEntity);

                RefreshTokenEntity newRefreshToken = RefreshTokenEntity.builder()
                        .token(refreshTokenString)
                        .user(usersEntity)
                        .createdAt(LocalDateTime.now())
                        .expiredAt(LocalDateTime.now().plusDays(30))
                        .revoked(false)
                        .build();

                refreshTokenEntityService.save(newRefreshToken);

                // Build response with firstTimeLogin flag for INACTIVE drivers and staff
                LoginResponse response = userMapper.mapLoginResponse(usersEntity, accessToken, refreshTokenString);
                
                if (isInactive) {
                    response.setFirstTimeLogin(true);
                    
                    if (isDriver) {
                        // Drivers need to change password and upload face image
                        response.setRequiredActions(Arrays.asList("CHANGE_PASSWORD", "UPLOAD_FACE"));
                        log.info("[login] Driver {} logged in for first time onboarding", username);
                    } else if (isStaff) {
                        // Staff only need to change password
                        response.setRequiredActions(Arrays.asList("CHANGE_PASSWORD"));
                        log.info("[login] Staff {} logged in for first time onboarding", username);
                    }
                } else {
                    response.setFirstTimeLogin(false);
                    response.setRequiredActions(null);
                }

                return response;
            }
            
            throw new NotFoundException(
                    ErrorEnum.LOGIN_WRONG_PASSWORD.getMessage(),
                    ErrorEnum.LOGIN_WRONG_PASSWORD.getErrorCode()
            );
        }
        
        throw new NotFoundException(
                ErrorEnum.LOGIN_NOT_FOUND_USER_NAME_OR_EMAIL.getMessage(),
                ErrorEnum.LOGIN_NOT_FOUND_USER_NAME_OR_EMAIL.getErrorCode()
        );
    }

    @Override
    public LoginResponse loginWithGoogle(final RegisterUserRequest registerUserRequest) {

        final var username = registerUserRequest.getUsername();
        final var email = registerUserRequest.getEmail();
        final var userByEmail = userEntityService.getUserByEmail(email);

        if (Objects.isNull(email) || email.isBlank()) {
            log.warn("[loginWithGoogle] Email is null or empty");
            throw new BadRequestException(ErrorEnum.NULL.getMessage(),
                    ErrorEnum.NULL.getErrorCode()
            );
        }

        if (userByEmail.isPresent()) {
            UserEntity existingUser = userByEmail.get();

            if (!"NO_PASSWORD".equals(existingUser.getPassword())) {
                
                throw new BadRequestException(ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getMessage(),
                        ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getErrorCode()
                );
            }

            return handlerLoginWithGoogleLogic(new LoginWithGoogleRequest(existingUser.getEmail()));
        }

        RoleEntity role = roleEntityService.findByRoleName(RoleTypeEnum.CUSTOMER.name())
                .orElseThrow(() -> new BadRequestException(
                        "Role CUSTOMER not found",
                        ErrorEnum.ROLE_NOT_FOUND.getErrorCode()
                ));

        LocalDateTime validatedDob = validateDateFormat(registerUserRequest.getDateOfBirth());

        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .password(NO_PASSWORD)
                .fullName(registerUserRequest.getFullName())
                .phoneNumber(registerUserRequest.getPhoneNumber())
                .gender(registerUserRequest.getGender())
                .imageUrl(registerUserRequest.getImageUrl())
                .dateOfBirth(validatedDob.toLocalDate())
                .status(UserStatusEnum.ACTIVE.name())
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();

        userEntityService.save(user);

        return handlerLoginWithGoogleLogic(new LoginWithGoogleRequest(registerUserRequest.getEmail()));
    }

    private LoginResponse handlerLoginWithGoogleLogic(LoginWithGoogleRequest loginWithGoogleRequest) {
        
        final var email = loginWithGoogleRequest.getEmail();
        final var userByEmail = userEntityService.getUserByEmail(email);

        if (userByEmail.isPresent()) {
            final var usersEntity = userByEmail.get();

            // Revoke all old refresh tokens for this user (same as normal login)
            List<RefreshTokenEntity> oldTokens = refreshTokenEntityService.findByUserIdAndRevokedFalse(usersEntity.getId());

            oldTokens.forEach(refreshTokenEntity -> {
                refreshTokenEntity.setRevoked(true);
            });

            refreshTokenEntityService.saveAll(oldTokens);

            final var token = JWTUtil.generateToken(usersEntity);
            final var refreshTokenString = JWTUtil.generateRefreshToken(usersEntity);

            // FIX: Use .user() instead of .id()
            RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                    .token(refreshTokenString)
                    .user(usersEntity)
                    .createdAt(LocalDateTime.now())
                    .expiredAt(LocalDateTime.now().plusDays(30))
                    .revoked(false)
                    .build();

            refreshTokenEntityService.save(refreshTokenEntity);
            
            return userMapper.mapLoginResponse(usersEntity, token, refreshTokenString);

        }
        
        return null;
    }

    @Override
    @Transactional
    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        
        LocalDateTime now = LocalDateTime.now();
        
        // CRITICAL FIX: Use findByTokenAndRevokedFalse to avoid "Query did not return a unique result" error
        // This filters out revoked tokens and returns only the active one
        RefreshTokenEntity tokenEntity = refreshTokenEntityService.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> {
                    log.error("[refreshAccessToken] Active token not found in database");
                    return new UnauthorizedException(ErrorEnum.UNAUTHORIZED);
                });

        // Check expiration (revoked is already filtered by query)
        if (tokenEntity.getExpiredAt().isBefore(now)) {
            log.warn("[refreshAccessToken] ❌ Refresh token has expired - expired_at: {}", tokenEntity.getExpiredAt());
            throw new UnauthorizedException(ErrorEnum.UNAUTHORIZED);
        }

        UserEntity user = userEntityService.findEntityById(tokenEntity.getUser().getId())
                .orElseThrow(() -> {
                    log.error("[refreshAccessToken] User not found");
                    return new UnauthorizedException(ErrorEnum.UNAUTHORIZED);
                });

        // SECURITY: Implement token rotation
        // 1. Revoke the old refresh token to prevent reuse
        tokenEntity.setRevoked(true);
        refreshTokenEntityService.save(tokenEntity);

        // 2. Generate new access token AND new refresh token
        String newAccessToken = JWTUtil.generateToken(user);
        String newRefreshToken = JWTUtil.generateRefreshToken(user);

        // 3. Save new refresh token to database
        RefreshTokenEntity newTokenEntity = RefreshTokenEntity.builder()
                .token(newRefreshToken)
                .user(user)
                .createdAt(LocalDateTime.now())
                .expiredAt(LocalDateTime.now().plusDays(30))
                .revoked(false)
                .build();
        refreshTokenEntityService.save(newTokenEntity);

        // Return BOTH new tokens (token rotation for security)
        return userMapper.mapRefreshTokenResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    public String extractRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            Optional<Cookie> refreshTokenCookie = Arrays.stream(cookies)
                    .filter(c -> "refreshToken".equals(c.getName()))
                    .findFirst();

            if (refreshTokenCookie.isPresent()) {
                return refreshTokenCookie.get().getValue();
            }
        }

        log.warn("[extractRefreshTokenFromCookies] Refresh token not found in cookies");
        throw new UnauthorizedException(ErrorEnum.UNAUTHORIZED);
    }

    @Override
    @Deprecated
    public RefreshTokenResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest) {
        return refreshAccessToken(refreshTokenRequest.getRefreshToken());
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest changePasswordRequest) {

        UserEntity user = validationForChangePassword(changePasswordRequest);

        user.setPassword(passwordEncoder.encode(changePasswordRequest.newPassword()));
        userEntityService.save(user);

        return new ChangePasswordResponse("Change password successful");
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePasswordForForgetPassword(ChangePasswordForForgetPassRequest changePasswordForForgetPassRequest) {

        UserEntity user = validationForChangePasswordForForgetPassword(changePasswordForForgetPassRequest);

        user.setPassword(passwordEncoder.encode(changePasswordForForgetPassRequest.newPassword()));
        userEntityService.save(user);

        return new ChangePasswordResponse("Change password successful");
    }

    private UserEntity validationForChangePasswordForForgetPassword(ChangePasswordForForgetPassRequest changePasswordForForgetPassRequest) {
        if (changePasswordForForgetPassRequest == null) {
            log.error("[changePasswordForForgetPassword] ChangePasswordForForgetPassRequest is null");
            throw new BadRequestException(
                    ErrorEnum.NULL.getMessage(),
                    ErrorEnum.NULL.getErrorCode()
            );
        }

        // Support lookup by email when username is null (for forgot password flow)
        UserEntity user;
        if (changePasswordForForgetPassRequest.username() != null && !changePasswordForForgetPassRequest.username().isBlank()) {
            user = userEntityService.getUserByUserName(changePasswordForForgetPassRequest.username())
                    .orElseThrow(() -> new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
        } else if (changePasswordForForgetPassRequest.email() != null && !changePasswordForForgetPassRequest.email().isBlank()) {
            user = userEntityService.getUserByEmail(changePasswordForForgetPassRequest.email())
                    .orElseThrow(() -> new NotFoundException(
                            "Không tìm thấy tài khoản với email này",
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
        } else {
            throw new BadRequestException(
                    "Vui lòng cung cấp username hoặc email",
                    ErrorEnum.NULL.getErrorCode()
            );
        }

        // IMPORTANT: Do not allow forget password for INACTIVE drivers.
        // First-time password change for drivers must go through the dedicated
        // onboarding flow (password + face image in a single API call).
        boolean isInactive = Objects.equals(user.getStatus(), UserStatusEnum.INACTIVE.name());
        boolean isDriver = user.getRole() != null
                && RoleTypeEnum.DRIVER.name().equals(user.getRole().getRoleName());

        if (isInactive && isDriver) {
            log.warn("[changePasswordForForgetPassword] INACTIVE DRIVER attempted to use forgot password flow. Username: {}", user.getUsername());
            throw new BadRequestException(
                    "Tài xế cần hoàn tất bước kích hoạt (đổi mật khẩu + chụp ảnh khuôn mặt) trong quy trình onboarding.",
                    ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
            );
        }

        if (!Objects.equals(changePasswordForForgetPassRequest.newPassword(), changePasswordForForgetPassRequest.confirmNewPassword())) {
            log.error("[changePasswordForForgetPassword] New password and confirm new password do not match");
            throw new BadRequestException(
                    ErrorEnum.PASSWORD_CONFIRM_NOT_MATCH.getMessage(),
                    ErrorEnum.PASSWORD_CONFIRM_NOT_MATCH.getErrorCode()
            );
        }
        return user;
    }

    private UserEntity validationForChangePassword(ChangePasswordRequest changePasswordRequest) {
        if (changePasswordRequest == null) {
            log.error("[changePassword] ChangePasswordRequest is null");
            throw new BadRequestException(
                    ErrorEnum.NULL.getMessage(),
                    ErrorEnum.NULL.getErrorCode()
            );
        }

        UserEntity user = userEntityService.getUserByUserName(changePasswordRequest.username())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // IMPORTANT: Do not allow normal changePassword for INACTIVE drivers.
        // First-time password change for drivers must go through the dedicated
        // onboarding flow (password + face image in a single API call).
        boolean isInactive = Objects.equals(user.getStatus(), UserStatusEnum.INACTIVE.name());
        boolean isDriver = user.getRole() != null
                && RoleTypeEnum.DRIVER.name().equals(user.getRole().getRoleName());

        if (isInactive && isDriver) {
            log.warn("[changePassword] INACTIVE DRIVER attempted to change password via normal endpoint. Username: {}", user.getUsername());
            throw new BadRequestException(
                    "Tài xế cần hoàn tất bước kích hoạt (đổi mật khẩu + chụp ảnh khuôn mặt) trong quy trình onboarding.",
                    ErrorEnum.USER_PERMISSION_DENIED.getErrorCode()
            );
        }

        // Kiểm tra mật khẩu cũ, trừ khi đây là lần đăng nhập đầu tiên (status = INACTIVE)
        boolean isFirstTimeLogin = Objects.equals(user.getStatus(), UserStatusEnum.INACTIVE.name());
        
        if (!isFirstTimeLogin && !passwordEncoder.matches(changePasswordRequest.oldPassword(), user.getPassword())) {
            log.error("[changePassword] Current password is incorrect");
            throw new BadRequestException(
                    ErrorEnum.OLD_PASSWORD_IS_INCORRECT.getMessage(),
                    ErrorEnum.OLD_PASSWORD_IS_INCORRECT.getErrorCode()
            );
        }
        
        // Nếu là lần đăng nhập đầu tiên, cập nhật status thành ACTIVE
        if (isFirstTimeLogin) {
            log.info("[changePassword] First time login detected, updating user status to ACTIVE");
            user.setStatus(UserStatusEnum.ACTIVE.name());
        }

        if (Objects.equals(changePasswordRequest.oldPassword(), changePasswordRequest.newPassword())) {
            log.error("[changePassword] New password must be different from old password");
            throw new BadRequestException(
                    ErrorEnum.NEW_PASSWORD_MUST_BE_DIFFERENT_OLD_PASSWORD.getMessage(),
                    ErrorEnum.NEW_PASSWORD_MUST_BE_DIFFERENT_OLD_PASSWORD.getErrorCode()
            );
        }

        if (!Objects.equals(changePasswordRequest.newPassword(), changePasswordRequest.confirmNewPassword())) {
            log.error("[changePassword] New password and confirm new password do not match");
            throw new BadRequestException(
                    ErrorEnum.PASSWORD_CONFIRM_NOT_MATCH.getMessage(),
                    ErrorEnum.PASSWORD_CONFIRM_NOT_MATCH.getErrorCode()
            );
        }
        return user;
    }

    public LocalDateTime validateDateFormat(String dateOfBirthStr) {
        try {
            if (dateOfBirthStr.isEmpty()) {
                log.error("[validateDateFormat] Date string is empty");
                throw new BadRequestException(
                        ErrorEnum.NULL.getMessage(),
                        ErrorEnum.NULL.getErrorCode()
                );
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(dateOfBirthStr, formatter);
            return localDate.atStartOfDay();
        } catch (DateTimeParseException e) {
            log.error("[validateDateFormat] Invalid date format: {}", dateOfBirthStr, e);
            throw new BadRequestException(
                    ErrorEnum.INVALID_DATE_FORMAT.getMessage(),
                    ErrorEnum.INVALID_DATE_FORMAT.getErrorCode()
            );
        }
    }

    @Override
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, refreshToken);
        cookie.setHttpOnly(true);
        // Only set secure flag for HTTPS (production)
        // For localhost development, secure=false allows HTTP cookies
        boolean isProduction = System.getenv("ENVIRONMENT") != null && System.getenv("ENVIRONMENT").equals("production");
        cookie.setSecure(isProduction);
        cookie.setPath("/");
        cookie.setMaxAge(7 * 24 * 60 * 60); // 7 days, should match your token expiration
        
        // Add cookie via standard method
        response.addCookie(cookie);
        
        // IMPORTANT: Add Set-Cookie header with SameSite=Lax to support page refresh
        // This allows cookies to be sent on same-site requests (including page refresh)
        // SameSite=Lax is safer than SameSite=None and works for page refresh scenarios
        String sameSiteValue = isProduction ? "SameSite=None; Secure" : "SameSite=Lax";
        String setCookieHeader = String.format(
            "%s=%s; Path=/; HttpOnly; %s; Max-Age=%d",
            REFRESH_TOKEN_COOKIE_NAME,
            refreshToken,
            sameSiteValue,
            7 * 24 * 60 * 60
        );
        response.addHeader("Set-Cookie", setCookieHeader);

    }

    public String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }

    @Override
    public boolean logout(HttpServletRequest request, HttpServletResponse response) {
        
        try {
            String refreshToken = extractRefreshTokenFromCookies(request);
            
            // SECURITY: Extract and blacklist access token from request
            String accessToken = extractAccessTokenFromRequest(request);
            
            boolean result = logout(refreshToken, accessToken);

            // Clear the refresh token cookie
            Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE_NAME, "");
            cookie.setMaxAge(0); // Delete the cookie
            cookie.setPath("/");
            cookie.setHttpOnly(true);
            // Only set secure flag for HTTPS (production)
            boolean isProduction = System.getenv("ENVIRONMENT") != null && System.getenv("ENVIRONMENT").equals("production");
            cookie.setSecure(isProduction);
            response.addCookie(cookie);
            
            // Also add Set-Cookie header to ensure cookie is cleared
            String sameSiteValue = isProduction ? "SameSite=None; Secure" : "SameSite=Lax";
            String setCookieHeader = String.format(
                "%s=; Path=/; HttpOnly; %s; Max-Age=0",
                REFRESH_TOKEN_COOKIE_NAME,
                sameSiteValue
            );
            response.addHeader("Set-Cookie", setCookieHeader);

            return result;
        } catch (Exception e) {
            log.error("[logout] Error during logout: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean logout(String refreshToken) {
        return logout(refreshToken, null);
    }
    
    /**
     * Logout with both refresh and access token
     * @param refreshToken the refresh token to revoke
     * @param accessToken the access token to blacklist (optional)
     * @return true if logout successful
     */
    @Transactional
    public boolean logout(String refreshToken, String accessToken) {

        if (refreshToken == null || refreshToken.isEmpty()) {
            log.warn("[logout] No refresh token provided");
            return false;
        }

        try {
            // Use findByTokenAndRevokedFalse to avoid duplicate token issues
            Optional<RefreshTokenEntity> tokenEntity = refreshTokenEntityService.findByTokenAndRevokedFalse(refreshToken);

            if (tokenEntity.isEmpty()) {
                log.warn("[logout] Active refresh token not found: {}", refreshToken);
                return false;
            }

            RefreshTokenEntity token = tokenEntity.get();
            UUID userId = token.getUser().getId();
            String username = token.getUser().getUsername();

            // SECURITY: Revoke ALL active refresh tokens for this user
            List<RefreshTokenEntity> userTokens = refreshTokenEntityService.findByUserIdAndRevokedFalse(userId);

            for (RefreshTokenEntity userToken : userTokens) {
                userToken.setRevoked(true);
            }
            
            refreshTokenEntityService.saveAll(userTokens);
            
            // SECURITY: Blacklist the access token if provided
            if (accessToken != null && !accessToken.isEmpty()) {
                try {
                    long expirationSeconds = JWTUtil.getExpirationSeconds(accessToken);
                    jwtCacheService.blacklistToken(accessToken, expirationSeconds);
                    log.info("[logout] ✅ Access token blacklisted for user: {}", username);
                } catch (Exception e) {
                    log.warn("[logout] Failed to blacklist access token: {}", e.getMessage());
                }
            }
            
            // Clear user cache
            jwtCacheService.clearUserCache(username);
            
            log.info("[logout] ✅ User logged out successfully: {}", username);

            return true;
        } catch (Exception e) {
            log.error("[logout] Error revoking tokens: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Extract access token from request (header or cookie)
     */
    private String extractAccessTokenFromRequest(HttpServletRequest request) {
        // Try Authorization header first
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // Try cookies
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> "accessToken".equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        
        return null;
    }

    @Override
    @Transactional
    public DemoUsersGenerationResponse generateDemoUsers() {
        log.info("🎯 Starting demo users generation for December 2025");

        // Vietnamese first names
        String[] firstNames = {
            "Minh", "Hùng", "Tài", "Dũng", "Tuấn", "Hoàng", "Long", "Phúc", "Thành", "Đức",
            "Quang", "Hải", "Nam", "Bình", "Khang", "Trung", "Vinh", "Sơn", "Kiên", "Phong",
            "Hiếu", "Toàn", "Cường", "Lộc", "Tùng", "Hưng", "Đạt", "Nghĩa", "Thắng", "Tiến",
            "Anh", "Linh", "Hương", "Lan", "Mai", "Nga", "Hà", "Thu", "Trang", "Thảo"
        };

        // Vietnamese middle names
        String[] middleNames = {
            "Văn", "Hữu", "Đức", "Minh", "Quốc", "Thanh", "Ngọc", "Hoàng", "Anh", "Xuân",
            "Thị", "Kim", "Phương", "Thu", "Hồng"
        };

        // Vietnamese last names
        String[] lastNames = {
            "Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Vũ", "Võ", "Đặng", "Bùi", "Đỗ",
            "Hồ", "Ngô", "Dương", "Lý", "Phan", "Huỳnh", "Lương", "Trịnh", "Mai", "Đinh"
        };

        // Phone prefixes
        String[] phonePrefix = {"090", "091", "093", "094", "097", "098", "032", "033", "034", "035", "036", "037", "038", "039"};

        // Company/organization names for customers
        String[] companyNames = {
            "CÔNG TY TNHH THƯƠNG MẠI", "CÔNG TY CỔ PHẦN SẢN XUẤT", "CÔNG TY TNHH LOGISTICS",
            "CÔNG TY CỔ PHẦN VẬN TẢI", "CÔNG TY TNHH XUẤT NHẬP KHẨU", "CÔNG TY CỔ PHẦN PHÂN PHỐI",
            "CÔNG TY TNHH ĐIỆN TỬ", "CÔNG TY CỔ PHẦN THỰC PHẨM", "CÔNG TY TNHH DỆT MAY",
            "CÔNG TY CỔ PHẦN CƠ KHÍ"
        };

        // TPHCM addresses only
        String[] hcmAddresses = {
            "227 Nguyễn Văn Cừ, Quận 5, TP.HCM",
            "123 Lê Lợi, Quận 1, TP.HCM",
            "456 Nguyễn Huệ, Quận 1, TP.HCM",
            "789 Điện Biên Phủ, Quận 3, TP.HCM",
            "321 Cách Mạng Tháng 8, Quận 10, TP.HCM",
            "654 Lý Thường Kiệt, Quận Tân Bình, TP.HCM",
            "987 Trường Chinh, Quận 12, TP.HCM",
            "147 Võ Văn Tần, Quận 3, TP.HCM",
            "258 Hai Bà Trưng, Quận 1, TP.HCM",
            "369 Nguyễn Thị Minh Khai, Quận 3, TP.HCM"
        };

        Random random = new Random();
        Map<String, Integer> usersByDate = new LinkedHashMap<>();
        
        // Common password
        String commonPassword = "truckie123";

        // Get roles
        RoleEntity customerRole = roleEntityService.findByRoleName(RoleTypeEnum.CUSTOMER.name())
                .orElseThrow(() -> new BadRequestException("Role CUSTOMER not found", ErrorEnum.ROLE_NOT_FOUND.getErrorCode()));
        RoleEntity driverRole = roleEntityService.findByRoleName(RoleTypeEnum.DRIVER.name())
                .orElseThrow(() -> new BadRequestException("Role DRIVER not found", ErrorEnum.ROLE_NOT_FOUND.getErrorCode()));
        RoleEntity staffRole = roleEntityService.findByRoleName(RoleTypeEnum.STAFF.name())
                .orElseThrow(() -> new BadRequestException("Role STAFF not found", ErrorEnum.ROLE_NOT_FOUND.getErrorCode()));

        int customerCount = 0;
        int driverCount = 0;
        int staffCount = 0;

        // Track used usernames to ensure uniqueness
        Set<String> usedUsernames = new HashSet<>();
        Set<String> usedEmails = new HashSet<>();

        // Generate Customers (40 users) - distributed throughout December
        for (int i = 1; i <= 40; i++) {
            LocalDateTime createdAt = generateDecemberDate(i, 40, random);
            String dateKey = createdAt.toLocalDate().toString();
            usersByDate.put(dateKey, usersByDate.getOrDefault(dateKey, 0) + 1);

            String firstName = firstNames[random.nextInt(firstNames.length)];
            String middleName = middleNames[random.nextInt(middleNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String fullName = lastName + " " + middleName + " " + firstName;
            
            // Generate username: firstname + last name abbreviation (e.g., datnv for Nguyễn Văn Đạt)
            String firstNameNormalized = removeVietnameseAccents(firstName).toLowerCase();
            String lastNameAbbr = removeVietnameseAccents(lastName).substring(0, 1).toLowerCase();
            String baseUsername = firstNameNormalized + lastNameAbbr;
            
            // Ensure uniqueness by adding suffix only if needed
            String username = baseUsername;
            int suffix = 1;
            while (usedUsernames.contains(username)) {
                username = baseUsername + suffix;
                suffix++;
            }
            usedUsernames.add(username);
            
            String email = username + "@gmail.com";
            usedEmails.add(email);
            
            String phoneNumber = phonePrefix[random.nextInt(phonePrefix.length)] + String.format("%07d", 1000000 + i);
            boolean gender = random.nextBoolean();
            LocalDate dob = LocalDate.of(1985 + random.nextInt(20), 1 + random.nextInt(12), 1 + random.nextInt(28));

            UserEntity user = UserEntity.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(commonPassword))
                    .fullName(fullName)
                    .phoneNumber(phoneNumber)
                    .gender(gender)
                    .dateOfBirth(dob)
                    .status(UserStatusEnum.ACTIVE.name())
                    .role(customerRole)
                    .createdAt(createdAt)
                    .isDemoData(true)
                    .build();

            UserEntity savedUser = userEntityService.save(user);

            // Create CustomerEntity with realistic company info
            String companyName = companyNames[random.nextInt(companyNames.length)] + " " + 
                                removeVietnameseAccents(lastName).toUpperCase() + " " + removeVietnameseAccents(firstName).toUpperCase();
            String businessLicenseNumber = String.format("%010d", 1000000000L + random.nextInt(900000000));
            String businessAddress = hcmAddresses[random.nextInt(hcmAddresses.length)];
            
            CustomerEntity customer = CustomerEntity.builder()
                    .user(savedUser)
                    .companyName(companyName)
                    .businessLicenseNumber(businessLicenseNumber)
                    .businessAddress(businessAddress)
                    .representativeName(fullName)
                    .representativePhone(phoneNumber)
                    .status(UserStatusEnum.ACTIVE.name())
                    .createdAt(createdAt)
                    .isDemoData(true)
                    .build();

            customerEntityService.save(customer);
            customerCount++;
        }

        // Generate Drivers (35 users) - more concentrated on Dec 22-27
        for (int i = 1; i <= 35; i++) {
            LocalDateTime createdAt = generateDecemberDateWithFocus(i, 35, random);
            String dateKey = createdAt.toLocalDate().toString();
            usersByDate.put(dateKey, usersByDate.getOrDefault(dateKey, 0) + 1);

            String firstName = firstNames[random.nextInt(firstNames.length)];
            String middleName = middleNames[random.nextInt(middleNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String fullName = lastName + " " + middleName + " " + firstName;
            
            // Generate username: driver + firstname + last name abbreviation (e.g., driverdatnv for Nguyễn Văn Đạt)
            String firstNameNormalized = removeVietnameseAccents(firstName).toLowerCase();
            String lastNameAbbr = removeVietnameseAccents(lastName).substring(0, 1).toLowerCase();
            String baseUsername = "driver" + firstNameNormalized + lastNameAbbr;
            
            // Ensure uniqueness by adding suffix only if needed
            String username = baseUsername;
            int suffix = 1;
            while (usedUsernames.contains(username)) {
                username = baseUsername + suffix;
                suffix++;
            }
            usedUsernames.add(username);
            
            // Driver email with numbers for uniqueness
            String baseEmail = firstNameNormalized + lastNameAbbr;
            String email = baseEmail + i + "@gmail.com";
            usedEmails.add(email);
            
            String phoneNumber = phonePrefix[random.nextInt(phonePrefix.length)] + String.format("%07d", 2000000 + i);
            boolean gender = random.nextInt(100) < 85; // 85% male drivers
            LocalDate dob = LocalDate.of(1980 + random.nextInt(15), 1 + random.nextInt(12), 1 + random.nextInt(28));

            UserEntity user = UserEntity.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(commonPassword))
                    .fullName(fullName)
                    .phoneNumber(phoneNumber)
                    .gender(gender)
                    .dateOfBirth(dob)
                    .status(UserStatusEnum.ACTIVE.name())
                    .role(driverRole)
                    .createdAt(createdAt)
                    .isDemoData(true)
                    .build();

            UserEntity savedUser = userEntityService.save(user);

            // Create DriverEntity with realistic license info
            String licenseNumber = String.format("%02d", random.nextInt(89) + 10) + "ASX" + String.format("%06d", 100000 + i);
            String identityNumber = String.format("%012d", 100000000000L + random.nextInt(900000000));
            String cardSerialNumber = "HCM-CA-" + String.format("%08d", 10000000 + i);
            String placeOfIssue = "TP.HCM"; // Only TPHCM
            
            LocalDateTime dateOfIssue = createdAt.minusYears(2 + random.nextInt(3));
            LocalDateTime dateOfExpiry = dateOfIssue.plusYears(5);
            LocalDateTime dateOfPassing = dateOfIssue.minusMonths(3);
            String[] licenseClasses = {"B2", "C", "D", "E"};
            String licenseClass = licenseClasses[random.nextInt(licenseClasses.length)];

            DriverEntity driver = DriverEntity.builder()
                    .user(savedUser)
                    .driverLicenseNumber(licenseNumber)
                    .identityNumber(identityNumber)
                    .cardSerialNumber(cardSerialNumber)
                    .placeOfIssue(placeOfIssue)
                    .dateOfIssue(dateOfIssue)
                    .dateOfExpiry(dateOfExpiry)
                    .dateOfPassing(dateOfPassing)
                    .licenseClass(licenseClass)
                    .status(UserStatusEnum.ACTIVE.name())
                    .createdAt(createdAt)
                    .isDemoData(true)
                    .build();

            driverEntityService.save(driver);
            driverCount++;
        }

        // Generate Staff (15 users) - moderate distribution
        for (int i = 1; i <= 15; i++) {
            LocalDateTime createdAt = generateDecemberDate(i, 15, random);
            String dateKey = createdAt.toLocalDate().toString();
            usersByDate.put(dateKey, usersByDate.getOrDefault(dateKey, 0) + 1);

            String firstName = firstNames[random.nextInt(firstNames.length)];
            String middleName = middleNames[random.nextInt(middleNames.length)];
            String lastName = lastNames[random.nextInt(lastNames.length)];
            String fullName = lastName + " " + middleName + " " + firstName;
            
            // Generate username: staff + firstname + last name abbreviation (e.g., staffdatnv for Nguyễn Văn Đạt)
            String firstNameNormalized = removeVietnameseAccents(firstName).toLowerCase();
            String lastNameAbbr = removeVietnameseAccents(lastName).substring(0, 1).toLowerCase();
            String baseUsername = "staff" + firstNameNormalized + lastNameAbbr;
            
            // Ensure uniqueness by adding suffix only if needed
            String username = baseUsername;
            int suffix = 1;
            while (usedUsernames.contains(username)) {
                username = baseUsername + suffix;
                suffix++;
            }
            usedUsernames.add(username);
            
            String email = username + "@truckie.vn";
            usedEmails.add(email);
            
            String phoneNumber = phonePrefix[random.nextInt(phonePrefix.length)] + String.format("%07d", 3000000 + i);
            boolean gender = random.nextBoolean();
            LocalDate dob = LocalDate.of(1990 + random.nextInt(10), 1 + random.nextInt(12), 1 + random.nextInt(28));

            UserEntity user = UserEntity.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(commonPassword))
                    .fullName(fullName)
                    .phoneNumber(phoneNumber)
                    .gender(gender)
                    .dateOfBirth(dob)
                    .status(UserStatusEnum.ACTIVE.name())
                    .role(staffRole)
                    .createdAt(createdAt)
                    .isDemoData(true)
                    .build();

            userEntityService.save(user);
            staffCount++;
        }

        int totalUsers = customerCount + driverCount + staffCount;

        log.info("✅ Demo users generation completed: {} customers, {} drivers, {} staff = {} total users",
                customerCount, driverCount, staffCount, totalUsers);

        return DemoUsersGenerationResponse.builder()
                .totalCustomersCreated(customerCount)
                .totalDriversCreated(driverCount)
                .totalStaffCreated(staffCount)
                .totalUsersCreated(totalUsers)
                .usersByDate(usersByDate)
                .message("Successfully generated " + totalUsers + " demo users for December 2025 dashboard demo")
                .build();
    }

    @Override
    @Transactional
    public UpdateUsernamesResponse updateAllUsernamesToCorrectFormat() {
        log.info("🔄 Starting username format update for all users");
        long startTime = System.currentTimeMillis();

        Map<String, String> usernameChanges = new LinkedHashMap<>();
        int customerCount = 0;
        int driverCount = 0;
        int staffCount = 0;

        // Get all users
        List<UserEntity> allUsers = userEntityService.findAll();
        
        for (UserEntity user : allUsers) {
            String roleName = user.getRole().getRoleName();
            
            // Skip ADMIN users - don't update their usernames
            if ("ADMIN".equals(roleName)) {
                continue;
            }
            
            String oldUsername = user.getUsername();
            String newUsername = generateCorrectUsername(user);
            
            // Always update username and email to correct format (remove condition check)
            // Check if new username already exists
            Optional<UserEntity> existingUser = userEntityService.getUserByUserName(newUsername);
            if (existingUser.isPresent() && !existingUser.get().getId().equals(user.getId())) {
                // Add suffix to make it unique (but not if it's the same user)
                int suffix = 1;
                String uniqueUsername = newUsername;
                while (userEntityService.getUserByUserName(uniqueUsername).isPresent() && 
                       !userEntityService.getUserByUserName(uniqueUsername).get().getId().equals(user.getId())) {
                    uniqueUsername = newUsername + suffix;
                    suffix++;
                }
                newUsername = uniqueUsername;
            }
            
            // Generate new email with number for drivers
            String newEmail = generateCorrectEmail(user, newUsername, roleName);
            
            // Update both username and email
            user.setUsername(newUsername);
            user.setEmail(newEmail);
            userEntityService.save(user);
            usernameChanges.put(oldUsername, newUsername + " (email: " + newEmail + ")");
                
                // Count by role (already filtered out ADMIN above)
                switch (roleName) {
                    case "CUSTOMER":
                        customerCount++;
                        break;
                    case "DRIVER":
                        driverCount++;
                        break;
                    case "STAFF":
                        staffCount++;
                        break;
                }
                
                log.info("Updated username: {} -> {} ({})", oldUsername, newUsername, roleName);
        }

        int totalUpdated = customerCount + driverCount + staffCount;
        long executionTime = System.currentTimeMillis() - startTime;

        log.info("✅ Username update completed: {} customers, {} drivers, {} staff = {} total users updated in {}ms",
                customerCount, driverCount, staffCount, totalUpdated, executionTime);

        return UpdateUsernamesResponse.builder()
                .totalCustomersUpdated(customerCount)
                .totalDriversUpdated(driverCount)
                .totalStaffUpdated(staffCount)
                .totalUsersUpdated(totalUpdated)
                .usernameChanges(usernameChanges)
                .executionTimeMs(executionTime)
                .message("Successfully updated " + totalUpdated + " usernames to correct format")
                .build();
    }

    /**
     * Generate correct username format based on user's full name and role
     */
    private String generateCorrectUsername(UserEntity user) {
        String fullName = user.getFullName();
        String roleName = user.getRole().getRoleName();
        
        if (fullName == null || fullName.trim().isEmpty()) {
            return user.getUsername(); // Keep original if no full name
        }
        
        // Parse Vietnamese full name: "Họ Tên_đệm Tên"
        String[] nameParts = fullName.trim().split("\\s+");
        if (nameParts.length < 2) {
            return user.getUsername(); // Keep original if invalid name format
        }
        
        String lastName = nameParts[0]; // Họ
        String firstName = nameParts[nameParts.length - 1]; // Tên
        
        String firstNameNormalized = removeVietnameseAccents(firstName).toLowerCase();
        String lastNameAbbr = removeVietnameseAccents(lastName).substring(0, 1).toLowerCase();
        
        // Handle middle name if exists (for names with 3+ parts)
        String baseUsername;
        if (nameParts.length >= 3) {
            String middleName = nameParts[1]; // Tên đệm (first middle name)
            String middleNameAbbr = removeVietnameseAccents(middleName).substring(0, 1).toLowerCase();
            baseUsername = firstNameNormalized + lastNameAbbr + middleNameAbbr;
        } else {
            baseUsername = firstNameNormalized + lastNameAbbr;
        }
        
        // Add role prefix for staff and driver
        switch (roleName) {
            case "DRIVER":
                return "driver" + baseUsername;
            case "STAFF":
                return "staff" + baseUsername;
            case "CUSTOMER":
            default:
                return baseUsername;
        }
    }

    /**
     * Generate correct email format based on username and role
     */
    private String generateCorrectEmail(UserEntity user, String username, String roleName) {
        switch (roleName) {
            case "DRIVER":
                // For drivers: add a number to email for uniqueness
                String baseEmail = username.replace("driver", ""); // Remove driver prefix for email
                return baseEmail + user.getId().toString().substring(0, 3) + "@gmail.com";
            case "STAFF":
                return username + "@truckie.vn";
            case "CUSTOMER":
            default:
                return username + "@gmail.com";
        }
    }

    /**
     * Remove Vietnamese accents from a string
     */
    private String removeVietnameseAccents(String str) {
        if (str == null) return "";
        
        String result = str;
        
        // Lowercase vowels with accents
        result = result.replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a");
        result = result.replaceAll("[èéẹẻẽêềếệểễ]", "e");
        result = result.replaceAll("[ìíịỉĩ]", "i");
        result = result.replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o");
        result = result.replaceAll("[ùúụủũưừứựửữ]", "u");
        result = result.replaceAll("[ỳýỵỷỹ]", "y");
        result = result.replaceAll("[đ]", "d");
        
        // Uppercase vowels with accents
        result = result.replaceAll("[ÀÁẠẢÃÂẦẤẬẨẪĂẰẮẶẲẴ]", "A");
        result = result.replaceAll("[ÈÉẸẺẼÊỀẾỆỂỄ]", "E");
        result = result.replaceAll("[ÌÍỊỈĨ]", "I");
        result = result.replaceAll("[ÒÓỌỎÕÔỒỐỘỔỖƠỜỚỢỞỠ]", "O");
        result = result.replaceAll("[ÙÚỤỦŨƯỪỨỰỬỮ]", "U");
        result = result.replaceAll("[ỲÝỴỶỸ]", "Y");
        result = result.replaceAll("[Đ]", "D");
        
        return result;
    }

    /**
     * Generate dates distributed throughout December 2025
     */
    private LocalDateTime generateDecemberDate(int index, int total, Random random) {
        // Spread throughout December 1-31
        int day = 1 + (index * 30 / total) + random.nextInt(2);
        if (day > 31) day = 31;
        
        int hour = 8 + random.nextInt(12); // 8am - 8pm
        int minute = random.nextInt(60);
        int second = random.nextInt(60);
        
        return LocalDateTime.of(2025, 12, day, hour, minute, second);
    }

    /**
     * Generate dates with 60% concentration on Dec 22-27, rest distributed
     */
    private LocalDateTime generateDecemberDateWithFocus(int index, int total, Random random) {
        // 60% of users created between Dec 22-27
        if (random.nextDouble() < 0.6) {
            int day = 22 + random.nextInt(6); // Dec 22-27
            int hour = 8 + random.nextInt(12);
            int minute = random.nextInt(60);
            int second = random.nextInt(60);
            return LocalDateTime.of(2025, 12, day, hour, minute, second);
        } else {
            // Rest distributed throughout December
            return generateDecemberDate(index, total, random);
        }
    }

    @Override
    public boolean isUsernameAvailable(String username) {
        if (username == null || username.isBlank()) {
            return false;
        }
        return userEntityService.getUserByUserName(username).isEmpty();
    }

    @Override
    public boolean isEmailAvailable(String email) {
        if (email == null || email.isBlank()) {
            return false;
        }
        return userEntityService.getUserByEmail(email).isEmpty();
    }
}
