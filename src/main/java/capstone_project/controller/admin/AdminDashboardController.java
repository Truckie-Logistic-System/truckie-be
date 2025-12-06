package capstone_project.controller.admin;

import capstone_project.dtos.response.admin.AdminDashboardSummaryResponse;
import capstone_project.dtos.response.admin.RegistrationTimeSeriesResponse;
import capstone_project.dtos.response.admin.TopDriverResponse;
import capstone_project.dtos.response.admin.TopStaffResponse;
import capstone_project.service.services.admin.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("${admin-dashboard.api.base-path}")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping("/summary")
    public ResponseEntity<AdminDashboardSummaryResponse> getDashboardSummary(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(adminDashboardService.getDashboardSummary(period));
    }

    @GetMapping("/registrations")
    public ResponseEntity<RegistrationTimeSeriesResponse> getRegistrationTimeSeries(
            @RequestParam String role,
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(adminDashboardService.getRegistrationTimeSeries(role, period));
    }

    @GetMapping("/top-staff")
    public ResponseEntity<List<TopStaffResponse>> getTopStaff(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(adminDashboardService.getTopStaff(limit, period));
    }

    @GetMapping("/top-drivers")
    public ResponseEntity<List<TopDriverResponse>> getTopDrivers(
            @RequestParam(defaultValue = "5") Integer limit,
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(adminDashboardService.getTopDrivers(limit, period));
    }

    @GetMapping("/ai-summary")
    public ResponseEntity<String> getAdminAiSummary(
            @RequestParam(defaultValue = "month") String period) {
        return ResponseEntity.ok(adminDashboardService.getAdminAiSummary(period));
    }
}
