package capstone_project.service.services.vehicle;

import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionCreateRequest;
import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionEndReadingRequest;
import capstone_project.dtos.request.vehicle.VehicleFuelConsumptionInvoiceRequest;
import capstone_project.dtos.response.vehicle.VehicleFuelConsumptionResponse;

import java.util.UUID;

public interface VehicleFuelConsumptionService {

    VehicleFuelConsumptionResponse createVehicleFuelConsumption(VehicleFuelConsumptionCreateRequest request);

    VehicleFuelConsumptionResponse updateInvoiceImage(VehicleFuelConsumptionInvoiceRequest request);

    VehicleFuelConsumptionResponse updateFinalReading(VehicleFuelConsumptionEndReadingRequest request);

    VehicleFuelConsumptionResponse getVehicleFuelConsumptionById(UUID id);

    VehicleFuelConsumptionResponse getVehicleFuelConsumptionByVehicleAssignmentId(UUID vehicleAssignmentId);
}
