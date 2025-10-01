package capstone_project.controller.province;

import capstone_project.dtos.response.province.ProvinceResponse;
import capstone_project.service.services.thirdPartyServices.Province.ProvinceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("${province.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ProvinceController {

    private final ProvinceService provinceService;

    @GetMapping
    public ResponseEntity<List<ProvinceResponse>> getProvinces() {
        List<ProvinceResponse> provinces = provinceService.getAllProvinces();
        return ResponseEntity.ok(provinces);
    }
}
