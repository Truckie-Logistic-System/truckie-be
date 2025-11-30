package capstone_project.service.services.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Utility class for pricing calculations with consistent rounding
 * Ensures all monetary values across the system use the same rounding logic
 */
public class PricingUtils {

    /**
     * Round price to nearest 1000 VND
     * This matches the rounding logic used in UnifiedPricingService
     * 
     * @param price The price to round
     * @return Price rounded to nearest 1000 VND
     */
    public static BigDecimal roundToNearestThousand(BigDecimal price) {
        if (price == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal rounded = price.setScale(0, RoundingMode.HALF_UP);
        rounded = rounded.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(1000));
        return rounded;
    }

    /**
     * Calculate and round deposit amount
     * 
     * @param totalValue Total contract value
     * @param depositPercent Deposit percentage (e.g., 30 for 30%)
     * @return Rounded deposit amount
     */
    public static BigDecimal calculateRoundedDeposit(BigDecimal totalValue, BigDecimal depositPercent) {
        if (totalValue == null || depositPercent == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal depositAmount = totalValue.multiply(depositPercent)
                .divide(BigDecimal.valueOf(100), RoundingMode.HALF_UP);
        
        return roundToNearestThousand(depositAmount);
    }

    /**
     * Calculate and round remaining amount after deposit
     * 
     * @param totalValue Total contract value
     * @param depositAmount Deposit amount (should already be rounded)
     * @return Rounded remaining amount
     */
    public static BigDecimal calculateRoundedRemaining(BigDecimal totalValue, BigDecimal depositAmount) {
        if (totalValue == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal remainingAmount = totalValue.subtract(depositAmount != null ? depositAmount : BigDecimal.ZERO);
        return roundToNearestThousand(remainingAmount);
    }

    /**
     * Round monetary value for display purposes
     * 
     * @param value The monetary value
     * @return Rounded value as integer for display
     */
    public static int roundForDisplay(BigDecimal value) {
        return roundToNearestThousand(value).intValue();
    }
}
