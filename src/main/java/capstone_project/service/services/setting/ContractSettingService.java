package capstone_project.service.services.setting;

import capstone_project.dtos.request.setting.ContractSettingRequest;
import capstone_project.dtos.request.setting.UpdateContractSettingRequest;
import capstone_project.dtos.response.setting.ContractSettingResponse;

import java.util.List;
import java.util.UUID;

public interface ContractSettingService {
    List<ContractSettingResponse> getAllContractSettingEntities();

    ContractSettingResponse getContractSettingById(UUID id);

    ContractSettingResponse createContractSetting(ContractSettingRequest contractSettingRequest);

    ContractSettingResponse updateContractSetting(UUID id, UpdateContractSettingRequest contractSettingRequest);

    void deleteContractSetting(UUID id);
}
