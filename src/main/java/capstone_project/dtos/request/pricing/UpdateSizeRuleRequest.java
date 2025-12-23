package capstone_project.dtos.request.pricing;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.SizeRuleEnum;
import capstone_project.common.enums.enumValidator.EnumValidator;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UpdateSizeRuleRequest(

//        @EnumValidator(enumClass = SizeRuleEnum.class, message = "Invalid vehicle rule name")
//        String sizeRuleName,

        @DecimalMin(value = "0.0", inclusive = true, message = "Min weight must be >= 0")
        BigDecimal minWeight,

        @DecimalMin(value = "0.0", inclusive = true, message = "Max weight must be >= 0")
        BigDecimal maxWeight,

        @DecimalMin(value = "0.0", inclusive = true, message = "Min length must be >= 0")
        BigDecimal minLength,

        @DecimalMin(value = "0.0", inclusive = true, message = "Max length must be >= 0")
        BigDecimal maxLength,

        @DecimalMin(value = "0.0", inclusive = true, message = "Min width must be >= 0")
        BigDecimal minWidth,

        @DecimalMin(value = "0.0", inclusive = true, message = "Max width must be >= 0")
        BigDecimal maxWidth,

        @DecimalMin(value = "0.0", inclusive = true, message = "Min height must be >= 0")
        BigDecimal minHeight,

        @DecimalMin(value = "0.0", inclusive = true, message = "Max height must be >= 0")
        BigDecimal maxHeight,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime effectiveFrom,

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime effectiveTo,

        @EnumValidator(enumClass = CommonStatusEnum.class, message = "Status must be one of: ACTIVE, INACTIVE, DELETED")
        String status,

        String vehicleTypeId,

        String categoryId
) {
}
