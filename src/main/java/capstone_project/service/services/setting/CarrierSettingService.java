package capstone_project.service.services.setting;

import capstone_project.dtos.request.setting.CarrierSettingRequest;
import capstone_project.dtos.response.setting.CarrierSettingResponse;

import java.util.List;
import java.util.UUID;

public interface CarrierSettingService {
    List<CarrierSettingResponse> findAll();
    CarrierSettingResponse findById(UUID id);
    CarrierSettingResponse create(CarrierSettingRequest request);
    CarrierSettingResponse update(UUID id, CarrierSettingRequest request);
    void delete(UUID id);
}
