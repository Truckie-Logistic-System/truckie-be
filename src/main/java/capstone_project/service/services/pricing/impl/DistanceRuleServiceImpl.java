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
import capstone_project.repository.entityServices.pricing.VehicleRuleEntityService;
import capstone_project.service.mapper.order.DistanceRuleMapper;
import capstone_project.service.services.pricing.DistanceRuleService;
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
    private final VehicleRuleEntityService vehicleRuleEntityService;

    @Override
    public List<DistanceRuleResponse> getAllDistanceRules() {
        log.info("Fetching all pricing tiers");
        List<DistanceRuleEntity> pricingTierEntities = distanceRuleEntityService.findAll();
        if (pricingTierEntities.isEmpty()) {
            log.warn("No pricing tiers found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return pricingTierEntities.stream()
                .map(distanceRuleMapper::toDistanceRuleResponse)
                .toList();
    }

    @Override
    public DistanceRuleResponse getDistanceRuleById(UUID id) {
        log.info("Fetching pricing tier by ID: {}", id);
        DistanceRuleEntity distanceRuleEntity = distanceRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return distanceRuleMapper.toDistanceRuleResponse(distanceRuleEntity);
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

        log.info("Created pricing tier with ID: {}", savedEntity.getId());
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

        log.info("Updated pricing tier with ID: {}", savedEntity.getId());
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
