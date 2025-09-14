package capstone_project.service.services.pricing.impl;

import capstone_project.common.enums.DistanceRuleEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.pricing.DistanceRuleRequest;
import capstone_project.dtos.request.pricing.UpdateDistanceRuleRequest;
import capstone_project.dtos.response.pricing.DistanceRuleResponse;
import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.repository.entityServices.pricing.DistanceRuleEntityService;
import capstone_project.service.mapper.order.DistanceRuleMapper;
import capstone_project.service.services.pricing.DistanceRuleService;
import capstone_project.service.services.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DistanceRuleServiceImpl implements DistanceRuleService {

    private final DistanceRuleEntityService distanceRuleEntityService;
    private final DistanceRuleMapper distanceRuleMapper;
    private final RedisService redisService;

    private static final String DISTANCE_RULE_ALL_CACHE_KEY = "distances:all";
    private static final String DISTANCE_RULE_BY_ID_CACHE_KEY_PREFIX = "distance:";

    @Override
    public List<DistanceRuleResponse> getAllDistanceRules() {
        log.info("[getAllDistanceRules] Fetching distance rules");

        List<DistanceRuleEntity> cachedDistanceRules = redisService.getList(DISTANCE_RULE_ALL_CACHE_KEY, DistanceRuleEntity.class);
        if (cachedDistanceRules != null && !cachedDistanceRules.isEmpty()) {
            log.info("[getAllDistanceRules] Returning {} rules from cache", cachedDistanceRules.size());
            return cachedDistanceRules.stream()
                    .map(distanceRuleMapper::toDistanceRuleResponse)
                    .toList();
        }

        List<DistanceRuleEntity> distanceRuleEntities = distanceRuleEntityService.findAll();
        if (distanceRuleEntities.isEmpty()) {
            log.warn("[getAllDistanceRules] No distance rules found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        redisService.save(DISTANCE_RULE_ALL_CACHE_KEY, distanceRuleEntities);

        return distanceRuleEntities.stream()
                .map(distanceRuleMapper::toDistanceRuleResponse)
                .toList();
    }


    @Override
    public DistanceRuleResponse getDistanceRuleById(UUID id) {
        log.info("Fetching pricing tier by ID: {}", id);
        if (id == null) {
            log.error("Pricing tier ID is required");
            throw new BadRequestException("Pricing tier ID is required", ErrorEnum.REQUIRED.getErrorCode());
        }

        String cacheKey = DISTANCE_RULE_BY_ID_CACHE_KEY_PREFIX + id;
        DistanceRuleEntity cachedEntity = redisService.get(cacheKey, DistanceRuleEntity.class);

        if (cachedEntity != null) {
            log.info("Returning cached pricing tier for ID: {}", id);
            return distanceRuleMapper.toDistanceRuleResponse(cachedEntity);
        } else {
            DistanceRuleEntity entity = distanceRuleEntityService.findEntityById(id)
                    .orElseThrow(() -> new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            redisService.save(cacheKey, entity);

            return distanceRuleMapper.toDistanceRuleResponse(entity);
        }
    }

    @Override
    public DistanceRuleResponse createDistanceRule(DistanceRuleRequest distanceRuleRequest) {
        log.info("Creating new pricing tier");

        DistanceRuleEnum ruleEnum;
        try {
            ruleEnum = DistanceRuleEnum.valueOf(distanceRuleRequest.distanceRuleTier());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid pricing tier: " + distanceRuleRequest.distanceRuleTier(),
                    ErrorEnum.ENUM_INVALID.getErrorCode());
        }

        DistanceRuleEntity distanceRuleEntity = distanceRuleMapper.mapRequestToEntity(distanceRuleRequest);

        applyPricingTierEnum(distanceRuleEntity, ruleEnum);

        DistanceRuleEntity savedEntity = distanceRuleEntityService.save(distanceRuleEntity);

        redisService.delete(DISTANCE_RULE_ALL_CACHE_KEY);

        return distanceRuleMapper.toDistanceRuleResponse(savedEntity);
    }

    @Override
    public DistanceRuleResponse updateDistanceRule(UUID id, UpdateDistanceRuleRequest distanceRuleRequest) {
        log.info("Updating pricing tier by ID: {}", id);

        if (id == null) {
            log.error("Pricing tier ID is required for updating a pricing tier");
            throw new BadRequestException("Pricing tier ID is required", ErrorEnum.REQUIRED.getErrorCode());
        }

        DistanceRuleEntity existingEntity = distanceRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        distanceRuleMapper.toDistanceRuleEntity(distanceRuleRequest, existingEntity);

        DistanceRuleEntity savedEntity = distanceRuleEntityService.save(existingEntity);

        redisService.delete(DISTANCE_RULE_ALL_CACHE_KEY);
        redisService.delete(DISTANCE_RULE_BY_ID_CACHE_KEY_PREFIX + id);

        return distanceRuleMapper.toDistanceRuleResponse(savedEntity);
    }

    @Override
    public void deleteDistanceRule(UUID id) {

    }

    private void applyPricingTierEnum(DistanceRuleEntity entity, DistanceRuleEnum tierEnum) {
        try {
            entity.setToKm(BigDecimal.valueOf(tierEnum.getToKm()));
            entity.setFromKm(BigDecimal.valueOf(tierEnum.getFromKm()));

        } catch (IllegalArgumentException e) {
            log.error("Invalid PricingRuleEnum value: {}", tierEnum, e);
            throw new BadRequestException("Invalid pricing rule type: " + tierEnum,
                    ErrorEnum.INVALID.getErrorCode());
        }
    }
}
