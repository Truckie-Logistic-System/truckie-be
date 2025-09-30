package capstone_project.controller.admin.dashboard;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.dashboard.MonthlyOrderCount;
import capstone_project.service.services.dashboard.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${dashboard.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/count-all-orders")
    public ResponseEntity<ApiResponse<Integer>> countAllOrder() {
        final var result = dashboardService.countAllOrder();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/count-order-by/{senderId}")
    public ResponseEntity<ApiResponse<Integer>> countOrderEntitiesBySenderId(@PathVariable UUID senderId) {
        final var result = dashboardService.countOrderEntitiesBySenderId(senderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/count-order-by-sender")
    public ResponseEntity<ApiResponse<Integer>> countOrderEntitiesBySenderCompanyNameContainingIgnoreCase(@RequestParam String senderCompanyName) {
        final var result = dashboardService.countOrderEntitiesBySenderCompanyNameContainingIgnoreCase(senderCompanyName);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/count-order-by-receiver")
    public ResponseEntity<ApiResponse<Integer>> countOrderEntitiesByReceiverNameContainingIgnoreCase(@RequestParam String receiverName) {
        final var result = dashboardService.countOrderEntitiesByReceiverNameContainingIgnoreCase(receiverName);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/count-order-by-month-over-year")
    public ResponseEntity<ApiResponse<List<MonthlyOrderCount>>> countTotalOrderByMonthOverYear(@RequestParam int year) {
        final var result = dashboardService.countTotalOrderByMonthOverYear(year);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/count-order-all-by-status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countAllByOrderStatus() {
        final var result = dashboardService.countAllByOrderStatus();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/count-by-week")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countOrderByWeek(@RequestParam int amount) {
        final var result = dashboardService.countOrderByWeek(amount);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/count-by-year")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countAllByUserStatus(@RequestParam int amount) {
        final var result = dashboardService.countOrderByYear(amount);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/count-all-users-over-status")
    public ResponseEntity<ApiResponse<Map<String, Long>>> countAllByUserStatus() {
        final var result = dashboardService.countAllByUserStatus();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/count-all-users")
    public ResponseEntity<ApiResponse<Integer>> countAllUsers() {
        final var result = dashboardService.countAllUsers();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
