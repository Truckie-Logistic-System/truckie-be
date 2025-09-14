package capstone_project.service.services.setting;

import capstone_project.dtos.request.setting.UpdateWeightUnitSettingRequest;
import capstone_project.dtos.request.setting.WeightUnitSettingRequest;
import capstone_project.dtos.response.setting.WeightUnitSettingResponse;

import java.util.List;
import java.util.UUID;

public interface WeightUnitSettingService {
    List<WeightUnitSettingResponse> getAllWeightUnitSettings();

    WeightUnitSettingResponse getWeightUnitSettingById(UUID id);

    WeightUnitSettingResponse createContractSetting(WeightUnitSettingRequest weightUnitSettingRequest);

    WeightUnitSettingResponse updateWeightUnitSetting(UUID id, UpdateWeightUnitSettingRequest updateWeightUnitSettingRequest);

    void deleteWeightUnitSetting(UUID id);
}
