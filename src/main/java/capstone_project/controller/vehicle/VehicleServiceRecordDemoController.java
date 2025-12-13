package capstone_project.controller.vehicle;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.service.services.vehicle.VehicleServiceRecordDemoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/vehicle-service-records/demo")
@RequiredArgsConstructor
@Tag(name = "Vehicle Service Record Demo", description = "API tạo dữ liệu demo cho lịch sử bảo trì/đăng kiểm xe")
public class VehicleServiceRecordDemoController {

    private final VehicleServiceRecordDemoService demoService;

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tạo dữ liệu demo cho tất cả xe", description = "Tạo lịch sử bảo trì/đăng kiểm đa dạng cho tất cả xe trong hệ thống")
    public ResponseEntity<ApiResponse<Map<String, Object>>> generateDemoData() {
        Map<String, Object> result = demoService.generateDemoDataForAllVehicles();
        return ResponseEntity.ok(ApiResponse.ok(result, "Tạo dữ liệu demo thành công"));
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Xóa dữ liệu demo", description = "Xóa tất cả dữ liệu demo (is_demo_data = true)")
    public ResponseEntity<ApiResponse<String>> clearDemoData() {
        demoService.clearAllDemoData();
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã xóa toàn bộ dữ liệu demo"));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Thống kê dữ liệu demo", description = "Hiển thị thống kê dữ liệu demo hiện tại")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDemoStats() {
        Map<String, Object> stats = demoService.getDemoStatistics();
        return ResponseEntity.ok(ApiResponse.ok(stats, "Thống kê dữ liệu demo"));
    }

    @PostMapping("/update-overdue")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cập nhật trạng thái quá hạn", description = "Manual trigger để cập nhật các bản ghi PLANNED quá hạn thành OVERDUE")
    public ResponseEntity<ApiResponse<String>> updateOverdueStatus() {
        demoService.updateOverdueServiceRecords();
        return ResponseEntity.ok(ApiResponse.ok(null, "Đã cập nhật trạng thái quá hạn"));
    }
}
