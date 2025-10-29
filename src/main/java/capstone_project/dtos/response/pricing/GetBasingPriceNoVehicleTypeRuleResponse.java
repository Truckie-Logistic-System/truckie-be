package capstone_project.dtos.response.pricing;

public record GetBasingPriceNoVehicleTypeRuleResponse(
        String id,
        String basePrice,

        DistanceRuleResponse distanceRuleResponse
) {
}
