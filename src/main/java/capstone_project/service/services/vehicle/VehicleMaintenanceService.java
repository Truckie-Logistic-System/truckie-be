package capstone_project.service.services.vehicle;

import capstone_project.dtos.request.vehicle.UpdateVehicleMaintenanceRequest;
import capstone_project.dtos.request.vehicle.VehicleMaintenanceRequest;
import capstone_project.dtos.response.vehicle.VehicleMaintenanceResponse;

import java.util.List;
import java.util.UUID;


public interface VehicleMaintenanceService {
    List<VehicleMaintenanceResponse> getAllMaintenance();
    VehicleMaintenanceResponse getMaintenanceById(UUID id);
    VehicleMaintenanceResponse createMaintenance(VehicleMaintenanceRequest req);
    VehicleMaintenanceResponse updateMaintenance(UUID id, UpdateVehicleMaintenanceRequest req);
}
