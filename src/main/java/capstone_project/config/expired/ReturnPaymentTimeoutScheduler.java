package capstone_project.config.expired;

import capstone_project.common.enums.IssueCategoryEnum;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.repository.entityServices.issue.IssueEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * SAFETY NET scheduler to catch any missed timeout checks
 * Primary timeout handling is done by PaymentTimeoutSchedulerService (real-time)
 * This scheduler runs every 5 minutes as a backup to catch edge cases:
 * - Server restart before scheduled task executes
 * - Task scheduling failures
 * - Issues created before this system was deployed
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReturnPaymentTimeoutScheduler {
    
    private final IssueEntityService issueEntityService;
    private final PaymentTimeoutProcessor paymentTimeoutProcessor;

    // Scheduler statistics for monitoring
    private long totalRunCount = 0;
    private long totalExpiredProcessed = 0;
    private java.time.LocalDateTime lastRunTime = null;
    
    /**
     * SAFETY NET: Check for expired return payment deadlines every 5 minutes
     * Most timeouts are handled by PaymentTimeoutSchedulerService (real-time)
     * This catches any missed cases due to server restart or scheduling failures
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes (300,000 ms)
    public void checkReturnPaymentDeadlines() {
        totalRunCount++;
        lastRunTime = java.time.LocalDateTime.now();
        long startTime = System.currentTimeMillis();
        
        
        try {
            // Use optimized query to find ORDER_REJECTION issues in IN_PROGRESS
            // This query eager-fetches issueTypeEntity to avoid LazyInitializationException
            List<IssueEntity> inProgressIssues = issueEntityService.findInProgressOrderRejections();
            
            
            LocalDateTime now = LocalDateTime.now();
            int expiredCount = 0;
            
            for (IssueEntity issue : inProgressIssues) {
                // Check if payment deadline has passed
                if (issue.getPaymentDeadline() != null 
                        && issue.getPaymentDeadline().isBefore(now)) {
                    
                    log.warn("⏰ [SAFETY NET] Issue {} payment deadline expired at {}", 
                            issue.getId(), issue.getPaymentDeadline());
                    
                    // Delegate to @Transactional service to handle all processing
                    // This prevents LazyInitializationException and ensures consistency
                    try {
                        boolean processed = paymentTimeoutProcessor.processTimeout(issue.getId());
                        if (processed) {
                            expiredCount++;
                        } else {
                        }
                    } catch (Exception e) {
                        log.error("❌ [SAFETY NET] Failed to process timeout for issue {}: {}", 
                                issue.getId(), e.getMessage(), e);
                    }
                }
            }
            
            if (expiredCount > 0) {
                totalExpiredProcessed += expiredCount;
            } else {
            }
            
            // Calculate and log execution time
            long executionTime = System.currentTimeMillis() - startTime;
            
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;
            log.error("═══════════════════════════════════════════════════════════════");
            log.error("❌ [SAFETY NET SCHEDULER] Error after {} ms: {}", executionTime, e.getMessage(), e);
            log.error("═══════════════════════════════════════════════════════════════");
        }
    }
    
    // NOTE: Notification methods moved to PaymentTimeoutProcessor service
    // All timeout processing now happens in @Transactional context via PaymentTimeoutProcessor
}
