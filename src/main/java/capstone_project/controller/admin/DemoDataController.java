package capstone_project.controller.admin;

import capstone_project.dtos.request.demo.GenerateDemoDataRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.demo.DemoDataSummary;
import capstone_project.service.services.demo.DashboardDemoDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing demo/test data generation and cleanup
 * ADMIN ONLY - For testing dashboard visualizations
 * 
 * IMPORTANT: Should only be used in DEV/STAGING environments
 */
@RestController
@RequestMapping("${demo-data.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
@Slf4j
public class DemoDataController {

    private final DashboardDemoDataService demoDataService;

    /**
     * Generate demo data for dashboard testing
     * Creates users, orders, trips, issues, etc. distributed across the specified year
     * with realistic seasonal patterns (Q3 high season)
     * 
     * POST /api/admin/demo/dashboard-data/generate
     * 
     * Request body:
     * {
     *   "year": 2025,
     *   "minPerMonth": 5,
     *   "maxPerMonth": 20,
     *   "include": {
     *     "admin": true,
     *     "staff": true,
     *     "customer": true,
     *     "driver": true
     *   }
     * }
     * 
     * @param request Generation configuration
     * @return Summary of generated data
     */
    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<DemoDataSummary>> generateDemoData(
            @Valid @RequestBody GenerateDemoDataRequest request) {
        
        log.info("🎯 Received request to generate demo data for year {}", request.getYear());
        
        try {
            DemoDataSummary summary = demoDataService.generateDemoData(request);
            
            log.info("✅ Demo data generation completed: {} orders, {} users, {} issues created",
                    summary.getOrdersCreated(), 
                    summary.getUsersCreated(), 
                    summary.getIssuesCreated());
            
            return ResponseEntity.ok(ApiResponse.ok(summary));
            
        } catch (Exception e) {
            log.error("❌ Failed to generate demo data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Failed to generate demo data: " + e.getMessage(), 500));
        }
    }

    /**
     * Clear all demo data marked with isDemoData = true
     * Deletes all entities created by the generate endpoint
     * Use this before generating new demo data to avoid duplicates
     * 
     * DELETE /api/admin/demo/dashboard-data
     * 
     * @return Summary of cleared data
     */
    @DeleteMapping
    public ResponseEntity<ApiResponse<DemoDataSummary>> clearDemoData() {
        
        log.info("🧹 Received request to clear all demo data");
        
        try {
            DemoDataSummary summary = demoDataService.clearDemoData();
            
            log.info("✅ Demo data cleared: {} records deleted in {}ms",
                    summary.getTotalRecordsDeleted(),
                    summary.getExecutionTimeMs());
            
            return ResponseEntity.ok(ApiResponse.ok(summary));
            
        } catch (Exception e) {
            log.error("❌ Failed to clear demo data", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Failed to clear demo data: " + e.getMessage(), 500));
        }
    }

    /**
     * Health check endpoint to verify demo data service availability
     * 
     * GET /api/admin/demo/dashboard-data/health
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<String>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.ok("Demo data service is available"));
    }
}
