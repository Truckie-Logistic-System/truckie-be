package capstone_project.service.services.pricing;

import capstone_project.dtos.request.pricing.VehicleRuleRequest;
import capstone_project.dtos.request.pricing.UpdateVehicleRuleRequest;
import capstone_project.dtos.response.pricing.FullVehicleRuleResponse;
import capstone_project.dtos.response.pricing.VehicleRuleResponse;

import java.util.List;
import java.util.UUID;

public interface VehicleRuleService {
    List<VehicleRuleResponse> getAllVehicleRules();

    VehicleRuleResponse getVehicleRuleById(UUID id);

    FullVehicleRuleResponse getFullVehicleRuleById(UUID id);

    VehicleRuleResponse createVehicleRule(VehicleRuleRequest vehicleRuleRequest);

    VehicleRuleResponse updateVehicleRule(UUID id, UpdateVehicleRuleRequest updateVehicleRuleRequest);

    void deleteVehicleRule(UUID id);
}
