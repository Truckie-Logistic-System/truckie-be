package capstone_project.service.services.pricing;

import capstone_project.dtos.request.pricing.VehicleTypeRuleRequest;
import capstone_project.dtos.request.pricing.UpdateVehicleTypeRuleRequest;
import capstone_project.dtos.response.pricing.FullVehicleTypeRuleResponse;
import capstone_project.dtos.response.pricing.VehicleTypeRuleResponse;

import java.util.List;
import java.util.UUID;

public interface VehicleTypeRuleService {
    List<VehicleTypeRuleResponse> getAllVehicleTypeRules();

    List<FullVehicleTypeRuleResponse> getAllFullVehicleTypeRules();

    VehicleTypeRuleResponse getVehicleTypeRuleById(UUID id);

    FullVehicleTypeRuleResponse getFullVehicleTypeRuleById(UUID id);

    VehicleTypeRuleResponse createVehicleTypeRule(VehicleTypeRuleRequest vehicleTypeRuleRequest);

    VehicleTypeRuleResponse updateVehicleTypeRule(UUID id, UpdateVehicleTypeRuleRequest updateVehicleTypeRuleRequest);

    void deleteVehicleTypeRule(UUID id);
}
