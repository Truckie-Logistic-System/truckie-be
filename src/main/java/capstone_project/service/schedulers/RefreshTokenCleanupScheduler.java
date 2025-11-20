package capstone_project.service.schedulers;

import capstone_project.repository.entityServices.auth.RefreshTokenEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled task to clean up expired and old revoked refresh tokens
 * This prevents database bloat and improves query performance
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenEntityService refreshTokenEntityService;

    /**
     * Clean up expired tokens every day at 2 AM
     * Cron: second minute hour day month weekday
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        try {
            LocalDateTime now = LocalDateTime.now();
            log.info("[RefreshTokenCleanup] Starting cleanup of expired tokens...");
            
            refreshTokenEntityService.deleteExpiredTokens(now);
            
            log.info("[RefreshTokenCleanup] ✅ Expired tokens cleanup completed");
        } catch (Exception e) {
            log.error("[RefreshTokenCleanup] ❌ Error during expired tokens cleanup", e);
        }
    }

    /**
     * Clean up old revoked tokens every week on Sunday at 3 AM
     * Keep revoked tokens for 7 days for audit purposes, then delete
     * Cron: second minute hour day month weekday (0 = Sunday)
     */
    @Scheduled(cron = "0 0 3 * * 0")
    @Transactional
    public void cleanupOldRevokedTokens() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(7);
            log.info("[RefreshTokenCleanup] Starting cleanup of old revoked tokens (older than 7 days)...");
            
            refreshTokenEntityService.deleteOldRevokedTokens(cutoffDate);
            
            log.info("[RefreshTokenCleanup] ✅ Old revoked tokens cleanup completed");
        } catch (Exception e) {
            log.error("[RefreshTokenCleanup] ❌ Error during old revoked tokens cleanup", e);
        }
    }
}
