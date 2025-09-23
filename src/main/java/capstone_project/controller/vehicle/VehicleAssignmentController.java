package capstone_project.controller.vehicle;

import capstone_project.dtos.request.vehicle.GroupedAssignmentRequest;
import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.vehicle.GroupedVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.SampleVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.SimplifiedVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${vehicle-assignment.api.base-path}")
@RequiredArgsConstructor
@Validated
public class VehicleAssignmentController {

    private final VehicleAssignmentService service;

    @GetMapping
    public ResponseEntity<ApiResponse<List<VehicleAssignmentResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(service.getAllAssignments()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleAssignmentResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(service.getAssignmentById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VehicleAssignmentResponse>> create(
            @RequestBody @Valid VehicleAssignmentRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(service.createAssignment(req)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<VehicleAssignmentResponse>> update(
            @PathVariable UUID id,
            @RequestBody @Valid UpdateVehicleAssignmentRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(service.updateAssignment(id, req)));
    }

    @GetMapping("/{vehicleTypeId}/get-all-with-order")
    public ResponseEntity<ApiResponse<List<VehicleAssignmentResponse>>> getByIdWithOrder(
            @PathVariable UUID vehicleTypeId) {

        List<VehicleAssignmentResponse> response = service.getAllAssignmentsWithOrder(vehicleTypeId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/{orderId}/get-all-by-order-id")
    public ResponseEntity<ApiResponse<List<VehicleAssignmentResponse>>> getByOrderId(
            @PathVariable UUID orderId) {

        List<VehicleAssignmentResponse> response = service.getListVehicleAssignmentByOrderID(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Endpoint trả về danh sách đề xuất xe và tài xế cho order với định dạng đơn giản hóa
     *
     * @param orderId ID của đơn hàng
     * @return Danh sách đề xuất xe và tài xế đã được đơn giản hóa
     */
    @GetMapping("/{orderId}/suggest-drivers-and-vehicle-for-details")
    public ResponseEntity<ApiResponse<SimplifiedVehicleAssignmentResponse>> getSimplifiedSuggestions(
            @PathVariable UUID orderId) {
        SimplifiedVehicleAssignmentResponse response = service.getSimplifiedSuggestionsForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Endpoint trả về danh sách đề xuất xe và tài xế cho order,
     * với các order detail được nhóm lại thành các chuyến hợp lý
     *
     * @param orderId ID của đơn hàng
     * @return Danh sách đề xuất xe và tài xế đã được nhóm thành các chuyến
     */
    @GetMapping("/{orderId}/grouped-suggestions")
    public ResponseEntity<ApiResponse<GroupedVehicleAssignmentResponse>> getGroupedSuggestions(
            @PathVariable UUID orderId) {
        GroupedVehicleAssignmentResponse response = service.getGroupedSuggestionsForOrder(orderId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    /**
     * Endpoint để tạo và gán vehicle assignment cho nhiều order detail cùng lúc
     *
     * @param request Request chứa thông tin về các nhóm order detail và xe, tài xế được gán
     * @return Danh sách vehicle assignment đã được tạo
     */
    @PostMapping("/create-grouped-assignments")
    public ResponseEntity<ApiResponse<List<VehicleAssignmentResponse>>> createGroupedAssignments(
            @RequestBody @Valid GroupedAssignmentRequest request) {
        List<VehicleAssignmentResponse> responses = service.createGroupedAssignments(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(responses));
    }
}