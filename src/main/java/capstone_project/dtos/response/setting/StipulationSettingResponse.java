package capstone_project.dtos.response.setting;

import java.util.Map;

public record StipulationSettingResponse(
        String id,
        Map<String, String> contents
) {
}
