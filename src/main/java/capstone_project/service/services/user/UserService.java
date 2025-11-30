package capstone_project.service.services.user;

import capstone_project.dtos.request.user.UpdateUserRequest;
import capstone_project.dtos.response.auth.UserResponse;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserResponse getUserByUserName(String username);

    UserResponse getUserByEmail(String email);

    UserResponse getUserById(UUID id);

    UserResponse updateUserStatusById(UUID id, String status);

    UserResponse updateUser(UUID userId, UpdateUserRequest updateUserRequest);

    UserResponse updateUserStatus(String email, String status);

    List<UserResponse> getAllUsers();

    List<UserResponse> getUserByUserNameOrEmailLike(final String username, final String email);

    List<UserResponse> getUserByRoleRoleName(final String roleName);

    UserResponse getCurrentUserProfile();

}
