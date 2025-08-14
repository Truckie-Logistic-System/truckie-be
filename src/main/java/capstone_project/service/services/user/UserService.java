package capstone_project.service.services.user;

import capstone_project.dtos.response.auth.UserResponse;

public interface UserService {
    UserResponse updateUserProfile();

    UserResponse updateUserStatus(String email, String status);
}
