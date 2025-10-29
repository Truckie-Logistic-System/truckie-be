package capstone_project.dtos.response.pricing;

import capstone_project.dtos.response.order.CategoryResponse;
import capstone_project.dtos.response.vehicle.VehicleTypeResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record VehicleTypeRuleResponse(
        String id,
        String vehicleRuleName,
        BigDecimal minWeight,
        BigDecimal maxWeight,
        BigDecimal minLength,
        BigDecimal maxLength,
        BigDecimal minWidth,
        BigDecimal maxWidth,
        BigDecimal minHeight,
        BigDecimal maxHeight,
        String status,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,

        CategoryResponse category,
        VehicleTypeResponse vehicleTypeEntity
) {
}
