package capstone_project.service.services.impl;

import capstone_project.controller.dtos.request.*;
import capstone_project.controller.dtos.response.*;
import capstone_project.entity.*;
import capstone_project.enums.ErrorEnum;
import capstone_project.enums.RoleType;
import capstone_project.exceptions.dto.BadRequestException;
import capstone_project.exceptions.dto.InternalServerException;
import capstone_project.exceptions.dto.NotFoundException;
import capstone_project.service.entityServices.*;
import capstone_project.service.mapper.CustomersMapper;
import capstone_project.service.mapper.DriversMapper;
import capstone_project.service.mapper.UsersMapper;
import capstone_project.service.services.RegistersService;
import capstone_project.utilities.JWTUtil;
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
public class RegistersServiceImpl implements RegistersService {

    private final UsersEntityService usersEntityService;
    private final CustomersEntityService customersEntityService;
    private final DriversEntityService driversEntityService;
    private final RolesEntityService rolesEntityService;
    private final RefreshTokenEntityService refreshTokenEntityService;

    private final PasswordEncoder passwordEncoder;

    private final UsersMapper usersMapper;
    private final CustomersMapper customersMapper;
    private final DriversMapper driversMapper;

    private static final String NO_PASSWORD = "NO_PASSWORD";
    private static final String TOKEN_TYPE = "Bearer";

    // just only for register staff
    @Override
    @Transactional
    public UserResponse register(final RegisterUserRequest registerUserRequest, RoleType roleType) {
        log.info("[register] Start function");

        final String username = registerUserRequest.getUsername();
        final String email = registerUserRequest.getEmail();

        usersEntityService.getUserByUserNameOrEmail(username, email).ifPresent(user -> {
            log.info("[register] Username or Email existed");
            throw new BadRequestException(
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getMessage(),
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getErrorCode()
            );
        });

        try {

            RolesEntity role = rolesEntityService.findByRoleName(roleType.name())
                    .orElseThrow(() -> new InternalServerException(
                            "Role " + roleType.name() + " not found",
                            ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
                    ));

            LocalDateTime validatedDob = validateDateFormat(registerUserRequest.getDateOfBirth());

            UsersEntity user = UsersEntity.builder()
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

            UsersEntity savedUser = usersEntityService.save(user);

            return usersMapper.mapUserResponse(savedUser);

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

        usersEntityService.getUserByUserNameOrEmail(username, email).ifPresent(user -> {
            log.info("[register] Username or Email existed");
            throw new BadRequestException(
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getMessage(),
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getErrorCode()
            );
        });

        try {

            RolesEntity role = rolesEntityService.findByRoleName(RoleType.CUSTOMER.name())
                    .orElseThrow(() -> new InternalServerException(
                            "Role CUSTOMER not found",
                            ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
                    ));

            LocalDateTime validatedDob = validateDateFormat(registerCustomerRequest.getDateOfBirth());

            UsersEntity user = UsersEntity.builder()
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

            UsersEntity savedUser = usersEntityService.save(user);

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

            CustomerEntity savedCustomer = customersEntityService.save(customer);

            return customersMapper.mapCustomerResponse(savedCustomer);

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

        usersEntityService.getUserByUserNameOrEmail(username, email).ifPresent(user -> {
            log.info("[register] Username or Email existed");
            throw new BadRequestException(
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getMessage(),
                    ErrorEnum.USER_NAME_OR_EMAIL_EXISTED.getErrorCode()
            );
        });

        try {

            RolesEntity role = rolesEntityService.findByRoleName(RoleType.DRIVER.name())
                    .orElseThrow(() -> new InternalServerException(
                            "Role CUSTOMER not found",
                            ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
                    ));

            LocalDateTime validatedDob = validateDateFormat(registerDriverRequest.getDateOfBirth());

            UsersEntity user = UsersEntity.builder()
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

            UsersEntity savedUser = usersEntityService.save(user);

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

            DriverEntity savedDriver = driversEntityService.save(driverEntity);

            return driversMapper.mapDriverResponse(savedDriver);

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
        final var userByUserName = usersEntityService.getUserByUserName(username);

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

                return usersMapper.mapLoginResponse(usersEntity, accessToken, refreshTokenString);
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
        final var userByEmail = usersEntityService.getUserByEmail(email);

        if (Objects.isNull(email) || email.isBlank()) {
            log.warn("[loginWithGoogle] Email is null or empty");
            throw new BadRequestException(ErrorEnum.NULL.getMessage(),
                    ErrorEnum.NULL.getErrorCode()
            );
        }

        if (userByEmail.isPresent()) {
            UsersEntity existingUser = userByEmail.get();

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
            RolesEntity role = rolesEntityService.findByRoleName(RoleType.CUSTOMER.name())
                    .orElseThrow(() -> new InternalServerException(
                            "Role CUSTOMER not found",
                            ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
                    ));

            LocalDateTime validatedDob = validateDateFormat(registerUserRequest.getDateOfBirth());

            UsersEntity user = UsersEntity.builder()
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

            usersEntityService.save(user);

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
        final var userByEmail = usersEntityService.getUserByEmail(email);

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
            return usersMapper.mapLoginResponse(usersEntity, token, refreshTokenString);

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

        UsersEntity user = usersEntityService.findById(tokenEntity.getUser().getId())
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
