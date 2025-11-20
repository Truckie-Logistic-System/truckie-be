package capstone_project.config.scheduler;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Configuration for dynamic task scheduling
 * Used for real-time payment timeout detection
 */
@Configuration
public class SchedulerConfig {
    
    /**
     * Create ThreadPoolTaskScheduler for dynamic task scheduling
     * Pool size 10 allows concurrent timeout checks for multiple issues
     */
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // Allow up to 10 concurrent scheduled tasks
        scheduler.setThreadNamePrefix("payment-timeout-");
        scheduler.setDaemon(true); // Daemon threads won't prevent JVM shutdown
        scheduler.initialize();
        return scheduler;
    }
}
