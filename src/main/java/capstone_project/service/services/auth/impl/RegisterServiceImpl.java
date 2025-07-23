package capstone_project.service.services.auth.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.InternalServerException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.auth.*;
import capstone_project.dtos.request.user.RegisterCustomerRequest;
import capstone_project.dtos.request.user.RegisterDriverRequest;
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
import capstone_project.service.entityServices.auth.RoleEntityService;
import capstone_project.service.entityServices.user.CustomerEntityService;
import capstone_project.service.entityServices.user.DriverEntityService;
import capstone_project.service.entityServices.auth.RefreshTokenEntityService;
import capstone_project.service.entityServices.auth.UserEntityService;
import capstone_project.service.mapper.user.CustomerMapper;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.service.mapper.user.UserMapper;
import capstone_project.service.services.auth.RegisterService;
import capstone_project.common.utils.JWTUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
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

        try {

            RoleEntity role = roleEntityService.findByRoleName(roleTypeEnum.name())
                    .orElseThrow(() -> new InternalServerException(
                            "Role " + roleTypeEnum.name() + " not found",
                            ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
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
                    .status("active")
                    .role(role)
                    .createdAt(LocalDateTime.now())
                    .build();

            UserEntity savedUser = userEntityService.save(user);

            return userMapper.mapUserResponse(savedUser);

        } catch (Exception ex) {
            log.error("[register] Exception: ", ex);
            throw new InternalServerException(
                    ErrorEnum.INTERNAL_SERVER_ERROR.getMessage(),
                    ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
            );
        } finally {
            log.info("[register] End function");
        }
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

        try {

            RoleEntity role = roleEntityService.findByRoleName(RoleTypeEnum.CUSTOMER.name())
                    .orElseThrow(() -> new InternalServerException(
                            "Role CUSTOMER not found",
                            ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
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
                    .status("active")
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
                    .status("active")
                    .user(savedUser)
                    .build();

            CustomerEntity savedCustomer = customerEntityService.save(customer);

            return customerMapper.mapCustomerResponse(savedCustomer);

        } catch (Exception ex) {
            log.error("[register] Exception: ", ex);
            throw new InternalServerException(
                    ErrorEnum.INTERNAL_SERVER_ERROR.getMessage(),
                    ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
            );
        } finally {
            log.info("[register] End function");
        }
    }

    @Override
    @Transactional
    public DriverResponse registerDriver(RegisterDriverRequest registerDriverRequest) {
        log.info("[register] Start function");

        final String username = registerDriverRequest.getUsername();
        final String email = registerDriverRequest.getEmail();

        userEntityService.getUserByUserNameOrEmail(username, email).ifPresent(user -> {
            log.info("[register] Username or Email existed");
            throw new BadRequestException(
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getMessage(),
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getErrorCode()
            );
        });

        try {

            RoleEntity role = roleEntityService.findByRoleName(RoleTypeEnum.DRIVER.name())
                    .orElseThrow(() -> new InternalServerException(
                            "Role CUSTOMER not found",
                            ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
                    ));

            LocalDateTime validatedDob = validateDateFormat(registerDriverRequest.getDateOfBirth());

            UserEntity user = UserEntity.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(registerDriverRequest.getPassword()))
                    .fullName(registerDriverRequest.getFullName())
                    .phoneNumber(registerDriverRequest.getPhoneNumber())
                    .gender(registerDriverRequest.getGender())
                    .imageUrl(registerDriverRequest.getImageUrl())
                    .dateOfBirth(validatedDob.toLocalDate())
                    .status("active")
                    .role(role)
                    .createdAt(LocalDateTime.now())
                    .build();

            UserEntity savedUser = userEntityService.save(user);

            DriverEntity driverEntity = DriverEntity.builder()
                    .driverLicenseNumber(registerDriverRequest.getDriverLicenseNumber())
                    .identityNumber(registerDriverRequest.getIdentityNumber())
                    .cardSerialNumber(registerDriverRequest.getCardSerialNumber())
                    .placeOfIssue(registerDriverRequest.getPlaceOfIssue())
                    .dateOfIssue(validateDateFormat(registerDriverRequest.getDateOfIssue()))
                    .dateOfExpiry(validateDateFormat(registerDriverRequest.getDateOfExpiry()))
                    .licenseClass(registerDriverRequest.getLicenseClass())
                    .dateOfPassing(validateDateFormat(registerDriverRequest.getDateOfPassing()))
                    .createdAt(LocalDateTime.now())
                    .status("active")
                    .user(savedUser)
                    .build();

            DriverEntity savedDriver = driverEntityService.save(driverEntity);

            return driverMapper.mapDriverResponse(savedDriver);

        } catch (Exception ex) {
            log.error("[register] Exception: ", ex);
            throw new InternalServerException(
                    ErrorEnum.INTERNAL_SERVER_ERROR.getMessage(),
                    ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
            );
        } finally {
            log.info("[register] End function");
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

                return userMapper.mapLoginResponse(usersEntity, accessToken, refreshTokenString);
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

        log.info("[loginWithGoogle] User does not exist, creating a new account");
        try {
            RoleEntity role = roleEntityService.findByRoleName(RoleTypeEnum.CUSTOMER.name())
                    .orElseThrow(() -> new InternalServerException(
                            "Role CUSTOMER not found",
                            ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
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
                    .status("active")
                    .role(role)
                    .createdAt(LocalDateTime.now())
                    .build();

            userEntityService.save(user);

            log.info("[loginWithGoogle] Create new account & Login successful");

            return handlerLoginWithGoogleLogic(new LoginWithGoogleRequest(registerUserRequest.getEmail()));

        } catch (Exception ex) {
            log.error("[loginWithGoogle] Failed: ", ex);
            throw new InternalServerException(ErrorEnum.INTERNAL_SERVER_ERROR.getMessage(),
                    ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode());
        } finally {
            log.info("[loginWithGoogle] End function");
        }
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
            return userMapper.mapLoginResponse(usersEntity, token, refreshTokenString);

        }
        log.info("[login] Username and Email are not found");
        return null;
    }

    @Override
    public RefreshTokenResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest) {
        RefreshTokenEntity tokenEntity = refreshTokenEntityService.findByToken(refreshTokenRequest.getRefreshToken())
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        LocalDateTime now = LocalDateTime.now();

        if (tokenEntity.getRevoked()) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (tokenEntity.getExpiredAt().isBefore(now)) {
            throw new RuntimeException("Refresh token expired");
        }

        UserEntity user = userEntityService.findById(tokenEntity.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = JWTUtil.generateToken(user);

        return new RefreshTokenResponse(newAccessToken, refreshTokenRequest.getRefreshToken());
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
