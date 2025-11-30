package capstone_project.config.expired;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Individual task to check specific issue payment timeout
 * Scheduled dynamically when issue is created
 */
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutTask implements Runnable {
    
    private final java.util.UUID issueId;
    private final PaymentTimeoutProcessor paymentTimeoutProcessor;
    
    @Override
    public void run() {
        
        try {
            // Delegate to @Transactional service to prevent LazyInitializationException
            // All database operations will happen within a single transaction
            boolean processed = paymentTimeoutProcessor.processTimeout(issueId);
            
            if (processed) {
            } else {
            }
            
        } catch (Exception e) {
            log.error("❌ [PaymentTimeoutTask] Error processing timeout for issue {}: {}", issueId, e.getMessage(), e);
        }
    }
    
    // NOTE: All timeout processing logic moved to PaymentTimeoutProcessor service
    // This ensures operations run within @Transactional context
}
