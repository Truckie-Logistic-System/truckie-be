package capstone_project.repository;

import capstone_project.entity.fcm.FCMTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FCMTokenRepository extends JpaRepository<FCMTokenEntity, Long> {

    /**
     * Find active token by token string
     */
    Optional<FCMTokenEntity> findByTokenAndIsActiveTrue(String token);

    /**
     * Find all active tokens for a user
     */
    List<FCMTokenEntity> findByUserIdAndIsActiveTrue(UUID userId);

    /**
     * Find all active tokens for a user by device type
     */
    List<FCMTokenEntity> findByUserIdAndDeviceTypeAndIsActiveTrue(UUID userId, FCMTokenEntity.DeviceType deviceType);

    /**
     * Mark token as inactive by token string
     */
    @Modifying
    @Query("UPDATE FCMTokenEntity t SET t.isActive = false WHERE t.token = :token")
    void markTokenAsInactive(@Param("token") String token);

    /**
     * Mark all tokens for a user as inactive (when user logs out from all devices)
     */
    @Modifying
    @Query("UPDATE FCMTokenEntity t SET t.isActive = false WHERE t.user.id = :userId")
    void markAllUserTokensAsInactive(@Param("userId") UUID userId);

    /**
     * Delete expired tokens
     */
    @Modifying
    @Query("DELETE FROM FCMTokenEntity t WHERE t.expiresAt IS NOT NULL AND t.expiresAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Count active tokens for a user
     */
    @Query("SELECT COUNT(t) FROM FCMTokenEntity t WHERE t.user.id = :userId AND t.isActive = true")
    long countActiveTokensByUserId(@Param("userId") UUID userId);

    /**
     * Find tokens that haven't been used since a specific time
     */
    @Query("SELECT t FROM FCMTokenEntity t WHERE t.lastUsedAt < :cutoff OR t.lastUsedAt IS NULL")
    List<FCMTokenEntity> findUnusedTokensSince(@Param("cutoff") LocalDateTime cutoff);
}
