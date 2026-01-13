package capstone_project.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple Health Check Controller for Azure App Service.
 * This controller provides endpoints that Azure can use to verify the application is running.
 * These endpoints respond immediately without requiring full application context initialization.
 */
@RestController
public class HealthCheckController {

    private static final Instant startTime = Instant.now();

    /**
     * Root health check endpoint.
     * Azure App Service will ping this endpoint to verify the container is responsive.
     */
    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return healthResponse();
    }

    /**
     * Dedicated health endpoint for Azure.
     * Configure WEBSITE_WARMUP_PATH=/health in Azure App Settings.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return healthResponse();
    }

    /**
     * Ping endpoint for quick response checks.
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("pong");
    }

    private ResponseEntity<Map<String, Object>> healthResponse() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "Truckie Backend");
        response.put("timestamp", Instant.now().toString());
        response.put("uptime", java.time.Duration.between(startTime, Instant.now()).toString());
        return ResponseEntity.ok(response);
    }
}
