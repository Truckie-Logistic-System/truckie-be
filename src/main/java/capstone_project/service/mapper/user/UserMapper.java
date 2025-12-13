package capstone_project.service.mapper.user;

import capstone_project.dtos.request.user.UpdateUserRequest;
import capstone_project.dtos.response.auth.LoginResponse;
import capstone_project.dtos.response.auth.RefreshTokenResponse;
import capstone_project.dtos.response.auth.UserResponse;
import capstone_project.entity.auth.UserEntity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {
    /**
     * Map user response user response.
     *
     * @param userEntity the users entity
     * @return the user response
     */
    UserResponse mapUserResponse(final UserEntity userEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void toUserEntity(UpdateUserRequest request, @MappingTarget UserEntity entity);

    /**
     * Map login response login response.
     *
     * @param userEntity the users entity
     * @param token       the token
     * @return the login response
     */
    @Mapping(source = "token", target = "authToken")
    @Mapping(source = "refreshToken", target = "refreshToken")
    @Mapping(target = "user", expression = "java(mapUserResponse(userEntity))")
    @Mapping(target = "firstTimeLogin", ignore = true)
    @Mapping(target = "requiredActions", ignore = true)
    LoginResponse mapLoginResponse(final UserEntity userEntity, final String token, final String refreshToken);

    /**
     * Map refresh token response with user info.
     *
     * @param userEntity the users entity
     * @param accessToken the new access token
     * @param refreshToken the new refresh token
     * @return the refresh token response with user info
     */
    @Mapping(source = "accessToken", target = "accessToken")
    @Mapping(source = "refreshToken", target = "refreshToken")
    @Mapping(target = "user", expression = "java(mapUserResponse(userEntity))")
    RefreshTokenResponse mapRefreshTokenResponse(final UserEntity userEntity, final String accessToken, final String refreshToken);
}