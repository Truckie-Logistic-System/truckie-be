package capstone_project.service.mapper.user;


import capstone_project.dtos.request.user.UpdateUserRequest;
import capstone_project.dtos.response.auth.LoginResponse;
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
    LoginResponse mapLoginResponse(final UserEntity userEntity, final String token, final String refreshToken);



}