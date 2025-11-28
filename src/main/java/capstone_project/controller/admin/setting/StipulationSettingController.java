package capstone_project.controller.admin.setting;

import capstone_project.dtos.request.setting.StipulationSettingRequest;
import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.setting.StipulationSettingResponse;
import capstone_project.service.services.setting.StipulationSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("${stipulation-setting.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ADMIN', 'STAFF')")
public class StipulationSettingController {

    private final StipulationSettingService stipulationSettingService;

    @GetMapping()
    public ResponseEntity<ApiResponse<StipulationSettingResponse>> getAll() {
        final var result = stipulationSettingService.getAllStipulationSettings();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<StipulationSettingResponse>> getById(@PathVariable UUID id) {
        final var result = stipulationSettingService.getStipulationSettingById(id);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping()
    public ResponseEntity<ApiResponse<StipulationSettingResponse>> createOrUpdate(@RequestBody StipulationSettingRequest request) {
        final var result = stipulationSettingService.createOrUpdateStipulationSettings(request);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        stipulationSettingService.deleteStipulationSetting(id);
        return ResponseEntity.noContent().build();
    }
}
