package capstone_project.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum DistanceRuleEnum {
    TIER_1(0.00, 3.99),
    TIER_2(4.0, 14.99),
    TIER_3(15.0, 99.99),
    TIER_4(100.0, 99999999.0);

    private final Double fromKm;
    private final Double toKm;

    public static DistanceRuleEnum fromDistance(int distance) {
        for (DistanceRuleEnum tier : values()) {
            if (distance >= tier.getFromKm() && distance <= tier.getToKm()) {
                return tier;
            }
        }
        throw new IllegalArgumentException("No pricing tier found for distance: " + distance);
    }
}

