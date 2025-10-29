package capstone_project.dtos.request.pricing;

import capstone_project.common.enums.VehicleTypeRuleEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VehicleTypeRuleRequest(

        @NotBlank(message = "Vehicle rule name is required")
        @EnumValidator(enumClass = VehicleTypeRuleEnum.class, message = "Invalid vehicle rule name")
        String vehicleRuleName,

        @NotNull(message = "Min weight is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Max weight must be >= 0")
        BigDecimal minWeight,

        @NotNull(message = "Max weight is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Max weight must be >= 0")
        BigDecimal maxWeight,

        @NotNull(message = "Min length is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Max weight must be >= 0")
        BigDecimal minLength,

        @NotNull(message = "Max length is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Max weight must be >= 0")
        BigDecimal maxLength,

        @NotNull(message = "Min width is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Max weight must be >= 0")
        BigDecimal minWidth,

        @NotNull(message = "Max width is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Max weight must be >= 0")
        BigDecimal maxWidth,

        @NotNull(message = "Min height is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Max weight must be >= 0")
        BigDecimal minHeight,

        @NotNull(message = "Max height is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Max weight must be >= 0")
        BigDecimal maxHeight,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime effectiveFrom,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime effectiveTo,

        @NotBlank(message = "Vehicle type ID is required")
        String vehicleTypeId,

        @NotBlank(message = "Category ID is required")
        String categoryId

){

}
