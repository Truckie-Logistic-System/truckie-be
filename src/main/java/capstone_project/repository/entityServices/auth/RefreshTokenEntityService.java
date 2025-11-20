package capstone_project.repository.entityServices.auth;


import capstone_project.entity.auth.RefreshTokenEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenEntityService extends BaseEntityService<RefreshTokenEntity, UUID> {

    List<RefreshTokenEntity> findByUserIdAndRevokedFalse(UUID userId);

    /**
     * Find active refresh token (filters out revoked tokens)
     */
    Optional<RefreshTokenEntity> findByTokenAndRevokedFalse(String token);

    /**
     * Legacy method - deprecated
     * @deprecated Use findByTokenAndRevokedFalse instead
     */
    @Deprecated
    Optional<RefreshTokenEntity> findByToken(String token);

    void saveAll(List<RefreshTokenEntity> tokens);

    /**
     * Delete expired tokens for cleanup
     */
    void deleteExpiredTokens(LocalDateTime now);

    /**
     * Delete old revoked tokens for cleanup
     */
    void deleteOldRevokedTokens(LocalDateTime cutoffDate);
}
