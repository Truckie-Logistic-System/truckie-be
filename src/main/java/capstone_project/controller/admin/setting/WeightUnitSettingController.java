package capstone_project.controller.admin.setting;

import capstone_project.dtos.request.setting.UpdateWeightUnitSettingRequest;
import capstone_project.dtos.request.setting.WeightUnitSettingRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.setting.WeightUnitSettingResponse;
import capstone_project.service.services.setting.WeightUnitSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${weight-unit-setting.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class WeightUnitSettingController {

    private final WeightUnitSettingService weightUnitSettingService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<WeightUnitSettingResponse>>> getAll() {
        final var result = weightUnitSettingService.getAllWeightUnitSettings();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WeightUnitSettingResponse>> getById(@PathVariable("id") UUID id) {
        final var result = weightUnitSettingService.getWeightUnitSettingById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<WeightUnitSettingResponse>> create(@RequestBody @Valid WeightUnitSettingRequest weightUnitSettingRequest) {
        final var result = weightUnitSettingService.createContractSetting(weightUnitSettingRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<WeightUnitSettingResponse>> update(@PathVariable("id") UUID id,
                                                                        @RequestBody @Valid UpdateWeightUnitSettingRequest weightUnitSettingRequest) {
        final var result = weightUnitSettingService.updateWeightUnitSetting(id, weightUnitSettingRequest);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
