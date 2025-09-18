package capstone_project.dtos.request.setting;

import capstone_project.common.enums.WeightUnitEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;
import jakarta.validation.constraints.NotBlank;

public record WeightUnitSettingRequest(

        @NotBlank(message = "Weight unit cannot be blank")
        @EnumValidator(enumClass = WeightUnitEnum.class, message = "Weight unit must be a valid category")
        String weightUnit,

        String description
) {
}
