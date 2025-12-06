package capstone_project.config.scheduled;

import capstone_project.service.services.offroute.OffRouteDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Scheduled job to monitor off-route events and send warnings
 * Runs every minute to check for events that have crossed warning thresholds
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OffRouteMonitoringScheduler {

    private final OffRouteDetectionService offRouteDetectionService;

    /**
     * Check all active off-route events every minute
     * This ensures warnings are sent even if GPS updates are delayed
     */
    @Scheduled(fixedRate = 60000) // Every 60 seconds
    public void checkOffRouteEvents() {
        try {
            log.debug("[OffRouteScheduler] Checking active off-route events...");
            offRouteDetectionService.checkAndSendWarnings();
        } catch (Exception e) {
            log.error("[OffRouteScheduler] Error checking off-route events: {}", e.getMessage(), e);
        }
    }
}
