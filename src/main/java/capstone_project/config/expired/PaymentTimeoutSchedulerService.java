package capstone_project.config.expired;

import capstone_project.entity.issue.IssueEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Service to schedule payment timeout checks for individual issues
 * Provides real-time timeout detection with minimal delay
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentTimeoutSchedulerService {
    
    private final TaskScheduler taskScheduler;
    private final PaymentTimeoutProcessor paymentTimeoutProcessor;
    
    // Track scheduled tasks to allow cancellation if payment received
    private final Map<java.util.UUID, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();
    
    // Statistics for monitoring
    private long totalScheduledTasks = 0;
    private long totalCancelledTasks = 0;
    private long totalExecutedTasks = 0;
    
    /**
     * Schedule timeout check for specific issue
     * Will execute exactly at deadline (+ small buffer for safety)
     * 
     * @param issue Issue entity with payment deadline
     */
    public void scheduleTimeoutCheck(IssueEntity issue) {
        totalScheduledTasks++;
        if (issue.getPaymentDeadline() == null) {
            log.warn("Cannot schedule timeout check for issue {} - no deadline set", issue.getId());
            return;
        }
        
        // Add 30 second buffer to ensure deadline has definitely passed
        Instant scheduledTime = issue.getPaymentDeadline()
                .plusSeconds(30)
                .atZone(ZoneId.systemDefault())
                .toInstant();
        
        long secondsUntilExecution = java.time.Duration.between(Instant.now(), scheduledTime).getSeconds();
        
        
        // Create and schedule task with wrapper to track execution
        PaymentTimeoutTask task = new PaymentTimeoutTask(
            issue.getId(),
            paymentTimeoutProcessor
        );
        
        // Wrap task to count executions
        Runnable wrappedTask = () -> {
            totalExecutedTasks++;
            task.run();
            // Remove from tracking map after execution
            scheduledTasks.remove(issue.getId());
        };
        
        ScheduledFuture<?> scheduledFuture = taskScheduler.schedule(wrappedTask, scheduledTime);
        
        // Track task for potential cancellation
        scheduledTasks.put(issue.getId(), scheduledFuture);
        
    }
    
    /**
     * Cancel scheduled timeout check when payment is received
     * 
     * @param issueId Issue ID
     */
    public void cancelTimeoutCheck(java.util.UUID issueId) {
        totalCancelledTasks++;
        ScheduledFuture<?> scheduledFuture = scheduledTasks.remove(issueId);
        
        if (scheduledFuture != null && !scheduledFuture.isDone()) {
            boolean cancelled = scheduledFuture.cancel(false);
            if (cancelled) {
            } else {
                log.warn("⚠️ Failed to cancel timeout check for issue {} (may already be running)", issueId);
            }
        } else {
        }
    }
    
    /**
     * Get number of currently scheduled timeout checks
     */
    public int getScheduledTaskCount() {
        // Clean up completed tasks
        scheduledTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
        return scheduledTasks.size();
    }
    
    /**
     * Get scheduler statistics for monitoring
     */
    public String getStatistics() {
        // Clean up completed tasks first
        scheduledTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
        
        return String.format(
            "Dynamic Scheduler Stats - Total scheduled: %d | Executed: %d | Cancelled: %d | Active: %d",
            totalScheduledTasks, totalExecutedTasks, totalCancelledTasks, scheduledTasks.size()
        );
    }
    
    /**
     * Log current scheduler status (for health checks)
     */
    public void logStatus() {
        scheduledTasks.entrySet().removeIf(entry -> entry.getValue().isDone());
        
    }
}
