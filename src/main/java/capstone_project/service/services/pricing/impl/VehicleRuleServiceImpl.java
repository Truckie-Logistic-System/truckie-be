package capstone_project.service.services.pricing.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.VehicleRuleEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.pricing.UpdateVehicleRuleRequest;
import capstone_project.dtos.request.pricing.VehicleRuleRequest;
import capstone_project.dtos.response.pricing.FullVehicleRuleResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceNoVehicleRuleResponse;
import capstone_project.dtos.response.pricing.VehicleRuleResponse;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
import capstone_project.repository.entityServices.pricing.VehicleRuleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.order.BasingPriceMapper;
import capstone_project.service.mapper.order.VehicleRuleMapper;
import capstone_project.service.services.pricing.VehicleRuleService;
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
public class VehicleRuleServiceImpl implements VehicleRuleService {

    private final VehicleRuleEntityService vehicleRuleEntityService;
    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final BasingPriceEntityService basingPriceEntityService;
    private final VehicleRuleMapper vehicleRuleMapper;
    private final BasingPriceMapper basingPriceMapper;

    @Override
    public List<VehicleRuleResponse> getAllVehicleRules() {
        log.info("Fetching all vehicle rules");
        List<VehicleRuleEntity> pricingRuleEntities = vehicleRuleEntityService.findAll();
        if (pricingRuleEntities.isEmpty()) {
            log.warn("No vehicle rules found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return pricingRuleEntities.stream()
                .map(vehicleRuleMapper::toVehicleRuleResponse)
                .toList();
    }

    @Override
    public List<FullVehicleRuleResponse> getAllFullVehicleRules() {
        log.info("Fetching all full vehicle rules");
        List<VehicleRuleEntity> vehicleRuleEntities = vehicleRuleEntityService.findAll();
        if (vehicleRuleEntities.isEmpty()) {
            log.warn("No vehicle rules found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return vehicleRuleEntities.stream()
                .map(vehicleRuleEntity -> {
                    List<BasingPriceEntity> basingPriceEntities =
                            basingPriceEntityService.findAllByVehicleRuleEntityId(vehicleRuleEntity.getId());

                    GetBasingPriceNoVehicleRuleResponse basingPriceResponse = basingPriceEntities.isEmpty()
                            ? null
                            : basingPriceMapper.toGetBasingPriceNoVehicleRuleResponse(basingPriceEntities.get(0));

                    return vehicleRuleMapper.toFullVehicleRuleResponse(vehicleRuleEntity, basingPriceResponse);
                })
                .toList();
    }


    @Override
    public VehicleRuleResponse getVehicleRuleById(UUID id) {
        log.info("Fetching vehicle rule by ID: {}", id);
        VehicleRuleEntity vehicleRuleEntity = vehicleRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return vehicleRuleMapper.toVehicleRuleResponse(vehicleRuleEntity);
    }

    @Override
    public FullVehicleRuleResponse getFullVehicleRuleById(UUID id) {
        VehicleRuleEntity vehicleRuleEntity = vehicleRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        List<BasingPriceEntity> basingPriceEntities = basingPriceEntityService.findAllByVehicleRuleEntityId(id);

        GetBasingPriceNoVehicleRuleResponse basingPriceResponse = basingPriceEntities.isEmpty()
                ? null
                : basingPriceMapper.toGetBasingPriceNoVehicleRuleResponse(basingPriceEntities.get(0));

        return vehicleRuleMapper.toFullVehicleRuleResponse(vehicleRuleEntity, basingPriceResponse);
    }


    @Override
    public VehicleRuleResponse createVehicleRule(VehicleRuleRequest vehicleRuleRequest) {
        log.info("Creating new vehicle rule");

        if (vehicleRuleRequest == null) {
            log.error("Vehicle rule request cannot be null");
            throw new BadRequestException("Vehicle rule request cannot be null", ErrorEnum.REQUIRED.getErrorCode());
        }

        if (vehicleRuleRequest.categoryId() == null || vehicleRuleRequest.vehicleTypeId() == null) {
            log.error("Category ID and Vehicle Type ID are required for creating a vehicle rule");
            throw new BadRequestException("Category ID and Vehicle Type ID are required",
                    ErrorEnum.REQUIRED.getErrorCode());
        }

        UUID categoryUuid = UUID.fromString(vehicleRuleRequest.categoryId());
        UUID vehicleTypeUuid = UUID.fromString(vehicleRuleRequest.vehicleTypeId());

        VehicleTypeEntity vehicleTypeEntity = vehicleTypeEntityService.findEntityById(vehicleTypeUuid)
                .orElseThrow(() -> {
                    log.error("Vehicle type with ID {} not found", vehicleTypeUuid);
                    return new NotFoundException("Vehicle type not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        Optional<VehicleRuleEntity> existingRule = vehicleRuleEntityService.findByVehicleRuleName(vehicleRuleRequest.vehicleRuleName());
        if (existingRule.isPresent()) {
            log.error("Vehicle rule with name '{}' already exists", vehicleRuleRequest.vehicleRuleName());
            throw new BadRequestException("Vehicle rule with this name already exists",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        try {
            VehicleRuleEnum.valueOf(vehicleRuleRequest.vehicleRuleName());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid vehicle rule type: " + vehicleRuleRequest.vehicleRuleName(),
                    ErrorEnum.ENUM_INVALID.getErrorCode());
        }

        if (!vehicleRuleRequest.vehicleRuleName().equalsIgnoreCase(vehicleTypeEntity.getVehicleTypeName())) {
            log.error("Vehicle rule name '{}' does not match vehicle type name '{}'",
                    vehicleRuleRequest.vehicleRuleName(), vehicleTypeEntity.getVehicleTypeName());
            throw new BadRequestException("Vehicle rule name must match vehicle type name ("
                    + vehicleTypeEntity.getVehicleTypeName() + ")", ErrorEnum.REQUIRED.getErrorCode());
        }

        if (vehicleRuleEntityService.findByCategoryIdAndVehicleTypeEntityIdAndVehicleRuleName(
                categoryUuid, vehicleTypeUuid, vehicleRuleRequest.vehicleRuleName()).isPresent()) {
            log.error("Vehicle rule with category ID {} and vehicle type ID {} already exists", categoryUuid, vehicleTypeUuid);
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        VehicleRuleEntity vehicleRuleEntity = vehicleRuleMapper.mapRequestToEntity(vehicleRuleRequest);
        vehicleRuleEntity.setStatus(CommonStatusEnum.ACTIVE.name());

        VehicleRuleEntity savedEntity = vehicleRuleEntityService.save(vehicleRuleEntity);

        log.info("Vehicle rule created successfully with ID: {}", savedEntity.getId());
        return vehicleRuleMapper.toVehicleRuleResponse(savedEntity);
    }

    @Override
    public VehicleRuleResponse updateVehicleRule(UUID id, UpdateVehicleRuleRequest updateVehicleRuleRequest) {
        log.info("Updating vehicle rule with ID: {}", id);

        VehicleRuleEntity existingEntity = vehicleRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (updateVehicleRuleRequest.vehicleRuleName() != null) {
            try {
                VehicleRuleEnum.valueOf(updateVehicleRuleRequest.vehicleRuleName());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                        "Invalid vehicle rule type: " + updateVehicleRuleRequest.vehicleRuleName(),
                        ErrorEnum.ENUM_INVALID.getErrorCode()
                );
            }
        }

        vehicleRuleMapper.toVehicleRuleEntity(updateVehicleRuleRequest, existingEntity);

        VehicleRuleEntity updatedEntity = vehicleRuleEntityService.save(existingEntity);

        log.info("Vehicle rule updated successfully with ID: {}", updatedEntity.getId());
        return vehicleRuleMapper.toVehicleRuleResponse(updatedEntity);
    }

    @Override
    public void deleteVehicleRule(UUID id) {

    }

    public List<VehicleRuleEntity> suggestVehicles(BigDecimal orderWeight) {
        return vehicleRuleEntityService.findAll().stream()
                // chỉ lấy xe đủ tải
                .filter(vehicle -> vehicle.getMaxWeight().compareTo(orderWeight) >= 0)
                // sort theo độ chênh lệch tải trọng
                .sorted(Comparator.comparing(vehicle -> vehicle.getMaxWeight().subtract(orderWeight)))
                .collect(Collectors.toList());
    }


}
