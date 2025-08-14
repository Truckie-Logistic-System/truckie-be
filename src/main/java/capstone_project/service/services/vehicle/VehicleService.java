package capstone_project.service.services.vehicle;

import capstone_project.dtos.request.vehicle.UpdateVehicleRequest;
import capstone_project.dtos.request.vehicle.VehicleRequest;
import capstone_project.dtos.response.vehicle.VehicleResponse;

import java.util.List;
import java.util.UUID;

public interface VehicleService {
    List<VehicleResponse> getAllVehicles();
    VehicleResponse getVehicleById(UUID id);
    VehicleResponse createVehicle(VehicleRequest req);
    VehicleResponse updateVehicle(UUID id, UpdateVehicleRequest req);
}