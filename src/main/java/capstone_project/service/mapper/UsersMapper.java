package capstone_project.service.mapper;


import capstone_project.controller.dtos.response.LoginResponse;
import capstone_project.controller.dtos.response.UserResponse;
import capstone_project.entity.UsersEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;


@Mapper(componentModel = "spring")
public interface UsersMapper {
    /**
     * Map user response user response.
     *
     * @param usersEntity the users entity
     * @return the user response
     */
    UserResponse mapUserResponse(final UsersEntity usersEntity);

    /**
     * Map login response login response.
     *
     * @param usersEntity the users entity
     * @param token       the token
     * @return the login response
     */
    @Mapping(source = "token", target = "authToken")
    @Mapping(source = "refreshToken", target = "refreshToken")
    @Mapping(target = "user", expression = "java(mapUserResponse(usersEntity))")
    LoginResponse mapLoginResponse(final UsersEntity usersEntity, final String token, final String refreshToken);

}