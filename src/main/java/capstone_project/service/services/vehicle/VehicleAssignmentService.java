package capstone_project.service.services.vehicle;


import capstone_project.dtos.request.vehicle.GroupedAssignmentRequest;
import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.vehicle.GroupedVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.SampleVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.SimplifiedVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;

import java.util.List;
import java.util.UUID;

public interface VehicleAssignmentService {
    List<VehicleAssignmentResponse> getAllAssignments();
    VehicleAssignmentResponse getAssignmentById(UUID id);
    VehicleAssignmentResponse createAssignment(VehicleAssignmentRequest req);
    VehicleAssignmentResponse updateAssignment(UUID id, UpdateVehicleAssignmentRequest req);
    List<VehicleAssignmentResponse> getAllAssignmentsWithOrder(UUID vehicleType);
    List<VehicleAssignmentResponse> getListVehicleAssignmentByOrderID(UUID orderID);
    List<SampleVehicleAssignmentResponse> getVehicleAndDriversForDetails(UUID orderID);

    /**
     * Lấy danh sách xe và tài xế gợi ý cho order với định dạng đơn giản
     * @param orderID ID của order
     * @return Danh sách xe và tài xế đã được đơn giản hóa
     */
    SimplifiedVehicleAssignmentResponse getSimplifiedSuggestionsForOrder(UUID orderID);

    /**
     * Lấy danh sách gợi ý xe và tài xế cho order với các order detail được nhóm lại
     * @param orderID ID của order
     * @return Danh sách gợi ý với các order detail được nhóm lại thành các chuyến
     */
    GroupedVehicleAssignmentResponse getGroupedSuggestionsForOrder(UUID orderID);

    /**
     * Tạo và gán vehicle assignment cho nhiều order detail cùng lúc
     * @param request Request chứa thông tin về nhóm order detail và xe, tài xế được gán
     * @return Danh sách vehicle assignment đã được tạo
     */
    List<VehicleAssignmentResponse> createGroupedAssignments(GroupedAssignmentRequest request);
}