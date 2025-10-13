package capstone_project.dtos.request.setting;

import java.util.Map;

public record StipulationSettingRequest(
        Map<String, String> contents
) {
}
