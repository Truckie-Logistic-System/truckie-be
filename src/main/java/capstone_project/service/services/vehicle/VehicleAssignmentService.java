package capstone_project.service.services.vehicle;


import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
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
}