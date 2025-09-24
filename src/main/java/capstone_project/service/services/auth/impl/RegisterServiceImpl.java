package capstone_project.service.services.auth.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.LicenseClassEnum;
import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.common.utils.JWTUtil;
import capstone_project.dtos.request.auth.*;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
import capstone_project.dtos.request.user.RegisterDriverRequest;
import capstone_project.dtos.response.auth.ChangePasswordResponse;
import capstone_project.dtos.response.auth.LoginResponse;
import capstone_project.dtos.response.auth.RefreshTokenResponse;
import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.dtos.response.user.CustomerResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.entity.auth.RefreshTokenEntity;
import capstone_project.entity.auth.RoleEntity;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.entityServices.auth.RefreshTokenEntityService;
import capstone_project.repository.entityServices.auth.RoleEntityService;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.service.mapper.user.CustomerMapper;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.service.mapper.user.UserMapper;
import capstone_project.service.services.auth.RegisterService;
import capstone_project.service.services.email.EmailProtocolService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

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

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final DriverMapper driverMapper;

    private static final String NO_PASSWORD = "NO_PASSWORD";
    private static final String TOKEN_TYPE = "Bearer";

    // just only for register staff
    @Override
    @Transactional
    public UserResponse register(final RegisterUserRequest registerUserRequest, RoleTypeEnum roleTypeEnum) {
        log.info("[register] Start function");

        final String username = registerUserRequest.getUsername();
        final String email = registerUserRequest.getEmail();

        userEntityService.getUserByUserNameOrEmail(username, email).ifPresent(user -> {
            log.info("[register] Username or Email existed");
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

        UserEntity user = UserEntity.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(registerUserRequest.getPassword()))
                .fullName(registerUserRequest.getFullName())
                .phoneNumber(registerUserRequest.getPhoneNumber())
                .gender(registerUserRequest.getGender())
                .imageUrl(registerUserRequest.getImageUrl())
                .dateOfBirth(validatedDob.toLocalDate())
                .status(UserStatusEnum.ACTIVE.name())
                .role(role)
                .createdAt(LocalDateTime.now())
                .build();

        UserEntity savedUser = userEntityService.save(user);

        return userMapper.mapUserResponse(savedUser);
    }

    @Override
    @Transactional
    public CustomerResponse registerCustomer(final RegisterCustomerRequest registerCustomerRequest) {
        log.info("[register] Start function");

        final String username = registerCustomerRequest.getUsername();
        final String email = registerCustomerRequest.getEmail();

        userEntityService.getUserByUserNameOrEmail(username, email).ifPresent(user -> {
            log.info("[register] Username or Email existed");
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
                .status(UserStatusEnum.OTP_PENDING.name())
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

        try {
            log.info("[register] OTP sent to email: {}", savedCustomer.getUser().getEmail());
            emailProtocolService.sendOtpEmail(savedCustomer.getUser().getEmail(), otp);
        } catch (Exception e) {
            log.error("Failed to send OTP email", e);
            throw new BadRequestException(
                    "Failed to send OTP email",
                    ErrorEnum.INVALID_EMAIL.getErrorCode()
            );
        }

        return customerMapper.mapCustomerResponse(savedCustomer);

    }

    @Override
    @Transactional
    public DriverResponse registerDriver(RegisterDriverRequest registerDriverRequest) {
        log.info("[register] Start function");

        if (registerDriverRequest == null) {
            log.error("[registerDriver] RegisterDriverRequest is null");
            throw new BadRequestException(ErrorEnum.INVALID.getMessage(), ErrorEnum.INVALID.getErrorCode());
        }

        final String username = registerDriverRequest.getUsername();
        final String email = registerDriverRequest.getEmail();

        userEntityService.getUserByUserNameOrEmail(username, email)
                .ifPresent(user -> {
                    log.info("[register] Username or Email existed");
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
        log.info("[login] Start function");
        final var username = loginRequest.getUsername();
        final var userByUserName = userEntityService.getUserByUserName(username);

        if (userByUserName.isPresent()) {
            final var usersEntity = userByUserName.get();

            if (passwordEncoder.matches(loginRequest.getPassword(), usersEntity.getPassword())) {

                if (!Objects.equals(usersEntity.getStatus(), UserStatusEnum.ACTIVE.name())) {
                    log.info("[login] User is not in ACTIVE status, cannot login");
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

                log.info("[login] Login successful");

                return userMapper.mapLoginResponse(usersEntity);
            }
            log.info("[login] Wrong password");
            throw new NotFoundException(
                    ErrorEnum.LOGIN_WRONG_PASSWORD.getMessage(),
                    ErrorEnum.LOGIN_WRONG_PASSWORD.getErrorCode()
            );
        }
        log.info("[login] Username and Email are not found");
        throw new NotFoundException(
                ErrorEnum.LOGIN_NOT_FOUND_USER_NAME_OR_EMAIL.getMessage(),
                ErrorEnum.LOGIN_NOT_FOUND_USER_NAME_OR_EMAIL.getErrorCode()
        );
    }

    @Override
    public LoginResponse loginWithGoogle(final RegisterUserRequest registerUserRequest) {
        log.info("[loginWithGoogle] Start function with email: {}", registerUserRequest.getEmail());

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
                log.info("[loginWithGoogle] Email is already registered with a password. Please log in with your username and password.");
                throw new BadRequestException(ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getMessage(),
                        ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getErrorCode()
                );
            }

            log.info("[loginWithGoogle] User exists, proceeding with login");
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

        log.info("[loginWithGoogle] Create new account & Login successful");

        return handlerLoginWithGoogleLogic(new LoginWithGoogleRequest(registerUserRequest.getEmail()));
    }

    private LoginResponse handlerLoginWithGoogleLogic(LoginWithGoogleRequest loginWithGoogleRequest) {
        log.info("[login] Start function");
        final var email = loginWithGoogleRequest.getEmail();
        final var userByEmail = userEntityService.getUserByEmail(email);

        if (userByEmail.isPresent()) {
            final var usersEntity = userByEmail.get();

            final var token = JWTUtil.generateToken(usersEntity);
            final var refreshTokenString = JWTUtil.generateRefreshToken(usersEntity);

            RefreshTokenEntity refreshTokenEntity = RefreshTokenEntity.builder()
                    .token(refreshTokenString)
                    .id(usersEntity.getId())
                    .createdAt(LocalDateTime.now())
                    .expiredAt(LocalDateTime.now().plusDays(30))
                    .revoked(false)
                    .build();

            refreshTokenEntityService.save(refreshTokenEntity);
            log.info("[loginWithGoogle] login successful");
            return userMapper.mapLoginResponse(usersEntity);

        }
        log.info("[login] Username and Email are not found");
        return null;
    }

    @Override
    public RefreshTokenResponse refreshAccessToken(String refreshToken) {
        RefreshTokenEntity tokenEntity = refreshTokenEntityService.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        LocalDateTime now = LocalDateTime.now();

        if (tokenEntity.getRevoked()) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (tokenEntity.getExpiredAt().isBefore(now)) {
            throw new RuntimeException("Refresh token expired");
        }

        UserEntity user = userEntityService.findEntityById(tokenEntity.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = JWTUtil.generateToken(user);

        return new RefreshTokenResponse(newAccessToken, refreshToken);
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

        throw new RuntimeException("Refresh token not found in cookies");
    }

    @Override
    @Deprecated
    public RefreshTokenResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest) {
        return refreshAccessToken(refreshTokenRequest.getRefreshToken());
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePassword(ChangePasswordRequest changePasswordRequest) {
        log.info("[changePassword] Start function");

        UserEntity user = validationForChangePassword(changePasswordRequest);

        user.setPassword(passwordEncoder.encode(changePasswordRequest.newPassword()));
        userEntityService.save(user);

        return new ChangePasswordResponse("Change password successful");
    }

    @Override
    @Transactional
    public ChangePasswordResponse changePasswordForForgetPassword(ChangePasswordForForgetPassRequest changePasswordForForgetPassRequest) {
        log.info("[changePasswordForForgetPassword] Start function");

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

        UserEntity user = userEntityService.getUserByUserName(changePasswordForForgetPassRequest.username())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

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

        if (!passwordEncoder.matches(changePasswordRequest.oldPassword(), user.getPassword())) {
            log.error("[changePassword] Current password is incorrect");
            throw new BadRequestException(
                    ErrorEnum.OLD_PASSWORD_IS_INCORRECT.getMessage(),
                    ErrorEnum.OLD_PASSWORD_IS_INCORRECT.getErrorCode()
            );
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


    public String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}
