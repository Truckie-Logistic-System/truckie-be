package capstone_project.service.services.pricing;

import capstone_project.dtos.request.pricing.SizeRuleRequest;
import capstone_project.dtos.request.pricing.UpdateSizeRuleRequest;
import capstone_project.dtos.response.pricing.FullSizeRuleResponse;
import capstone_project.dtos.response.pricing.SizeRuleResponse;

import java.util.List;
import java.util.UUID;

public interface SizeRuleService {
    List<SizeRuleResponse> getAllsizeRules();

    List<FullSizeRuleResponse> getAllFullsizeRules();

    SizeRuleResponse getsizeRuleById(UUID id);

    FullSizeRuleResponse getFullsizeRuleById(UUID id);

    SizeRuleResponse createsizeRule(SizeRuleRequest sizeRuleRequest);

    SizeRuleResponse updateSizeRule(UUID id, UpdateSizeRuleRequest updatesizeRuleRequest);

    void deleteSizeRule(UUID id);
}
