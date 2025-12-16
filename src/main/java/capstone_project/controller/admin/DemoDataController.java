package capstone_project.controller.admin;

import capstone_project.dtos.request.demo.GenerateDemoDataRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.demo.DemoDataSummary;
import capstone_project.dtos.response.demo.DemoUsersGenerationResponse;
import capstone_project.dtos.response.demo.UpdateUsernamesResponse;
import capstone_project.service.services.demo.DashboardDemoDataService;
import capstone_project.service.services.auth.RegisterService;
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
    private final RegisterService registerService;

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
     * Generate demo users (customers, drivers, staff) for December 2025 dashboard demo
     * Creates users with realistic Vietnamese data distributed throughout December
     * with special focus on Dec 22-27 for demo purposes
     * 
     * POST /api/admin/demo/dashboard-data/users
     * 
     * @return Summary of generated users with date distribution
     */
    @PostMapping("/users")
    public ResponseEntity<ApiResponse<DemoUsersGenerationResponse>> generateDemoUsers() {
        
        log.info("🎯 Received request to generate demo users for December 2025");
        
        try {
            DemoUsersGenerationResponse response = registerService.generateDemoUsers();
            
            log.info("✅ Demo users generation completed: {} total users created (Customers: {}, Drivers: {}, Staff: {})",
                    response.getTotalUsersCreated(),
                    response.getTotalCustomersCreated(),
                    response.getTotalDriversCreated(),
                    response.getTotalStaffCreated());
            
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (Exception e) {
            log.error("❌ Failed to generate demo users", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Failed to generate demo users: " + e.getMessage(), 500));
        }
    }

    /**
     * Redistribute timestamps of demo data to December 2025 with focused demo week
     * This is critical for realistic dashboard visualization during demos
     * 
     * PUT /api/admin/demo/dashboard-data/redistribute-december-2025
     * 
     * Request body example:
     * {
     *   "targetMonth": 12,
     *   "targetYear": 2025,
     *   "distributionStrategy": "FOCUS_DEMO_WEEK",
     *   "demoWeekStart": "2025-12-22",
     *   "demoWeekEnd": "2025-12-27",
     *   "percentageBeforeDemoWeek": 30,
     *   "percentageDemoWeek": 50,
     *   "percentageAfterDemoWeek": 20
     * }
     * 
     * @param request Redistribution configuration
     * @return Summary of timestamp redistribution with statistics
     */
    @PutMapping("/redistribute-december-2025")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.demo.RedistributeTimestampsResponse>> redistributeTimestamps(
            @Valid @RequestBody capstone_project.dtos.request.demo.RedistributeTimestampsRequest request) {
        
        log.info("📅 Received request to redistribute timestamps to {}/{}", request.getTargetMonth(), request.getTargetYear());
        
        try {
            // Validate percentage distribution
            if (!request.isValidPercentageDistribution()) {
                String errorMsg = String.format("Percentages must sum to 100. Current sum: %d", 
                        request.getPercentageBeforeDemoWeek() + 
                        request.getPercentageDemoWeek() + 
                        request.getPercentageAfterDemoWeek());
                log.warn("❌ Invalid percentage distribution: {}", errorMsg);
                return ResponseEntity.badRequest()
                        .body(ApiResponse.fail(errorMsg, 400));
            }
            
            capstone_project.dtos.response.demo.RedistributeTimestampsResponse response = 
                    demoDataService.redistributeToDecember2025(request);
            
            log.info("✅ Timestamp redistribution completed: {} records updated in {}ms",
                    response.getTotalRecordsUpdated(), 
                    response.getExecutionTimeMs());
            
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (IllegalArgumentException e) {
            log.error("❌ Invalid request for timestamp redistribution", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.fail("Invalid request: " + e.getMessage(), 400));
        } catch (Exception e) {
            log.error("❌ Failed to redistribute timestamps", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Failed to redistribute timestamps: " + e.getMessage(), 500));
        }
    }

    /**
     * Update all existing usernames to correct format with proper role prefixes
     * - Customer: firstname + lastname_abbreviation (e.g., datn for Nguyễn Văn Đạt)
     * - Driver: driver + firstname + lastname_abbreviation (e.g., driverdatn)
     * - Staff: staff + firstname + lastname_abbreviation (e.g., staffdatn)
     * 
     * PUT /api/admin/demo/dashboard-data/update-usernames
     * 
     * @return Summary of updated usernames
     */
    @PutMapping("/update-usernames")
    public ResponseEntity<ApiResponse<UpdateUsernamesResponse>> updateAllUsernames() {
        
        log.info("🔄 Received request to update all usernames to correct format");
        
        try {
            UpdateUsernamesResponse response = registerService.updateAllUsernamesToCorrectFormat();
            
            log.info("✅ Username update completed: {} total users updated in {}ms",
                    response.getTotalUsersUpdated(),
                    response.getExecutionTimeMs());
            
            return ResponseEntity.ok(ApiResponse.ok(response));
            
        } catch (Exception e) {
            log.error("❌ Failed to update usernames", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.fail("Failed to update usernames: " + e.getMessage(), 500));
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
