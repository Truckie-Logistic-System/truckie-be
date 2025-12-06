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
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

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

    private final PasswordEncoder passwordEncoder;

    private final UserMapper userMapper;
    private final CustomerMapper customerMapper;
    private final DriverMapper driverMapper;

    private static final String NO_PASSWORD = "NO_PASSWORD";
    private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";
    private static final String TOKEN_TYPE = "Bearer";

    // just only for register staff
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

        try {
            
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

                if (!Objects.equals(usersEntity.getStatus(), UserStatusEnum.ACTIVE.name())) {
                    
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

                return userMapper.mapLoginResponse(usersEntity, accessToken, refreshTokenString);
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
}
