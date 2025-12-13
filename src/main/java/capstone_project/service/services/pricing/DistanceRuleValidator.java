package capstone_project.service.services.pricing;

import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.enums.ErrorEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Validator for distance rule business logic.
 * Ensures data integrity and prevents invalid configurations.
 */
@Component
@Slf4j
public class DistanceRuleValidator {

    private static final BigDecimal ZERO = BigDecimal.ZERO;
    private static final BigDecimal MAX_KM = new BigDecimal("999999.99");

    /**
     * Validate a distance rule before creation or update.
     */
    public void validateDistanceRule(DistanceRuleEntity rule, List<DistanceRuleEntity> existingRules) {
        validateRangeValues(rule);
        validateNoOverlap(rule, existingRules);
        validateBasePrice(rule, existingRules);
    }

    /**
     * Validate that fromKm < toKm and values are positive.
     */
    private void validateRangeValues(DistanceRuleEntity rule) {
        BigDecimal fromKm = rule.getFromKm();
        BigDecimal toKm = rule.getToKm();

        if (fromKm == null || toKm == null) {
            throw new BadRequestException("Khoảng cách từ và đến không được để trống", ErrorEnum.REQUIRED.getErrorCode());
        }

        if (fromKm.compareTo(ZERO) < 0) {
            throw new BadRequestException("Khoảng cách từ phải >= 0", ErrorEnum.INVALID.getErrorCode());
        }

        if (toKm.compareTo(fromKm) <= 0) {
            throw new BadRequestException("Khoảng cách đến phải lớn hơn khoảng cách từ", ErrorEnum.INVALID.getErrorCode());
        }

        if (toKm.compareTo(MAX_KM) > 0) {
            throw new BadRequestException("Khoảng cách đến không được vượt quá " + MAX_KM + " km", ErrorEnum.INVALID.getErrorCode());
        }
    }

    /**
     * Validate that the new range does not overlap with existing ranges.
     */
    private void validateNoOverlap(DistanceRuleEntity rule, List<DistanceRuleEntity> existingRules) {
        UUID ruleId = rule.getId();
        BigDecimal fromKm = rule.getFromKm();
        BigDecimal toKm = rule.getToKm();

        List<DistanceRuleEntity> activeRules = existingRules.stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .filter(r -> !r.getId().equals(ruleId)) // Exclude self when updating
                .collect(Collectors.toList());

        for (DistanceRuleEntity existing : activeRules) {
            BigDecimal existingFrom = existing.getFromKm();
            BigDecimal existingTo = existing.getToKm();

            // Check for overlap: [fromKm, toKm] overlaps with [existingFrom, existingTo]
            boolean overlaps = fromKm.compareTo(existingTo) < 0 && toKm.compareTo(existingFrom) > 0;

            if (overlaps) {
                throw new BadRequestException(
                    String.format("Khoảng cách [%.2f - %.2f] bị trùng lặp với khoảng cách hiện có [%.2f - %.2f]",
                            fromKm, toKm, existingFrom, existingTo),
                    ErrorEnum.INVALID.getErrorCode()
                );
            }
        }
    }

    /**
     * Validate base price rules.
     * - Must have exactly one range starting from 0
     * - Cannot delete the base price range if it's the last one
     */
    private void validateBasePrice(DistanceRuleEntity rule, List<DistanceRuleEntity> existingRules) {
        boolean isNewBasePrice = rule.getFromKm().compareTo(ZERO) == 0;
        
        if (isNewBasePrice) {
            // Check if there's already an active base price
            boolean hasExistingBasePrice = existingRules.stream()
                    .filter(r -> "ACTIVE".equals(r.getStatus()))
                    .filter(r -> !r.getId().equals(rule.getId()))
                    .anyMatch(r -> r.getFromKm().compareTo(ZERO) == 0);

            if (hasExistingBasePrice) {
                throw new BadRequestException("Đã tồn tại khoảng cách giá gốc (bắt đầu từ 0km). Vui lòng xóa hoặc chỉnh sửa khoảng cách hiện có trước.", ErrorEnum.INVALID.getErrorCode());
            }
        }
    }

    /**
     * Validate that deletion is allowed.
     * Cannot delete base price if no other base price exists.
     */
    public void validateDeletion(DistanceRuleEntity rule, List<DistanceRuleEntity> allRules) {
        if (rule.getFromKm().compareTo(ZERO) == 0) {
            long otherBasePriceCount = allRules.stream()
                    .filter(r -> "ACTIVE".equals(r.getStatus()))
                    .filter(r -> !r.getId().equals(rule.getId()))
                    .filter(r -> r.getFromKm().compareTo(ZERO) == 0)
                    .count();

            if (otherBasePriceCount == 0) {
                throw new BadRequestException("Không thể xóa khoảng cách giá gốc duy nhất. Vui lòng tạo khoảng cách giá gốc mới trước khi xóa.", ErrorEnum.INVALID.getErrorCode());
            }
        }
    }

    /**
     * Validate that there's at least one active distance rule.
     */
    public void validateMinimumRules(List<DistanceRuleEntity> rules) {
        long activeCount = rules.stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .count();

        if (activeCount == 0) {
            throw new BadRequestException("Phải có ít nhất một khoảng cách hoạt động trong hệ thống", ErrorEnum.INVALID.getErrorCode());
        }
    }

    /**
     * Validate that ranges cover continuous distance spectrum.
     * Warning only - not enforced to allow flexible configuration.
     */
    public void checkContinuity(List<DistanceRuleEntity> rules) {
        List<DistanceRuleEntity> activeRules = rules.stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .sorted((r1, r2) -> r1.getFromKm().compareTo(r2.getFromKm()))
                .collect(Collectors.toList());

        if (activeRules.size() < 2) {
            return; // No need to check continuity
        }

        for (int i = 0; i < activeRules.size() - 1; i++) {
            DistanceRuleEntity current = activeRules.get(i);
            DistanceRuleEntity next = activeRules.get(i + 1);

            // Check if there's a gap between current.toKm and next.fromKm
            if (current.getToKm().compareTo(next.getFromKm()) < 0) {
                log.warn("Gap detected between distance ranges: [%.2f - %.2f] and [%.2f - %.2f]",
                        current.getFromKm(), current.getToKm(), next.getFromKm(), next.getToKm());
            }
        }
    }
}
