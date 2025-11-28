package capstone_project.service.scheduled;

import capstone_project.repository.FCMTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class FCMTokenCleanupService {

    private final FCMTokenRepository fcmTokenRepository;

    /**
     * Clean up expired FCM tokens daily at 2 AM
     * Runs every day at 02:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("🧹 Starting scheduled cleanup of expired FCM tokens");
        
        try {
            int deletedCount = fcmTokenRepository.deleteExpiredTokens(LocalDateTime.now());
            log.info("✅ Cleanup completed. Deleted {} expired FCM tokens", deletedCount);
        } catch (Exception e) {
            log.error("❌ Failed to cleanup expired FCM tokens", e);
        }
    }

    /**
     * Clean up unused tokens (older than 90 days) weekly on Sunday at 3 AM
     * Runs every Sunday at 03:00 AM
     */
    @Scheduled(cron = "0 0 3 ? * SUN")
    @Transactional
    public void cleanupUnusedTokens() {
        log.info("🧹 Starting scheduled cleanup of unused FCM tokens (older than 90 days)");
        
        try {
            LocalDateTime cutoff = LocalDateTime.now().minusDays(90);
            var unusedTokens = fcmTokenRepository.findUnusedTokensSince(cutoff);
            
            if (!unusedTokens.isEmpty()) {
                // Mark unused tokens as inactive instead of deleting
                unusedTokens.forEach(token -> token.markAsInactive());
                fcmTokenRepository.saveAll(unusedTokens);
                
                log.info("✅ Cleanup completed. Marked {} unused FCM tokens as inactive", unusedTokens.size());
            } else {
                log.info("✅ No unused tokens found for cleanup");
            }
        } catch (Exception e) {
            log.error("❌ Failed to cleanup unused FCM tokens", e);
        }
    }
}
