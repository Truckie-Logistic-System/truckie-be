package capstone_project.repository.repositories.user;

import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.repositories.common.BaseRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DriverRepository extends BaseRepository<DriverEntity> {
    List<DriverEntity> findByUser_Role_RoleName(String userRoleRoleName);

    Optional<DriverEntity> findByUserId(UUID userId);

    List<DriverEntity> findByStatus(String status);

    /**
     * Find driver by user's phone number
     * @param phoneNumber Phone number to search
     * @return Optional containing driver if found
     */
    Optional<DriverEntity> findFirstByUserPhoneNumber(String phoneNumber);

    /**
     * Find all drivers by user's phone number (for duplicate detection)
     * @param phoneNumber Phone number to search
     * @return List of drivers matching the phone number
     */
    List<DriverEntity> findByUserPhoneNumber(String phoneNumber);

    /**
     * Find driver by user's email
     * @param email Email to search
     * @return Optional containing driver if found
     */
    Optional<DriverEntity> findFirstByUserEmail(String email);

    /**
     * Find all drivers by user's email (for duplicate detection)
     * @param email Email to search
     * @return List of drivers matching the email
     */
    List<DriverEntity> findByUserEmail(String email);
}
