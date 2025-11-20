package capstone_project.service.services.user.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.dtos.request.user.UpdateUserRequest;
import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.service.mapper.user.UserMapper;
import capstone_project.service.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserEntityService userEntityService;
    private final UserMapper userMapper;

    @Override
    public UserResponse getUserByUserName(String username) {

        if (username == null || username.isEmpty()) {
            log.error("[getUserByUserName] - Invalid username: {}", username);
            throw new BadRequestException(
                    "Invalid username: " + username,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        UserEntity userEntity = userEntityService.getUserByUserName(username)
                .orElseThrow(() -> {
                    log.error("[getUserByUserName] - User not found with username: {}", username);
                    return new BadRequestException(
                            "User not found with username: " + username,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        return userMapper.mapUserResponse(userEntity);
    }

    @Override
    public UserResponse getUserByEmail(String email) {

        if (email == null || email.isEmpty()) {
            log.error("[getUserByEmail] - Invalid email: {}", email);
            throw new BadRequestException(
                    "Invalid email: " + email,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        UserEntity userEntity = userEntityService.getUserByEmail(email)
                .orElseThrow(() -> {
                    log.error("[getUserByEmail] - User not found with email: {}", email);
                    return new BadRequestException(
                            "User not found with email: " + email,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        return userMapper.mapUserResponse(userEntity);
    }

    @Override
    public UserResponse getUserById(UUID id) {

        if (id == null) {
            log.error("[getUserById] - Invalid id: {}", id);
            throw new BadRequestException(
                    "Invalid id: " + id,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        UserEntity userEntity = userEntityService.getUserById(id)
                .orElseThrow(() -> {
                    log.error("[getUserById] - User not found with id: {}", id);
                    return new BadRequestException(
                            "User not found with id: " + id,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        return userMapper.mapUserResponse(userEntity);
    }

    @Override
    public UserResponse updateUserStatusById(UUID id, String status) {

        if (id == null) {
            log.error("[updateUserStatusById] - Invalid id: {}", id);
            throw new BadRequestException(
                    "Invalid id: " + id,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

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

        UserEntity userEntity = userEntityService.getUserById(id)
                .orElseThrow(() -> {
                    log.error("[updateUserStatusById] - User not found with id: {}", id);
                    return new BadRequestException(
                            "User not found with id: " + id,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        userEntity.setStatus(userStatusEnum.name());
        UserEntity updatedUser = userEntityService.save(userEntity);
        return userMapper.mapUserResponse(updatedUser);
    }

    @Override
    public List<UserResponse> getAllUsers() {

        List<UserEntity> userEntities = userEntityService.findAll();
        if (userEntities.isEmpty()) {
            log.warn("[getAllUsers] - No users found");
            throw new BadRequestException(
                    "No users found",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return userEntities.stream()
                .map(userMapper::mapUserResponse)
                .toList();
    }

    @Override
    public UserResponse updateUser(UUID userId, UpdateUserRequest updateUserRequest) {

        if (userId == null) {
            log.error("[updateUser] - Invalid userId: {}", userId);
            throw new BadRequestException(
                    "Invalid userId: " + userId,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        userEntityService.getUserByEmail(updateUserRequest.email())
                .ifPresent(existingUser -> {
                    if (!existingUser.getId().equals(userId)) {
                        log.error("[updateUser] - Email already in use: {}", updateUserRequest.email());
                        throw new BadRequestException(
                                "Email already in use: " + updateUserRequest.email(),
                                ErrorEnum.ALREADY_EXISTED.getErrorCode()
                        );
                    }
                });

        UserEntity userEntity = userEntityService.getUserById(userId)
                .orElseThrow(() -> {
                    log.error("[updateUser] - User not found with id: {}", userId);
                    return new BadRequestException(
                            "User not found with id: " + userId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        userMapper.toUserEntity(updateUserRequest, userEntity);

        UserEntity updatedUser = userEntityService.save(userEntity);
        return userMapper.mapUserResponse(updatedUser);
    }

    @Override
    public UserResponse updateUserStatus(String email, String status) {

        if (email == null || email.isEmpty()) {
            log.error("[updateUserStatus] - Invalid email: {}", email);
            throw new BadRequestException(
                    "Invalid email: " + email,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        UserEntity userEntity = userEntityService.getUserByEmail(email)
                .orElseThrow(() -> {
                    log.error("[updateUserStatus] - User not found with email: {}", email);
                    return new BadRequestException(
                            "User not found with email: " + email,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        userEntity.setStatus(status);
        UserEntity updatedUser = userEntityService.save(userEntity);
        return userMapper.mapUserResponse(updatedUser);
    }

    @Override
    public List<UserResponse> getUserByUserNameOrEmailLike(String username, String email) {

        if ((username == null || username.isEmpty()) && (email == null || email.isEmpty())) {
            log.error("[getUserByUserNameOrEmailLike] - Both username and email are invalid");
            throw new BadRequestException(
                    "Both username and email are invalid",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        List<UserEntity> userEntities = userEntityService.getUserByUserNameOrEmailLike(username, email);
        if (userEntities.isEmpty()) {
            log.warn("[getUserByUserNameOrEmailLike] - No users found with username like: {} or email like: {}", username, email);
            throw new BadRequestException(
                    "No users found with username like: " + username + " or email like: " + email,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return userEntities.stream()
                .map(userMapper::mapUserResponse)
                .toList();
    }

    @Override
    public List<UserResponse> getUserByRoleRoleName(String roleName) {

        if (roleName == null || roleName.isEmpty()) {
            log.error("[getUserByRoleRoleName] - Invalid roleName: {}", roleName);
            throw new BadRequestException(
                    "Invalid roleName: " + roleName,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        List<UserEntity> userEntities = userEntityService.getUserEntitiesByRoleRoleName(roleName);
        if (userEntities.isEmpty()) {
            log.warn("[getUserByRoleRoleName] - No users found with roleName: {}", roleName);
            throw new BadRequestException(
                    "No users found with roleName: " + roleName,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return userEntities.stream()
                .map(userMapper::mapUserResponse)
                .toList();
    }
}
