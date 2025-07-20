package capstone_project.repository;

import capstone_project.entity.UsersEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends BaseRepository<UsersEntity> {
    /**
     * Find by username optional.
     *
     * @param username the username
     * @return the optional
     */
    Optional<UsersEntity> findByUsername(final String username);

    /**
     * Find by username or email optional.
     *
     * @param username the username
     * @param email    the email
     * @return the optional
     */
    Optional<UsersEntity> findByUsernameOrEmail(final String username, final String email);

    /**
     * Find by email optional.
     *
     * @param email    the email
     * @return the optional
     */
    Optional<UsersEntity> findByEmail(final String email);

    @Query("SELECT u FROM UsersEntity u JOIN FETCH u.role WHERE u.username = :username")
    Optional<UsersEntity> findByUsernameWithRole(@Param("username") String username);
}
