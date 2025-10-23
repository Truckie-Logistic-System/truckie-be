package capstone_project.repository.repositories.auth;

import capstone_project.entity.auth.UserEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends BaseRepository<UserEntity> {
    /**
     * Find by username optional.
     *
     * @param username the username
     * @return the optional
     */
    Optional<UserEntity> findByUsername(final String username);

    /**
     * Find by username or email optional.
     *
     * @param username the username
     * @param email    the email
     * @return the optional
     */
    Optional<UserEntity> findByUsernameOrEmail(final String username, final String email);

    /**
     * Find by email optional.
     *
     * @param email    the email
     * @return the optional
     */
    Optional<UserEntity> findByEmail(final String email);

    @Query("SELECT u FROM UserEntity u JOIN FETCH u.role WHERE u.username = :username")
    Optional<UserEntity> findByUsernameWithRole(@Param("username") String username);

    @Modifying
    @Query("UPDATE UserEntity u SET u.status = :status WHERE u.email = :email")
    UserEntity updateUserStatus(String email, String status);

    List<UserEntity> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email);

    List<UserEntity> findUserEntitiesByRoleRoleName(String roleName);

    List<UserEntity> findAllByIdIn(List<UUID> ids);

    @Query(value = """
            SELECT o.status, COUNT(*)
            FROM users o
            group by o.status;
            """, nativeQuery = true)
    List<Object[]> countAllByUserStatus();

    @Query(value = """
            SELECT COUNT(*)
            FROM users o
            """, nativeQuery = true)
    int countAllUsers();

    @Query(value = """
            SELECT r.role_name, COUNT(u.id)
            FROM users u
            JOIN roles r ON u.role_id = r.id
            GROUP BY r.role_name;
            """, nativeQuery = true)
    List<Object[]> countUsersByRole();

    /**
     * Find all users with username starting with the given prefix
     *
     * @param prefix username prefix to search for
     * @return list of matching users
     */
    List<UserEntity> findByUsernameStartingWith(String prefix);
}
