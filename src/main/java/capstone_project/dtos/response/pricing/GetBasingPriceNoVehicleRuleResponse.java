package capstone_project.dtos.response.pricing;

public record GetBasingPriceNoVehicleRuleResponse(
        String id,
        String basePrice,

        DistanceRuleResponse distanceRuleResponse
) {
}
