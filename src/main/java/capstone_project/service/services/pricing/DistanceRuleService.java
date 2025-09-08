package capstone_project.service.services.pricing;

import capstone_project.dtos.request.pricing.DistanceRuleRequest;
import capstone_project.dtos.request.pricing.UpdateDistanceRuleRequest;
import capstone_project.dtos.response.pricing.DistanceRuleResponse;

import java.util.List;
import java.util.UUID;

public interface DistanceRuleService {
    List<DistanceRuleResponse> getAllDistanceRules();

    DistanceRuleResponse getDistanceRuleById(UUID id);

    DistanceRuleResponse createDistanceRule(DistanceRuleRequest distanceRuleRequest);

    DistanceRuleResponse updateDistanceRule(UUID id, UpdateDistanceRuleRequest distanceRuleRequest);

    void deleteDistanceRule(UUID id);
}
