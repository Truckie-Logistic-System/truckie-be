package capstone_project.service.services.impl;

import capstone_project.controller.dtos.request.LoginWithGoogleRequest;
import capstone_project.controller.dtos.request.LoginWithoutEmailRequest;
import capstone_project.controller.dtos.request.RefreshTokenRequest;
import capstone_project.controller.dtos.request.RegisterUserRequest;
import capstone_project.controller.dtos.response.LoginResponse;
import capstone_project.controller.dtos.response.RefreshTokenResponse;
import capstone_project.controller.dtos.response.UserResponse;
import capstone_project.enums.ErrorEnum;
import capstone_project.enums.RoleType;
import capstone_project.exceptions.dto.BadRequestException;
import capstone_project.exceptions.dto.InternalServerException;
import capstone_project.exceptions.dto.NotFoundException;
import capstone_project.entity.RefreshTokenEntity;
import capstone_project.entity.RolesEntity;
import capstone_project.entity.UsersEntity;
import capstone_project.service.entityServices.RefreshTokenEntityService;
import capstone_project.service.entityServices.RolesEntityService;
import capstone_project.service.entityServices.UsersEntityService;
import capstone_project.service.mapper.UsersMapper;
import capstone_project.service.services.RegistersService;
import capstone_project.utilities.JWTUtil;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistersServiceImpl implements RegistersService {

    private final UsersEntityService usersEntityService;
    private final RolesEntityService rolesEntityService;
    private final RefreshTokenEntityService refreshTokenEntityService;
    private final PasswordEncoder passwordEncoder;
    private final UsersMapper usersMapper;

    private static final String NO_PASSWORD = "NO_PASSWORD";
    private static final String TOKEN_TYPE = "Bearer";

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

            Date validatedDob = validateDateFormat(registerUserRequest.getDateOfBirth());

            if (registerUserRequest.getPhoneNumber().isEmpty()) {
                log.info("[register] Phone number is empty");
                throw new BadRequestException(
                        ErrorEnum.NULL.getMessage(),
                        ErrorEnum.NULL.getErrorCode()
                );
            }

            UsersEntity user = UsersEntity.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(registerUserRequest.getPassword()))
                    .fullName(registerUserRequest.getFullName())
                    .phoneNumber(registerUserRequest.getPhoneNumber())
                    .gender(registerUserRequest.getGender())
                    .imageUrl(registerUserRequest.getImageUrl())
                    .dateOfBirth(validatedDob)
                    .status("active")
                    .role(role)
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();

            UsersEntity savedUser = usersEntityService.createUser(user);
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

    public Date validateDateFormat(String dateOfBirthStr) {
        try {

            if (dateOfBirthStr.isEmpty()) {
                log.error("[validateDateFormat] Date of Birth is empty");
                throw new BadRequestException(
                        ErrorEnum.NULL.getMessage(),
                        ErrorEnum.NULL.getErrorCode()
                );
            }

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate localDate = LocalDate.parse(dateOfBirthStr, formatter);
            return java.sql.Date.valueOf(localDate);
        } catch (DateTimeParseException e) {
            log.error("[validateDateFormat] Invalid date format: {}", dateOfBirthStr, e);
            throw new BadRequestException(
                    ErrorEnum.INVALID_DATE_FORMAT.getMessage(),
                    ErrorEnum.INVALID_DATE_FORMAT.getErrorCode()
            );
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
                        .createdAt(new Timestamp(System.currentTimeMillis()))
                        .expiredAt(new Timestamp(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                        .revoked(false)
                        .build();

                refreshTokenEntityService.create(newRefreshToken);

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

            Date validatedDob = validateDateFormat(registerUserRequest.getDateOfBirth());

            UsersEntity user = UsersEntity.builder()
                    .username(username)
                    .email(email)
                    .password(NO_PASSWORD)
                    .fullName(registerUserRequest.getFullName())
                    .phoneNumber(registerUserRequest.getPhoneNumber())
                    .gender(registerUserRequest.getGender())
                    .imageUrl(registerUserRequest.getImageUrl())
                    .dateOfBirth(validatedDob)
                    .status("active")
                    .role(role)
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .build();

            usersEntityService.createUser(user);

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
                    .createdAt(new Timestamp(System.currentTimeMillis()))
                    .expiredAt(new Timestamp(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                    .revoked(false)
                    .build();

            refreshTokenEntityService.create(refreshTokenEntity);
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

        Timestamp now = new Timestamp(System.currentTimeMillis());

        if (tokenEntity.getRevoked()) {
            throw new RuntimeException("Refresh token revoked");
        }

        if (now.after(tokenEntity.getExpiredAt())) {
            throw new RuntimeException("Refresh token expired");
        }

        UsersEntity user = usersEntityService.getUserById(tokenEntity.getUser().getId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String newAccessToken = JWTUtil.generateToken(user);

        return new RefreshTokenResponse(newAccessToken, refreshTokenRequest.getRefreshToken());
    }

    public String generateOtp() {
        return String.format("%06d", new Random().nextInt(999999));
    }
}
