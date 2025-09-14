package capstone_project.dtos.request.setting;

public record UpdateWeightUnitSettingRequest(

        String weightUnit,
        String description,
        String status
) {
}
