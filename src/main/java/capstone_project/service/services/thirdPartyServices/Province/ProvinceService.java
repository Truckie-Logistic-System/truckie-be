package capstone_project.service.services.thirdPartyServices.Province;

import capstone_project.dtos.response.province.ProvinceResponse;

import java.util.List;

public interface ProvinceService {
    List<ProvinceResponse> getAllProvinces();
}
