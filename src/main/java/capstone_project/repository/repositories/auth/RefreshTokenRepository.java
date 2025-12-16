package capstone_project.repository.repositories.auth;

import capstone_project.entity.auth.RefreshTokenEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends BaseRepository<RefreshTokenEntity> {

    List<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId);

    /**
     * Find active (non-revoked) refresh token by token string
     * CRITICAL: Filter by revoked=false to avoid duplicate token issues
     */
    @Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.token = :token AND rt.revoked = false ORDER BY rt.createdAt DESC")
    Optional<RefreshTokenEntity> findByTokenAndRevokedFalse(@Param("token") String token);

    /**
     * Legacy method - deprecated, use findByTokenAndRevokedFalse instead
     * @deprecated This may return duplicates if old revoked tokens exist
     */
    @Deprecated
    Optional<RefreshTokenEntity> findByToken(String token);

    /**
     * Delete all expired refresh tokens for cleanup
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiredAt < :now")
    int deleteExpiredTokens(@Param("now") LocalDateTime now);

    /**
     * Delete all revoked tokens older than specified date for cleanup
     */
    @Modifying
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.revoked = true AND rt.createdAt < :cutoffDate")
    int deleteOldRevokedTokens(@Param("cutoffDate") LocalDateTime cutoffDate);
}
