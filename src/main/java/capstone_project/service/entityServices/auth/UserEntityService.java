package capstone_project.service.entityServices.auth;


import capstone_project.entity.auth.UserEntity;
import capstone_project.service.entityServices.common.BaseEntityService;

import java.util.Optional;
import java.util.UUID;

public interface UserEntityService extends BaseEntityService<UserEntity, UUID> {
//    Optional<UsersEntity> getUserById(UUID id);

    /**
     * Gets user by username.
     *
     * @param username the username
     *
     * @return the user by username
     */
    Optional<UserEntity> getUserByUserName(String username);

    /**
     * Gets user by username or email.
     *
     * @param username the username
     * @param email    the email
     *
     * @return the user by username or email
     */
    Optional<UserEntity> getUserByUserNameOrEmail(String username, String email);

    /**
     * Gets user by email.
     *
     * @param email the email
     *
     * @return the user by email
     */
    Optional<UserEntity> getUserByEmail(String email);

//    UsersEntity createUser(UsersEntity usersEntity);

    Optional<UserEntity> getByUsernameWithRole(final String username);
}
