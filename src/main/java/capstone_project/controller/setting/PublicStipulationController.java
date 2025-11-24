package capstone_project.controller.setting;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.setting.StipulationSettingResponse;
import capstone_project.service.services.setting.StipulationSettingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public controller for customer to view stipulation settings
 * No authentication required
 */
@RestController
@RequestMapping("/api/v1/public/stipulations")
@RequiredArgsConstructor
public class PublicStipulationController {

    private final StipulationSettingService stipulationSettingService;

    /**
     * Get current stipulation settings for customers
     * Returns the first (and only) stipulation record
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<StipulationSettingResponse>> getCurrentStipulation() {
        final var result = stipulationSettingService.getAllStipulationSettings();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
