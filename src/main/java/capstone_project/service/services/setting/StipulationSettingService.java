package capstone_project.service.services.setting;

import capstone_project.dtos.request.setting.StipulationSettingRequest;
import capstone_project.dtos.response.setting.StipulationSettingResponse;

import java.util.UUID;

public interface StipulationSettingService {
    StipulationSettingResponse getAllStipulationSettings();

    StipulationSettingResponse getStipulationSettingById(UUID id);

//    StipulationSettingResponse createStipulationSettings(StipulationSettingRequest request);
//
//    StipulationSettingResponse updateStipulationSettings(UUID id, StipulationSettingRequest request);

    StipulationSettingResponse createOrUpdateStipulationSettings(StipulationSettingRequest request);

    void deleteStipulationSetting(UUID id);
}
