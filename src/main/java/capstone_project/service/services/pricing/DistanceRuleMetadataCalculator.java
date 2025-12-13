package capstone_project.service.services.pricing;

import capstone_project.entity.pricing.DistanceRuleEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Smart calculator for distance rule metadata.
 * Auto-generates display names, display order, and is_base_price flag.
 * User-friendly: admin only needs to input fromKm and toKm, all else is calculated.
 */
@Component
@Slf4j
public class DistanceRuleMetadataCalculator {

    private static final BigDecimal MAX_KM_THRESHOLD = new BigDecimal("99999");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    /**
     * Calculate all metadata for a list of distance rules.
     * This ensures consistent metadata across all rules.
     */
    public void calculateMetadataForAll(List<DistanceRuleEntity> rules) {
        if (rules == null || rules.isEmpty()) {
            return;
        }

        // Sort by fromKm ascending for proper ordering
        rules.sort(Comparator.comparing(DistanceRuleEntity::getFromKm));

        // Calculate display order and is_base_price
        for (int i = 0; i < rules.size(); i++) {
            DistanceRuleEntity rule = rules.get(i);
            rule.setDisplayOrder(i);
            rule.setIsBasePrice(i == 0); // First range is always base price
            rule.setDisplayName(generateDisplayName(rule.getFromKm(), rule.getToKm()));
        }

        log.info("Calculated metadata for {} distance rules", rules.size());
    }

    /**
     * Calculate metadata for a single distance rule based on existing rules.
     * This is used when creating or updating a rule.
     */
    public void calculateMetadataForSingle(DistanceRuleEntity rule, List<DistanceRuleEntity> existingRules) {
        // Generate display name
        rule.setDisplayName(generateDisplayName(rule.getFromKm(), rule.getToKm()));

        // Calculate display order based on fromKm position
        long lowerRangesCount = existingRules.stream()
                .filter(r -> !r.getId().equals(rule.getId())) // Exclude self if updating
                .filter(r -> r.getFromKm().compareTo(rule.getFromKm()) < 0)
                .count();
        rule.setDisplayOrder((int) lowerRangesCount);

        // Determine if this is the base price (starts at 0)
        rule.setIsBasePrice(rule.getFromKm().compareTo(ZERO) == 0);

        log.debug("Calculated metadata for distance rule: {} (order={}, isBase={})", 
                rule.getDisplayName(), rule.getDisplayOrder(), rule.getIsBasePrice());
    }

    /**
     * Smart generation of display name based on distance range.
     * Examples:
     * - 0.00 to 3.99 -> "4KM ĐẦU"
     * - 4.00 to 14.99 -> "5-15KM"
     * - 15.00 to 99.99 -> "16-100KM"
     * - 100.00 to 99999 -> ">100KM"
     */
    public String generateDisplayName(BigDecimal fromKm, BigDecimal toKm) {
        if (fromKm == null || toKm == null) {
            return "N/A";
        }

        // Base price range (starts from 0)
        if (fromKm.compareTo(ZERO) == 0) {
            int upperBound = toKm.add(BigDecimal.valueOf(0.01)).intValue();
            return upperBound + "KM ĐẦU";
        }

        // Open-ended range (very high toKm)
        if (toKm.compareTo(MAX_KM_THRESHOLD) >= 0) {
            int lowerBound = fromKm.intValue();
            return ">" + lowerBound + "KM";
        }

        // Standard range
        int lowerBound = fromKm.intValue();
        int upperBound = toKm.add(BigDecimal.valueOf(0.01)).intValue();
        return lowerBound + "-" + upperBound + "KM";
    }

    /**
     * Recalculate display order for all rules after a change.
     * This ensures no gaps in display order sequence.
     */
    public void recalculateDisplayOrder(List<DistanceRuleEntity> rules) {
        rules.sort(Comparator.comparing(DistanceRuleEntity::getFromKm));
        for (int i = 0; i < rules.size(); i++) {
            rules.get(i).setDisplayOrder(i);
        }
    }

    /**
     * Update is_base_price flag for all rules.
     * Ensures only the first range (fromKm = 0) is marked as base price.
     */
    public void recalculateBasePriceFlag(List<DistanceRuleEntity> rules) {
        rules.forEach(rule -> {
            boolean isBase = rule.getFromKm().compareTo(ZERO) == 0;
            rule.setIsBasePrice(isBase);
        });
    }

    /**
     * Regenerate display names for all rules.
     * Useful after bulk updates to ensure consistency.
     */
    public void regenerateDisplayNames(List<DistanceRuleEntity> rules) {
        rules.forEach(rule -> {
            String displayName = generateDisplayName(rule.getFromKm(), rule.getToKm());
            rule.setDisplayName(displayName);
        });
    }

    /**
     * Smart auto-adjustment of adjacent ranges to prevent gaps.
     * When a range is updated, this method adjusts the previous and next ranges
     * to maintain continuity (e.g., fromKm + 0.01 = previous toKm).
     * 
     * @param updatedRule The rule that was just updated
     * @param allRules All active rules including the updated one
     */
    public void adjustAdjacentRanges(DistanceRuleEntity updatedRule, List<DistanceRuleEntity> allRules) {
        if (allRules == null || allRules.size() < 2) {
            return;
        }

        // Sort by fromKm ascending
        allRules.sort(Comparator.comparing(DistanceRuleEntity::getFromKm));

        // Find the index of the updated rule
        int updatedIndex = -1;
        for (int i = 0; i < allRules.size(); i++) {
            if (allRules.get(i).getId().equals(updatedRule.getId())) {
                updatedIndex = i;
                break;
            }
        }

        if (updatedIndex == -1) {
            log.warn("Updated rule not found in allRules list");
            return;
        }

        // Adjust previous range's toKm to be updatedRule.fromKm - 0.01
        if (updatedIndex > 0) {
            DistanceRuleEntity prevRule = allRules.get(updatedIndex - 1);
            BigDecimal newToKm = updatedRule.getFromKm().subtract(BigDecimal.valueOf(0.01));
            if (newToKm.compareTo(prevRule.getFromKm()) > 0) {
                prevRule.setToKm(newToKm);
                prevRule.setDisplayName(generateDisplayName(prevRule.getFromKm(), prevRule.getToKm()));
                log.info("Adjusted previous range {} toKm to {}", prevRule.getId(), newToKm);
            }
        }

        // Adjust next range's fromKm to be updatedRule.toKm + 0.01
        if (updatedIndex < allRules.size() - 1) {
            DistanceRuleEntity nextRule = allRules.get(updatedIndex + 1);
            BigDecimal newFromKm = updatedRule.getToKm().add(BigDecimal.valueOf(0.01));
            if (newFromKm.compareTo(nextRule.getToKm()) < 0) {
                nextRule.setFromKm(newFromKm);
                nextRule.setDisplayName(generateDisplayName(nextRule.getFromKm(), nextRule.getToKm()));
                log.info("Adjusted next range {} fromKm to {}", nextRule.getId(), newFromKm);
            }
        }
    }

    /**
     * Smart adjustment after deleting a range.
     * Handles all cases: delete first, delete middle, delete last.
     * Expands adjacent range to cover the gap left by the deleted range.
     * 
     * @param deletedRule The rule that was deleted
     * @param remainingRules All remaining active rules
     */
    public void adjustAfterDeletion(DistanceRuleEntity deletedRule, List<DistanceRuleEntity> remainingRules) {
        if (remainingRules == null || remainingRules.isEmpty()) {
            return;
        }

        // Sort by fromKm ascending
        remainingRules.sort(Comparator.comparing(DistanceRuleEntity::getFromKm));

        // Case 1: Deleted the FIRST range (base price range)
        // -> Expand the new first range backward to start from 0
        if (deletedRule.getFromKm().compareTo(ZERO) == 0) {
            DistanceRuleEntity newFirstRange = remainingRules.get(0);
            newFirstRange.setFromKm(ZERO);
            newFirstRange.setIsBasePrice(true);
            newFirstRange.setDisplayName(generateDisplayName(newFirstRange.getFromKm(), newFirstRange.getToKm()));
            log.info("Deleted first range. New first range {} expanded to start from 0", newFirstRange.getId());
            return;
        }

        // Case 2: Deleted the LAST range (open-ended range)
        // -> Expand the new last range to become open-ended
        DistanceRuleEntity lastRemaining = remainingRules.get(remainingRules.size() - 1);
        if (deletedRule.getToKm().compareTo(MAX_KM_THRESHOLD) >= 0) {
            lastRemaining.setToKm(deletedRule.getToKm());
            lastRemaining.setDisplayName(generateDisplayName(lastRemaining.getFromKm(), lastRemaining.getToKm()));
            log.info("Deleted last range. New last range {} expanded to be open-ended", lastRemaining.getId());
            return;
        }

        // Case 3: Deleted a MIDDLE range
        // -> Expand the previous range to cover the deleted range's toKm
        DistanceRuleEntity prevRange = null;
        for (DistanceRuleEntity rule : remainingRules) {
            if (rule.getToKm().compareTo(deletedRule.getFromKm().subtract(BigDecimal.valueOf(0.01))) == 0) {
                prevRange = rule;
                break;
            }
        }

        if (prevRange != null) {
            // Expand previous range to cover deleted range
            prevRange.setToKm(deletedRule.getToKm());
            prevRange.setDisplayName(generateDisplayName(prevRange.getFromKm(), prevRange.getToKm()));
            log.info("Deleted middle range. Previous range {} expanded toKm to {}", 
                    prevRange.getId(), prevRange.getToKm());
        } else {
            // Fallback: expand next range backward
            for (DistanceRuleEntity rule : remainingRules) {
                if (rule.getFromKm().compareTo(deletedRule.getToKm().add(BigDecimal.valueOf(0.01))) == 0) {
                    rule.setFromKm(deletedRule.getFromKm());
                    rule.setDisplayName(generateDisplayName(rule.getFromKm(), rule.getToKm()));
                    log.info("Deleted middle range. Next range {} expanded fromKm to {}", 
                            rule.getId(), rule.getFromKm());
                    break;
                }
            }
        }
    }

    /**
     * Smart adjustment when adding a new range.
     * Handles all cases: add at end (new becomes open-ended), add in middle.
     * Rule A: KHÔNG cho phép thêm range đâm vào trong base range (from=0).
     * Rule B1: Range mới cuối là open-ended, range cuối cũ bị cắt lại.
     * 
     * @param newRule The new rule being added
     * @param existingRules All existing active rules (not including the new one)
     */
    public void adjustForNewRange(DistanceRuleEntity newRule, List<DistanceRuleEntity> existingRules) {
        if (existingRules == null || existingRules.isEmpty()) {
            return;
        }

        // Sort by fromKm ascending
        existingRules.sort(Comparator.comparing(DistanceRuleEntity::getFromKm));

        DistanceRuleEntity currentLastRange = existingRules.get(existingRules.size() - 1);

        // Case 1: Adding a new LAST range (newRule.fromKm > currentLastRange.fromKm)
        // -> New range becomes open-ended, current last range gets cut
        if (newRule.getFromKm().compareTo(currentLastRange.getFromKm()) > 0) {
            // Cut current last range's toKm to newRule.fromKm - 0.01
            BigDecimal newToKm = newRule.getFromKm().subtract(BigDecimal.valueOf(0.01));
            currentLastRange.setToKm(newToKm);
            currentLastRange.setDisplayName(generateDisplayName(currentLastRange.getFromKm(), currentLastRange.getToKm()));
            
            // New range becomes open-ended (toKm = 99999999)
            newRule.setToKm(new BigDecimal("99999999"));
            newRule.setDisplayName(generateDisplayName(newRule.getFromKm(), newRule.getToKm()));
            
            log.info("Added new last range. Previous last range {} cut toKm to {}. New range is open-ended.", 
                    currentLastRange.getId(), newToKm);
            return;
        }

        // Case 2: Adding a range in the MIDDLE
        // -> Shrink the overlapping range and adjust next range if needed
        for (int i = 0; i < existingRules.size(); i++) {
            DistanceRuleEntity existing = existingRules.get(i);
            
            // If new range starts within an existing range (not base), shrink the existing range
            if (newRule.getFromKm().compareTo(existing.getFromKm()) > 0 
                    && newRule.getFromKm().compareTo(existing.getToKm()) <= 0) {
                // Shrink existing range's toKm to newRule.fromKm - 0.01
                BigDecimal shrunkToKm = newRule.getFromKm().subtract(BigDecimal.valueOf(0.01));
                existing.setToKm(shrunkToKm);
                existing.setDisplayName(generateDisplayName(existing.getFromKm(), existing.getToKm()));
                log.info("Shrunk existing range {} toKm to {} for new middle range", existing.getId(), shrunkToKm);
            }

            // If there's a next range and new range's toKm overlaps with it, adjust next range's fromKm
            if (i < existingRules.size() - 1) {
                DistanceRuleEntity nextRange = existingRules.get(i + 1);
                if (newRule.getToKm().compareTo(nextRange.getFromKm()) >= 0 
                        && newRule.getToKm().compareTo(nextRange.getToKm()) < 0) {
                    // Adjust next range's fromKm to newRule.toKm + 0.01
                    BigDecimal newFromKm = newRule.getToKm().add(BigDecimal.valueOf(0.01));
                    nextRange.setFromKm(newFromKm);
                    nextRange.setDisplayName(generateDisplayName(nextRange.getFromKm(), nextRange.getToKm()));
                    log.info("Adjusted next range {} fromKm to {} for new middle range", nextRange.getId(), newFromKm);
                }
            }
        }
    }

    /**
     * Check if a new range would overlap with the base price range (from=0).
     * Used to enforce Rule A: KHÔNG cho phép thêm range đâm vào trong base range.
     * 
     * @param newRule The new rule being added
     * @param existingRules All existing active rules
     * @return true if the new range would overlap with base range
     */
    public boolean wouldOverlapBaseRange(DistanceRuleEntity newRule, List<DistanceRuleEntity> existingRules) {
        if (existingRules == null || existingRules.isEmpty()) {
            return false;
        }

        // Find base range (from=0)
        DistanceRuleEntity baseRange = existingRules.stream()
                .filter(r -> r.getFromKm().compareTo(ZERO) == 0)
                .findFirst()
                .orElse(null);

        if (baseRange == null) {
            return false;
        }

        // Check if new range starts within base range (would split it)
        return newRule.getFromKm().compareTo(ZERO) > 0 
                && newRule.getFromKm().compareTo(baseRange.getToKm()) <= 0
                && newRule.getToKm().compareTo(baseRange.getToKm()) <= 0;
    }
}
