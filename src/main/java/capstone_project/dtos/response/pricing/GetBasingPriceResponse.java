package capstone_project.dtos.response.pricing;

public record GetBasingPriceResponse(
        String id,
        String basePrice,

        VehicleTypeRuleResponse vehicleTypeRuleResponse,
        DistanceRuleResponse distanceRuleResponse
) {
}
