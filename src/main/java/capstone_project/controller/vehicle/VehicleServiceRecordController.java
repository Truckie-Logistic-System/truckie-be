package capstone_project.controller.vehicle;

import capstone_project.common.enums.VehicleServiceStatusEnum;
import capstone_project.dtos.request.vehicle.UpdateVehicleServiceRecordRequest;
import capstone_project.dtos.request.vehicle.VehicleServiceRecordRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.vehicle.PaginatedServiceRecordsResponse;
import capstone_project.dtos.response.vehicle.VehicleServiceRecordResponse;
import capstone_project.service.services.vehicle.VehicleServiceRecordService;
import capstone_project.service.services.vehicle.VehicleExpiryCheckService;
import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${vehicle-service-record.api.base-path}")
@RequiredArgsConstructor
@Validated
public class VehicleServiceRecordController {

    private final VehicleServiceRecordService service;
    private final VehicleExpiryCheckService expiryCheckService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleServiceRecordResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllRecords()));
    }

    @GetMapping("/paginated")
    public ResponseEntity<ApiResponse<PaginatedServiceRecordsResponse>> getAllPaginated(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllRecordsPaginated(page, size)));
    }

    @GetMapping("/by-type")
    public ResponseEntity<ApiResponse<PaginatedServiceRecordsResponse>> getByType(
            @RequestParam String serviceType,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.getRecordsByType(serviceType, page, size)));
    }

    @GetMapping("/service-types")
    public ResponseEntity<ApiResponse<List<String>>> getServiceTypes() {
        return ResponseEntity.ok(ApiResponse.ok(service.getServiceTypes()));
    }

    @GetMapping("/by-status/{serviceStatus}")
    public ResponseEntity<ApiResponse<PaginatedServiceRecordsResponse>> getByStatus(
            @PathVariable VehicleServiceStatusEnum serviceStatus,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(ApiResponse.ok(service.getRecordsByStatus(serviceStatus, page, size)));
    }

    @GetMapping("/by-vehicle/{vehicleId}")
    public ResponseEntity<ApiResponse<List<VehicleServiceRecordResponse>>> getByVehicle(
            @PathVariable UUID vehicleId) {
        return ResponseEntity.ok(ApiResponse.ok(service.getRecordsByVehicleId(vehicleId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleServiceRecordResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getRecordById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleServiceRecordResponse>> create(
            @RequestBody @Valid VehicleServiceRecordRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.createRecord(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleServiceRecordResponse>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateVehicleServiceRecordRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateRecord(id, req)));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<VehicleServiceRecordResponse>> complete(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.completeRecord(id)));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<VehicleServiceRecordResponse>> cancel(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.cancelRecord(id)));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<VehicleServiceRecordResponse>> start(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.startRecord(id)));
    }

    @PostMapping("/check-expiry")
    public ResponseEntity<ApiResponse<String>> triggerExpiryCheck() {
        expiryCheckService.runManualCheck();
        return ResponseEntity.ok(ApiResponse.ok("Đã chạy kiểm tra hạn đăng kiểm/bảo hiểm xe"));
    }

    @PostMapping("/generate-for-all-vehicles")
    public ResponseEntity<ApiResponse<String>> generateForAllVehicles() {
        int created = service.generateServiceRecordsForAllVehicles();
        String message = "Đã tạo " + created + " lịch đăng kiểm/bảo trì cho tất cả xe";
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    @GetMapping("/due-soon")
    public ResponseEntity<ApiResponse<List<VehicleServiceRecordResponse>>> getServicesDueSoon(
            @RequestParam(defaultValue = "30") int warningDays) {
        return ResponseEntity.ok(ApiResponse.ok(service.getServicesDueSoon(warningDays)));
    }

    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<VehicleServiceRecordResponse>>> getOverdueServices() {
        return ResponseEntity.ok(ApiResponse.ok(service.getOverdueServices()));
    }

    /**
     * Endpoint phục vụ mục đích test FE: tạo dữ liệu demo cho banner cảnh báo
     * bảo trì/đăng kiểm (overdue, ≤7 ngày, 8-30 ngày) cho một vài xe ngẫu nhiên.
     */
    @PostMapping("/generate-demo-alert-data")
    @Hidden
    public ResponseEntity<ApiResponse<String>> generateDemoAlertDataForBanner() {
        int created = service.generateDemoAlertDataForBanner();
        String message = "Đã tạo " + created + " bản ghi demo cho banner cảnh báo bảo trì/đăng kiểm";
        return ResponseEntity.ok(ApiResponse.ok(message));
    }
}
