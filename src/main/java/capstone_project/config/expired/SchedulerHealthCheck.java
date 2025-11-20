package capstone_project.config.expired;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Health check scheduler to monitor payment timeout schedulers
 * Logs status every 30 minutes for monitoring
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchedulerHealthCheck {
    
    private final PaymentTimeoutSchedulerService dynamicScheduler;
    
    /**
     * Log scheduler health status every 30 minutes
     * This helps monitor that schedulers are working correctly
     */
    @Scheduled(cron = "0 */30 * * * *") // Every 30 minutes
    public void logSchedulerHealth() {

        // Log dynamic scheduler status
        dynamicScheduler.logStatus();

    }
    
    /**
     * Log scheduler status on application startup
     */
    @org.springframework.context.event.EventListener(org.springframework.boot.context.event.ApplicationReadyEvent.class)
    public void onStartup() {

    }
}
