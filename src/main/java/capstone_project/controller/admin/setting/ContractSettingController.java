package capstone_project.controller.admin.setting;

import capstone_project.dtos.request.setting.ContractSettingRequest;
import capstone_project.dtos.request.setting.UpdateContractSettingRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.setting.ContractSettingResponse;
import capstone_project.service.services.setting.ContractSettingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${contract-setting.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ADMIN')")
public class ContractSettingController {

    private final ContractSettingService contractSettingService;

    @GetMapping()
    public ResponseEntity<ApiResponse<List<ContractSettingResponse>>> getAll() {
        final var result = contractSettingService.getAllContractSettingEntities();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractSettingResponse>> getById(@PathVariable UUID id) {
        final var result = contractSettingService.getContractSettingById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping()
    public ResponseEntity<ApiResponse<ContractSettingResponse>> create(@RequestBody @Valid ContractSettingRequest request) {
        final var result = contractSettingService.createContractSetting(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractSettingResponse>> update(@PathVariable UUID id,
                                                                      @RequestBody @Valid UpdateContractSettingRequest request) {
        final var result = contractSettingService.updateContractSetting(id, request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
