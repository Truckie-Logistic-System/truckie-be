package capstone_project.service.services.pricing.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.SizeRuleEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.pricing.UpdateSizeRuleRequest;
import capstone_project.dtos.request.pricing.SizeRuleRequest;
import capstone_project.dtos.response.pricing.FullSizeRuleResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceNoSizeRuleResponse;
import capstone_project.dtos.response.pricing.SizeRuleResponse;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
import capstone_project.repository.entityServices.pricing.SizeRuleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.order.BasingPriceMapper;
import capstone_project.service.mapper.order.SizeRuleMapper;
import capstone_project.service.services.pricing.SizeRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SizeRuleServiceImpl implements SizeRuleService {

    private final SizeRuleEntityService sizeRuleEntityService;
    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final BasingPriceEntityService basingPriceEntityService;
    private final SizeRuleMapper sizeRuleMapper;
    private final BasingPriceMapper basingPriceMapper;

    @Override
    public List<SizeRuleResponse> getAllsizeRules() {
        log.info("Fetching all vehicle rules");
        List<SizeRuleEntity> pricingRuleEntities = sizeRuleEntityService.findAll();
        if (pricingRuleEntities.isEmpty()) {
            log.warn("No vehicle rules found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return pricingRuleEntities.stream()
                .map(sizeRuleMapper::toSizeRuleResponse)
                .toList();
    }

    @Override
    public List<FullSizeRuleResponse> getAllFullsizeRules() {
        log.info("Fetching all full vehicle rules");
        List<SizeRuleEntity> sizeRuleEntities = sizeRuleEntityService.findAll();
        if (sizeRuleEntities.isEmpty()) {
            log.warn("No vehicle rules found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return sizeRuleEntities.stream()
                .map(sizeRuleEntity -> {
                    List<BasingPriceEntity> basingPriceEntities =
                            basingPriceEntityService.findAllBysizeRuleEntityId(sizeRuleEntity.getId());

                    List<GetBasingPriceNoSizeRuleResponse> basingPriceResponses = basingPriceEntities.isEmpty()
                            ? List.of()
                            : basingPriceEntities.stream()
                                .map(basingPriceMapper::toGetBasingPriceNoSizeRuleResponse)
                                .toList();

                    return sizeRuleMapper.toFullsizeRuleResponse(sizeRuleEntity, basingPriceResponses);
                })
                .toList();
    }


    @Override
    public SizeRuleResponse getsizeRuleById(UUID id) {
        log.info("Fetching vehicle rule by ID: {}", id);
        SizeRuleEntity sizeRuleEntity = sizeRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return sizeRuleMapper.toSizeRuleResponse(sizeRuleEntity);
    }

    @Override
    public FullSizeRuleResponse getFullsizeRuleById(UUID id) {
        SizeRuleEntity sizeRuleEntity = sizeRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        List<BasingPriceEntity> basingPriceEntities = basingPriceEntityService.findAllBysizeRuleEntityId(id);

        List<GetBasingPriceNoSizeRuleResponse> basingPriceResponses = basingPriceEntities.isEmpty()
                ? List.of()
                : basingPriceEntities.stream()
                    .map(basingPriceMapper::toGetBasingPriceNoSizeRuleResponse)
                    .toList();

        return sizeRuleMapper.toFullsizeRuleResponse(sizeRuleEntity, basingPriceResponses);
    }


    @Override
    public SizeRuleResponse createsizeRule(SizeRuleRequest sizeRuleRequest) {
        log.info("Creating new vehicle rule");

        if (sizeRuleRequest == null) {
            log.error("Vehicle rule request cannot be null");
            throw new BadRequestException("Vehicle rule request cannot be null", ErrorEnum.REQUIRED.getErrorCode());
        }

        if (sizeRuleRequest.categoryId() == null || sizeRuleRequest.vehicleTypeId() == null) {
            log.error("Category ID and Vehicle Type ID are required for creating a vehicle rule");
            throw new BadRequestException("Category ID and Vehicle Type ID are required",
                    ErrorEnum.REQUIRED.getErrorCode());
        }

        UUID categoryUuid = UUID.fromString(sizeRuleRequest.categoryId());
        UUID vehicleTypeUuid = UUID.fromString(sizeRuleRequest.vehicleTypeId());

        VehicleTypeEntity vehicleTypeEntity = vehicleTypeEntityService.findEntityById(vehicleTypeUuid)
                .orElseThrow(() -> {
                    log.error("Vehicle type with ID {} not found", vehicleTypeUuid);
                    return new NotFoundException("Vehicle type not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        Optional<SizeRuleEntity> existingRule = sizeRuleEntityService.findBySizeRuleName(sizeRuleRequest.sizeRuleName());
        if (existingRule.isPresent()) {
            log.error("Vehicle rule with name '{}' already exists", sizeRuleRequest.sizeRuleName());
            throw new BadRequestException("Vehicle rule with this name already exists",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        try {
            SizeRuleEnum.valueOf(sizeRuleRequest.sizeRuleName());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid vehicle rule type: " + sizeRuleRequest.sizeRuleName(),
                    ErrorEnum.ENUM_INVALID.getErrorCode());
        }

        if (!sizeRuleRequest.sizeRuleName().equalsIgnoreCase(vehicleTypeEntity.getVehicleTypeName())) {
            log.error("Vehicle rule name '{}' does not match vehicle type name '{}'",
                    sizeRuleRequest.sizeRuleName(), vehicleTypeEntity.getVehicleTypeName());
            throw new BadRequestException("Vehicle rule name must match vehicle type name ("
                    + vehicleTypeEntity.getVehicleTypeName() + ")", ErrorEnum.REQUIRED.getErrorCode());
        }

        if (sizeRuleEntityService.findByCategoryIdAndVehicleTypeEntityIdAndSizeRuleName(
                categoryUuid, vehicleTypeUuid, sizeRuleRequest.sizeRuleName()).isPresent()) {
            log.error("Vehicle rule with category ID {} and vehicle type ID {} already exists", categoryUuid, vehicleTypeUuid);
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        SizeRuleEntity sizeRuleEntity = sizeRuleMapper.mapRequestToEntity(sizeRuleRequest);
        sizeRuleEntity.setStatus(CommonStatusEnum.ACTIVE.name());

        SizeRuleEntity savedEntity = sizeRuleEntityService.save(sizeRuleEntity);

        log.info("Vehicle rule created successfully with ID: {}", savedEntity.getId());
        return sizeRuleMapper.toSizeRuleResponse(savedEntity);
    }

    @Override
    public SizeRuleResponse updateSizeRule(UUID id, UpdateSizeRuleRequest updateSizeRuleRequest) {
        log.info("Updating vehicle rule with ID: {}", id);

        SizeRuleEntity existingEntity = sizeRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (updateSizeRuleRequest.sizeRuleName() != null) {
            try {
                SizeRuleEnum.valueOf(updateSizeRuleRequest.sizeRuleName());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                        "Invalid vehicle rule type: " + updateSizeRuleRequest.sizeRuleName(),
                        ErrorEnum.ENUM_INVALID.getErrorCode()
                );
            }
        }

        sizeRuleMapper.toSizeRuleEntity(updateSizeRuleRequest, existingEntity);

        SizeRuleEntity updatedEntity = sizeRuleEntityService.save(existingEntity);

        log.info("Vehicle rule updated successfully with ID: {}", updatedEntity.getId());
        return sizeRuleMapper.toSizeRuleResponse(updatedEntity);
    }

    @Override
    public void deleteSizeRule(UUID id) {

    }

    public List<SizeRuleEntity> suggestVehicles(BigDecimal orderWeight) {
        return sizeRuleEntityService.findAll().stream()
                // chỉ lấy xe đủ tải
                .filter(vehicle -> vehicle.getMaxWeight().compareTo(orderWeight) >= 0)
                // sort theo độ chênh lệch tải trọng
                .sorted(Comparator.comparing(vehicle -> vehicle.getMaxWeight().subtract(orderWeight)))
                .collect(Collectors.toList());
    }


}
