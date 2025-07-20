package capstone_project.service.entityServices;


import capstone_project.entity.UsersEntity;

import java.util.Optional;
import java.util.UUID;

public interface UsersEntityService {
    Optional<UsersEntity> getUserById(UUID id);

    /**
     * Gets user by username.
     *
     * @param username the username
     *
     * @return the user by username
     */
    Optional<UsersEntity> getUserByUserName(String username);

    /**
     * Gets user by username or email.
     *
     * @param username the username
     * @param email    the email
     *
     * @return the user by username or email
     */
    Optional<UsersEntity> getUserByUserNameOrEmail(String username, String email);

    /**
     * Gets user by email.
     *
     * @param email the email
     *
     * @return the user by email
     */
    Optional<UsersEntity> getUserByEmail(String email);

    UsersEntity createUser(UsersEntity usersEntity);

    Optional<UsersEntity> getByUsernameWithRole(final String username);
}
