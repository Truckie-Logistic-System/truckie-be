package capstone_project.service.services;

import capstone_project.controller.dtos.request.LoginWithoutEmailRequest;
import capstone_project.controller.dtos.request.RefreshTokenRequest;
import capstone_project.controller.dtos.request.RegisterUserRequest;
import capstone_project.controller.dtos.response.LoginResponse;
import capstone_project.controller.dtos.response.RefreshTokenResponse;
import capstone_project.controller.dtos.response.UserResponse;
import capstone_project.enums.RoleType;

/**
 * The interface Registers service.
 */
public interface RegistersService {
    /**
     * Register user response.
     *
     * @param registerUserRequest the register user request
     * @return the user response
     */
    UserResponse register(RegisterUserRequest registerUserRequest, RoleType roleType);

    /**
     * Login login response.
     *
     * @param loginRequest the login request
     * @return the login response
     */
    LoginResponse login(LoginWithoutEmailRequest loginRequest);

    LoginResponse loginWithGoogle(RegisterUserRequest registerUserRequest);

    RefreshTokenResponse refreshAccessToken(RefreshTokenRequest refreshTokenRequest);

    String generateOtp();



}
