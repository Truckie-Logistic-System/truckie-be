package capstone_project.service.services.vehicle;

import capstone_project.dtos.request.vehicle.VehicleTypeRequest;
import capstone_project.dtos.response.vehicle.VehicleTypeResponse;

import java.util.List;
import java.util.UUID;

public interface VehicleTypeService {

    List<VehicleTypeResponse> getAllVehicleTypes();

    VehicleTypeResponse getVehicleTypeById(UUID id);

    VehicleTypeResponse createVehicleType(VehicleTypeRequest vehicleTypeRequest);

    VehicleTypeResponse updateVehicleType(UUID id, VehicleTypeRequest vehicleTypeRequest);

    void deleteVehicleType(UUID id);
}
