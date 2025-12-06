package capstone_project.controller.dashboard;

import capstone_project.dtos.request.dashboard.DashboardFilterRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.dashboard.role.*;
import capstone_project.common.utils.UserContextUtils;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.service.services.dashboard.RoleDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("${dashboard.api.base-path}")
@RequiredArgsConstructor
@Slf4j
public class RoleDashboardController {

    private final RoleDashboardService roleDashboardService;
    private final CustomerEntityService customerEntityService;
    private final DriverEntityService driverEntityService;
    private final UserContextUtils userContextUtils;

    /**
     * Get admin dashboard data
     */
    @GetMapping("/admin")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardResponse>> getAdminDashboard(
            @RequestParam(defaultValue = "MONTH") String range,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        DashboardFilterRequest filter = buildFilterRequest(range, fromDate, toDate);
        AdminDashboardResponse response = roleDashboardService.getAdminDashboard(filter);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get admin dashboard AI summary only
     */
    @GetMapping("/admin/ai-summary")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity<ApiResponse<String>> getAdminAiSummary(
            @RequestParam(defaultValue = "MONTH") String range,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        DashboardFilterRequest filter = buildFilterRequest(range, fromDate, toDate);
        String summary = roleDashboardService.getAdminAiSummary(filter);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    /**
     * Get staff dashboard data
     */
    @GetMapping("/staff")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<StaffDashboardResponse>> getStaffDashboard(
            @RequestParam(defaultValue = "TODAY") String range,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        DashboardFilterRequest filter = buildFilterRequest(range, fromDate, toDate);
        StaffDashboardResponse response = roleDashboardService.getStaffDashboard(filter);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get staff dashboard AI summary only
     */
    @GetMapping("/staff/ai-summary")
    @PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<String>> getStaffAiSummary(
            @RequestParam(defaultValue = "TODAY") String range,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        DashboardFilterRequest filter = buildFilterRequest(range, fromDate, toDate);
        String summary = roleDashboardService.getStaffAiSummary(filter);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    /**
     * Get customer dashboard data
     */
    @GetMapping("/customer")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<CustomerDashboardResponse>> getCustomerDashboard(
            @RequestParam(defaultValue = "MONTH") String range,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        DashboardFilterRequest filter = buildFilterRequest(range, fromDate, toDate);
        
        // Get customer ID from authenticated user using UserContextUtils
        UUID customerId = userContextUtils.getCurrentCustomerId();
        
        CustomerDashboardResponse response = roleDashboardService.getCustomerDashboard(customerId, filter);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get driver dashboard data
     */
    @GetMapping("/driver")
    @PreAuthorize("hasAuthority('DRIVER')")
    public ResponseEntity<ApiResponse<DriverDashboardResponse>> getDriverDashboard(
            @RequestParam(defaultValue = "TODAY") String range,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        DashboardFilterRequest filter = buildFilterRequest(range, fromDate, toDate);
        
        // Get driver ID from authenticated user using UserContextUtils
        UUID driverId = userContextUtils.getCurrentDriverId();
        
        DriverDashboardResponse response = roleDashboardService.getDriverDashboard(driverId, filter);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Get customer dashboard AI summary only
     */
    @GetMapping("/customer/ai-summary")
    @PreAuthorize("hasAuthority('CUSTOMER')")
    public ResponseEntity<ApiResponse<String>> getCustomerAiSummary(
            @RequestParam(defaultValue = "MONTH") String range,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        DashboardFilterRequest filter = buildFilterRequest(range, fromDate, toDate);
        
        // Get customer ID from authenticated user using UserContextUtils
        UUID customerId = userContextUtils.getCurrentCustomerId();
        
        String summary = roleDashboardService.getCustomerAiSummary(customerId, filter);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    /**
     * Get driver dashboard AI summary only
     */
    @GetMapping("/driver/ai-summary")
    @PreAuthorize("hasAuthority('DRIVER')")
    public ResponseEntity<ApiResponse<String>> getDriverAiSummary(
            @RequestParam(defaultValue = "TODAY") String range,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate) {
        
        DashboardFilterRequest filter = buildFilterRequest(range, fromDate, toDate);
        
        // Get driver ID from authenticated user using UserContextUtils
        UUID driverId = userContextUtils.getCurrentDriverId();
        
        String summary = roleDashboardService.getDriverAiSummary(driverId, filter);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    /**
     * Build filter request from query parameters
     */
    private DashboardFilterRequest buildFilterRequest(String range, String fromDate, String toDate) {
        DashboardFilterRequest.TimeRange timeRange;
        try {
            timeRange = DashboardFilterRequest.TimeRange.valueOf(range.toUpperCase());
        } catch (IllegalArgumentException e) {
            timeRange = DashboardFilterRequest.TimeRange.MONTH;
        }

        LocalDateTime from = null;
        LocalDateTime to = null;
        
        if (timeRange == DashboardFilterRequest.TimeRange.CUSTOM) {
            try {
                if (fromDate != null) {
                    from = LocalDateTime.parse(fromDate);
                }
                if (toDate != null) {
                    to = LocalDateTime.parse(toDate);
                }
            } catch (Exception e) {
                log.warn("Failed to parse custom dates: fromDate={}, toDate={}", fromDate, toDate);
            }
        }

        return DashboardFilterRequest.builder()
                .range(timeRange)
                .fromDate(from)
                .toDate(to)
                .build();
    }
}
