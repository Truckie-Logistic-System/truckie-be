package capstone_project.dtos.response.pricing;

public record GetBasingPriceNoSizeRuleResponse(
        String id,
        String basePrice,

        DistanceRuleResponse distanceRuleResponse
) {
}
