package capstone_project.service.services.pricing;

import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.entity.order.order.CategoryPricingDetailEntity;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.repository.entityServices.order.order.CategoryEntityService;
import capstone_project.repository.entityServices.order.order.CategoryPricingDetailEntityService;
import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
import capstone_project.repository.entityServices.pricing.DistanceRuleEntityService;
import capstone_project.repository.entityServices.pricing.SizeRuleEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Unified pricing service to ensure consistency between AI chatbot and backend contract pricing
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UnifiedPricingService {

    private final SizeRuleEntityService sizeRuleService;
    private final DistanceRuleEntityService distanceRuleService;
    private final BasingPriceEntityService basingPriceService;
    private final CategoryEntityService categoryEntityService;
    private final CategoryPricingDetailEntityService categoryPricingDetailService;

    /**
     * Calculate price using unified formula (matches AI chatbot logic)
     * 
     * @param vehicleId Vehicle type ID
     * @param distanceKm Distance in km
     * @param numberOfVehicles Number of vehicles
     * @param categoryId Category ID (optional)
     * @return Unified price calculation result
     */
    public UnifiedPriceResult calculatePrice(UUID vehicleId, BigDecimal distanceKm, 
                                            int numberOfVehicles, UUID categoryId) {
        try {
            log.info("🧮 Unified pricing: vehicle={}, distance={}km, vehicles={}, category={}", 
                    vehicleId, distanceKm, numberOfVehicles, categoryId);

            // 1. Get vehicle and distance rules
            SizeRuleEntity vehicle = sizeRuleService.findEntityById(vehicleId)
                    .orElseThrow(() -> new RuntimeException("Vehicle not found: " + vehicleId));

            List<DistanceRuleEntity> distanceRules = distanceRuleService.findAll()
                    .stream()
                    .sorted(Comparator.comparing(DistanceRuleEntity::getFromKm))
                    .toList();

            if (distanceRules.isEmpty()) {
                throw new RuntimeException("No distance rules found");
            }

            // 2. Calculate base price for ONE vehicle and get individual tier breakdowns
            List<TierCalculationResult> tierResults = calculateBasePriceForOneVehicle(vehicle, distanceRules, distanceKm);
            BigDecimal basePriceForOneVehicle = tierResults.stream()
                    .map(TierCalculationResult::getSubtotal)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // 3. Apply category adjustment to ONE vehicle price (CRITICAL: before multiplying by vehicles)
            BigDecimal adjustedPriceForOneVehicle = applyCategoryAdjustment(basePriceForOneVehicle, categoryId);

            // 4. Multiply by number of vehicles
            BigDecimal totalPrice = adjustedPriceForOneVehicle.multiply(BigDecimal.valueOf(numberOfVehicles));

            // 5. Round to nearest 1000 VND (matches AI chatbot)
            totalPrice = roundToNearestThousand(totalPrice);

            log.info("✅ Unified pricing result: {} VND (base: {}, adjusted: {}, vehicles: {})", 
                    totalPrice, basePriceForOneVehicle, adjustedPriceForOneVehicle, numberOfVehicles);

            return UnifiedPriceResult.success(totalPrice, basePriceForOneVehicle, adjustedPriceForOneVehicle, tierResults);

        } catch (Exception e) {
            log.error("❌ Error in unified pricing calculation", e);
            return UnifiedPriceResult.error(e.getMessage());
        }
    }

    /**
     * Calculate base price for one vehicle using distance tiers
     */
    private List<TierCalculationResult> calculateBasePriceForOneVehicle(SizeRuleEntity vehicle, 
                                                      List<DistanceRuleEntity> distanceRules, 
                                                      BigDecimal distanceKm) {
        List<TierCalculationResult> tierResults = new ArrayList<>();
        BigDecimal remainingDistance = distanceKm;
        
        for (DistanceRuleEntity distanceRule : distanceRules) {
            if (remainingDistance.compareTo(BigDecimal.ZERO) <= 0) break;

            BigDecimal from = distanceRule.getFromKm();
            BigDecimal to = distanceRule.getToKm();

            BasingPriceEntity basePrice = basingPriceService
                    .findBasingPriceEntityBysizeRuleEntityIdAndDistanceRuleEntityId(
                            vehicle.getId(), distanceRule.getId())
                    .orElseThrow(() -> new RuntimeException("No base price found for vehicle=" 
                            + vehicle.getSizeRuleName() + ", tier=" + from + "-" + to));

            // Fixed tier (0-4km)
            if (from.compareTo(BigDecimal.ZERO) == 0 && to.compareTo(BigDecimal.valueOf(4)) == 0) {
                BigDecimal tierPrice = basePrice.getBasePrice();
                remainingDistance = remainingDistance.subtract(to);
                log.debug("📍 Fixed tier 0-4km: {} VND", tierPrice);
                
                // Add tier result
                String distanceRange = "0-4 km";
                tierResults.add(new TierCalculationResult(distanceRange, tierPrice, tierPrice, to));
            } else {
                // Variable tier
                BigDecimal tierDistance = (to == null) ? remainingDistance : remainingDistance.min(to.subtract(from));
                BigDecimal tierPrice = basePrice.getBasePrice().multiply(tierDistance);
                remainingDistance = remainingDistance.subtract(tierDistance);
                log.debug("📍 Variable tier {}-{}km: {}km × {} = {} VND", 
                        from, to == null ? "∞" : to, tierDistance, basePrice.getBasePrice(), tierPrice);
                
                // Add tier result
                String distanceRange;
                if (to == null) {
                    distanceRange = ">=" + from + " km";
                } else {
                    distanceRange = from + "-" + to + " km";
                }
                tierResults.add(new TierCalculationResult(distanceRange, basePrice.getBasePrice(), tierPrice, tierDistance));
            }
        }

        return tierResults;
    }

    /**
     * Apply category adjustment (multiplier + extra fee)
     */
    private BigDecimal applyCategoryAdjustment(BigDecimal basePrice, UUID categoryId) {
        if (categoryId == null) {
            return basePrice;
        }

        CategoryEntity category = categoryEntityService.findEntityById(categoryId).orElse(null);
        if (category == null) {
            log.warn("⚠️ Category not found: {}", categoryId);
            return basePrice;
        }

        CategoryPricingDetailEntity pricingDetail = categoryPricingDetailService.findByCategoryId(categoryId);
        if (pricingDetail == null) {
            log.warn("⚠️ No pricing detail for category: {}", category.getCategoryName());
            return basePrice;
        }

        BigDecimal multiplier = pricingDetail.getPriceMultiplier() != null 
                ? pricingDetail.getPriceMultiplier() 
                : BigDecimal.ONE;
        BigDecimal extraFee = pricingDetail.getExtraFee() != null 
                ? pricingDetail.getExtraFee() 
                : BigDecimal.ZERO;

        BigDecimal adjustedPrice = basePrice.multiply(multiplier).add(extraFee);

        log.debug("🏷️ Category adjustment for {}: {} × {} + {} = {} VND", 
                category.getCategoryName(), basePrice, multiplier, extraFee, adjustedPrice);

        return adjustedPrice;
    }

    /**
     * Round price to nearest 1000 VND (matches AI chatbot rounding)
     */
    private BigDecimal roundToNearestThousand(BigDecimal price) {
        BigDecimal rounded = price.setScale(0, RoundingMode.HALF_UP);
        rounded = rounded.divide(BigDecimal.valueOf(1000), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(1000));
        return rounded;
    }

    /**
     * Result class for unified pricing
     */
    public static class UnifiedPriceResult {
        private final boolean success;
        private final BigDecimal totalPrice;
        private final BigDecimal basePriceForOneVehicle;
        private final BigDecimal adjustedPriceForOneVehicle;
        private final List<TierCalculationResult> tierResults;
        private final String errorMessage;

        private UnifiedPriceResult(boolean success, BigDecimal totalPrice, 
                                 BigDecimal basePriceForOneVehicle, BigDecimal adjustedPriceForOneVehicle,
                                 List<TierCalculationResult> tierResults, String errorMessage) {
            this.success = success;
            this.totalPrice = totalPrice;
            this.basePriceForOneVehicle = basePriceForOneVehicle;
            this.adjustedPriceForOneVehicle = adjustedPriceForOneVehicle;
            this.tierResults = tierResults;
            this.errorMessage = errorMessage;
        }

        public static UnifiedPriceResult success(BigDecimal totalPrice, 
                                               BigDecimal basePriceForOneVehicle, 
                                               BigDecimal adjustedPriceForOneVehicle,
                                               List<TierCalculationResult> tierResults) {
            return new UnifiedPriceResult(true, totalPrice, basePriceForOneVehicle, 
                    adjustedPriceForOneVehicle, tierResults, null);
        }

        public static UnifiedPriceResult error(String errorMessage) {
            return new UnifiedPriceResult(false, null, null, null, null, errorMessage);
        }

        public boolean isSuccess() { return success; }
        public BigDecimal getTotalPrice() { return totalPrice; }
        public BigDecimal getBasePriceForOneVehicle() { return basePriceForOneVehicle; }
        public BigDecimal getAdjustedPriceForOneVehicle() { return adjustedPriceForOneVehicle; }
        public List<TierCalculationResult> getTierResults() { return tierResults; }
        public String getErrorMessage() { return errorMessage; }
    }

    /**
     * Helper class to hold individual tier calculation result
     */
    public static class TierCalculationResult {
        private final String distanceRange;
        private final BigDecimal unitPrice;
        private final BigDecimal subtotal;
        private final BigDecimal appliedKm;

        public TierCalculationResult(String distanceRange, BigDecimal unitPrice, BigDecimal subtotal, BigDecimal appliedKm) {
            this.distanceRange = distanceRange;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
            this.appliedKm = appliedKm;
        }

        public String getDistanceRange() { return distanceRange; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public BigDecimal getSubtotal() { return subtotal; }
        public BigDecimal getAppliedKm() { return appliedKm; }
    }
}
