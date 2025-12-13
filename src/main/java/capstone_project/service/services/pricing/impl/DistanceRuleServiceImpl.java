package capstone_project.service.services.pricing.impl;

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
import capstone_project.service.services.pricing.DistanceRuleMetadataCalculator;
import capstone_project.service.services.pricing.DistanceRuleValidator;
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
    private final DistanceRuleMetadataCalculator metadataCalculator;
    private final DistanceRuleValidator validator;

    private static final String DISTANCE_RULE_ALL_CACHE_KEY = "distances:all";
    private static final String DISTANCE_RULE_BY_ID_CACHE_KEY_PREFIX = "distance:";

    @Override
    public List<DistanceRuleResponse> getAllDistanceRules() {

        List<DistanceRuleEntity> cachedDistanceRules = redisService.getList(DISTANCE_RULE_ALL_CACHE_KEY, DistanceRuleEntity.class);
        if (cachedDistanceRules != null && !cachedDistanceRules.isEmpty()) {
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

        // Ensure metadata is up to date
        metadataCalculator.calculateMetadataForAll(distanceRuleEntities);

        redisService.save(DISTANCE_RULE_ALL_CACHE_KEY, distanceRuleEntities);

        return distanceRuleEntities.stream()
                .map(distanceRuleMapper::toDistanceRuleResponse)
                .toList();
    }

    @Override
    public DistanceRuleResponse getDistanceRuleById(UUID id) {
        
        if (id == null) {
            log.error("Pricing tier ID is required");
            throw new BadRequestException("Pricing tier ID is required", ErrorEnum.REQUIRED.getErrorCode());
        }

        String cacheKey = DISTANCE_RULE_BY_ID_CACHE_KEY_PREFIX + id;
        DistanceRuleEntity cachedEntity = redisService.get(cacheKey, DistanceRuleEntity.class);

        if (cachedEntity != null) {
            
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
        log.info("Creating new distance rule: {} - {}", distanceRuleRequest.fromKm(), distanceRuleRequest.toKm());

        // Get all existing active rules for validation and metadata calculation
        List<DistanceRuleEntity> existingActiveRules = distanceRuleEntityService.findAll().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        // Create new entity
        DistanceRuleEntity distanceRuleEntity = distanceRuleMapper.mapRequestToEntity(distanceRuleRequest);
        distanceRuleEntity.setStatus("ACTIVE");

        // Rule A: Block adding range that would split base range
        if (metadataCalculator.wouldOverlapBaseRange(distanceRuleEntity, existingActiveRules)) {
            throw new BadRequestException(
                "Không thể thêm khoảng cách nằm trong khoảng giá gốc. Vui lòng chọn khoảng cách khác.", 
                ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Smart auto-adjustment: adjust existing ranges to make room for new range
        metadataCalculator.adjustForNewRange(distanceRuleEntity, existingActiveRules);

        // Save adjusted existing rules first
        existingActiveRules.forEach(distanceRuleEntityService::save);

        // Calculate metadata smartly for the new rule
        metadataCalculator.calculateMetadataForSingle(distanceRuleEntity, existingActiveRules);

        // Save the new rule
        DistanceRuleEntity savedEntity = distanceRuleEntityService.save(distanceRuleEntity);

        // Recalculate metadata for all rules to ensure consistency
        List<DistanceRuleEntity> allActiveRules = distanceRuleEntityService.findAll().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        metadataCalculator.calculateMetadataForAll(allActiveRules);
        allActiveRules.forEach(distanceRuleEntityService::save);

        // Clear cache
        clearAllCache();

        log.info("Created distance rule: {} ({})", savedEntity.getDisplayName(), savedEntity.getId());
        return distanceRuleMapper.toDistanceRuleResponse(savedEntity);
    }

    @Override
    public DistanceRuleResponse updateDistanceRule(UUID id, UpdateDistanceRuleRequest distanceRuleRequest) {
        log.info("Updating distance rule: {}", id);

        if (id == null) {
            log.error("Distance rule ID is required for updating");
            throw new BadRequestException("Distance rule ID is required", ErrorEnum.REQUIRED.getErrorCode());
        }

        DistanceRuleEntity existingEntity = distanceRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Get all active rules for validation and adjustment
        List<DistanceRuleEntity> allActiveRules = distanceRuleEntityService.findAll().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        // Update entity with new values
        distanceRuleMapper.toDistanceRuleEntity(distanceRuleRequest, existingEntity);

        // Smart auto-adjustment: adjust adjacent ranges to prevent gaps
        metadataCalculator.adjustAdjacentRanges(existingEntity, allActiveRules);

        // Recalculate metadata for the updated rule
        metadataCalculator.calculateMetadataForSingle(existingEntity, allActiveRules);

        // Save the updated rule first
        DistanceRuleEntity savedEntity = distanceRuleEntityService.save(existingEntity);

        // Save all adjusted rules
        allActiveRules.forEach(distanceRuleEntityService::save);

        // Recalculate metadata for all rules to ensure consistency
        allActiveRules = distanceRuleEntityService.findAll().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        metadataCalculator.calculateMetadataForAll(allActiveRules);
        allActiveRules.forEach(distanceRuleEntityService::save);

        // Clear cache
        clearAllCache();

        log.info("Updated distance rule: {} ({})", savedEntity.getDisplayName(), savedEntity.getId());
        return distanceRuleMapper.toDistanceRuleResponse(savedEntity);
    }

    @Override
    public void deleteDistanceRule(UUID id) {
        log.info("Deleting distance rule: {}", id);

        if (id == null) {
            log.error("Distance rule ID is required for deletion");
            throw new BadRequestException("Distance rule ID is required", ErrorEnum.REQUIRED.getErrorCode());
        }

        DistanceRuleEntity entity = distanceRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Get all active rules for validation
        List<DistanceRuleEntity> allActiveRules = distanceRuleEntityService.findAll().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        // Validate deletion
        validator.validateDeletion(entity, allActiveRules);

        // Soft delete by setting status to DELETED
        entity.setStatus("DELETED");
        distanceRuleEntityService.save(entity);

        // Get remaining active rules (excluding the deleted one)
        List<DistanceRuleEntity> remainingActiveRules = distanceRuleEntityService.findAll().stream()
                .filter(r -> "ACTIVE".equals(r.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        // Smart auto-adjustment: expand adjacent range to cover the deleted range
        metadataCalculator.adjustAfterDeletion(entity, remainingActiveRules);

        // Save adjusted rules
        remainingActiveRules.forEach(distanceRuleEntityService::save);

        // Recalculate metadata for remaining active rules
        metadataCalculator.calculateMetadataForAll(remainingActiveRules);
        remainingActiveRules.forEach(distanceRuleEntityService::save);

        // Validate minimum rules requirement
        validator.validateMinimumRules(distanceRuleEntityService.findAll());

        // Clear cache
        clearAllCache();

        log.info("Deleted distance rule: {}", id);
    }

    private void clearAllCache() {
        redisService.delete(DISTANCE_RULE_ALL_CACHE_KEY);
        
        // Clear all individual caches
        List<DistanceRuleEntity> allRules = distanceRuleEntityService.findAll();
        allRules.forEach(rule -> {
            redisService.delete(DISTANCE_RULE_BY_ID_CACHE_KEY_PREFIX + rule.getId());
        });
    }
}
