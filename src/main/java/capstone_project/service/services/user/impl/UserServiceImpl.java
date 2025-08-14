package capstone_project.service.services.user.impl;

import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.service.entityServices.auth.UserEntityService;
import capstone_project.service.mapper.user.UserMapper;
import capstone_project.service.services.user.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserEntityService userEntityService;
    private final UserMapper userMapper;

    @Override
    public UserResponse updateUserStatus(String email, String status) {
        log.info("Updating user status for userId: {}, new status: {}", email, status);

        UserEntity user = userEntityService.getUserByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + email));

        user.setStatus(status);

        UserEntity updatedUser = userEntityService.save(user);
        return userMapper.mapUserResponse(updatedUser);
    }


    @Override
    public UserResponse updateUserProfile() {
        return null;
    }
}
