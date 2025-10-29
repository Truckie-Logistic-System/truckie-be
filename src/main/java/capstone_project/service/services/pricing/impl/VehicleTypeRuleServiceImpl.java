package capstone_project.service.services.pricing.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.VehicleTypeRuleEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.pricing.UpdateVehicleTypeRuleRequest;
import capstone_project.dtos.request.pricing.VehicleTypeRuleRequest;
import capstone_project.dtos.response.pricing.FullVehicleTypeRuleResponse;
import capstone_project.dtos.response.pricing.GetBasingPriceNoVehicleTypeRuleResponse;
import capstone_project.dtos.response.pricing.VehicleTypeRuleResponse;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.VehicleTypeRuleEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
import capstone_project.repository.entityServices.pricing.VehicleTypeRuleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.order.BasingPriceMapper;
import capstone_project.service.mapper.order.VehicleTypeRuleMapper;
import capstone_project.service.services.pricing.VehicleTypeRuleService;
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
public class VehicleTypeRuleServiceImpl implements VehicleTypeRuleService {

    private final VehicleTypeRuleEntityService vehicleTypeRuleEntityService;
    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final BasingPriceEntityService basingPriceEntityService;
    private final VehicleTypeRuleMapper vehicleTypeRuleMapper;
    private final BasingPriceMapper basingPriceMapper;

    @Override
    public List<VehicleTypeRuleResponse> getAllVehicleTypeRules() {
        log.info("Fetching all vehicle rules");
        List<VehicleTypeRuleEntity> pricingRuleEntities = vehicleTypeRuleEntityService.findAll();
        if (pricingRuleEntities.isEmpty()) {
            log.warn("No vehicle rules found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return pricingRuleEntities.stream()
                .map(vehicleTypeRuleMapper::toVehicleTypeRuleResponse)
                .toList();
    }

    @Override
    public List<FullVehicleTypeRuleResponse> getAllFullVehicleTypeRules() {
        log.info("Fetching all full vehicle rules");
        List<VehicleTypeRuleEntity> vehicleRuleEntities = vehicleTypeRuleEntityService.findAll();
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
                            basingPriceEntityService.findAllByVehicleTypeRuleEntityId(vehicleRuleEntity.getId());

                    List<GetBasingPriceNoVehicleTypeRuleResponse> basingPriceResponses = basingPriceEntities.isEmpty()
                            ? List.of()
                            : basingPriceEntities.stream()
                                .map(basingPriceMapper::toGetBasingPriceNoVehicleTypeRuleResponse)
                                .toList();

                    return vehicleTypeRuleMapper.toFullVehicleTypeRuleResponse(vehicleRuleEntity, basingPriceResponses);
                })
                .toList();
    }


    @Override
    public VehicleTypeRuleResponse getVehicleTypeRuleById(UUID id) {
        log.info("Fetching vehicle rule by ID: {}", id);
        VehicleTypeRuleEntity vehicleTypeRuleEntity = vehicleTypeRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return vehicleTypeRuleMapper.toVehicleTypeRuleResponse(vehicleTypeRuleEntity);
    }

    @Override
    public FullVehicleTypeRuleResponse getFullVehicleTypeRuleById(UUID id) {
        VehicleTypeRuleEntity vehicleTypeRuleEntity = vehicleTypeRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        List<BasingPriceEntity> basingPriceEntities = basingPriceEntityService.findAllByVehicleTypeRuleEntityId(id);

        List<GetBasingPriceNoVehicleTypeRuleResponse> basingPriceResponses = basingPriceEntities.isEmpty()
                ? List.of()
                : basingPriceEntities.stream()
                    .map(basingPriceMapper::toGetBasingPriceNoVehicleTypeRuleResponse)
                    .toList();

        return vehicleTypeRuleMapper.toFullVehicleTypeRuleResponse(vehicleTypeRuleEntity, basingPriceResponses);
    }


    @Override
    public VehicleTypeRuleResponse createVehicleTypeRule(VehicleTypeRuleRequest vehicleTypeRuleRequest) {
        log.info("Creating new vehicle rule");

        if (vehicleTypeRuleRequest == null) {
            log.error("Vehicle rule request cannot be null");
            throw new BadRequestException("Vehicle rule request cannot be null", ErrorEnum.REQUIRED.getErrorCode());
        }

        if (vehicleTypeRuleRequest.categoryId() == null || vehicleTypeRuleRequest.vehicleTypeId() == null) {
            log.error("Category ID and Vehicle Type ID are required for creating a vehicle rule");
            throw new BadRequestException("Category ID and Vehicle Type ID are required",
                    ErrorEnum.REQUIRED.getErrorCode());
        }

        UUID categoryUuid = UUID.fromString(vehicleTypeRuleRequest.categoryId());
        UUID vehicleTypeUuid = UUID.fromString(vehicleTypeRuleRequest.vehicleTypeId());

        VehicleTypeEntity vehicleTypeEntity = vehicleTypeEntityService.findEntityById(vehicleTypeUuid)
                .orElseThrow(() -> {
                    log.error("Vehicle type with ID {} not found", vehicleTypeUuid);
                    return new NotFoundException("Vehicle type not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        Optional<VehicleTypeRuleEntity> existingRule = vehicleTypeRuleEntityService.findByVehicleTypeRuleName(vehicleTypeRuleRequest.vehicleRuleName());
        if (existingRule.isPresent()) {
            log.error("Vehicle rule with name '{}' already exists", vehicleTypeRuleRequest.vehicleRuleName());
            throw new BadRequestException("Vehicle rule with this name already exists",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        try {
            VehicleTypeRuleEnum.valueOf(vehicleTypeRuleRequest.vehicleRuleName());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid vehicle rule type: " + vehicleTypeRuleRequest.vehicleRuleName(),
                    ErrorEnum.ENUM_INVALID.getErrorCode());
        }

        if (!vehicleTypeRuleRequest.vehicleRuleName().equalsIgnoreCase(vehicleTypeEntity.getVehicleTypeName())) {
            log.error("Vehicle rule name '{}' does not match vehicle type name '{}'",
                    vehicleTypeRuleRequest.vehicleRuleName(), vehicleTypeEntity.getVehicleTypeName());
            throw new BadRequestException("Vehicle rule name must match vehicle type name ("
                    + vehicleTypeEntity.getVehicleTypeName() + ")", ErrorEnum.REQUIRED.getErrorCode());
        }

        if (vehicleTypeRuleEntityService.findByCategoryIdAndVehicleTypeEntityIdAndVehicleTypeRuleName(
                categoryUuid, vehicleTypeUuid, vehicleTypeRuleRequest.vehicleRuleName()).isPresent()) {
            log.error("Vehicle rule with category ID {} and vehicle type ID {} already exists", categoryUuid, vehicleTypeUuid);
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        VehicleTypeRuleEntity vehicleTypeRuleEntity = vehicleTypeRuleMapper.mapRequestToEntity(vehicleTypeRuleRequest);
        vehicleTypeRuleEntity.setStatus(CommonStatusEnum.ACTIVE.name());

        VehicleTypeRuleEntity savedEntity = vehicleTypeRuleEntityService.save(vehicleTypeRuleEntity);

        log.info("Vehicle rule created successfully with ID: {}", savedEntity.getId());
        return vehicleTypeRuleMapper.toVehicleTypeRuleResponse(savedEntity);
    }

    @Override
    public VehicleTypeRuleResponse updateVehicleTypeRule(UUID id, UpdateVehicleTypeRuleRequest updateVehicleTypeRuleRequest) {
        log.info("Updating vehicle rule with ID: {}", id);

        VehicleTypeRuleEntity existingEntity = vehicleTypeRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (updateVehicleTypeRuleRequest.vehicleRuleName() != null) {
            try {
                VehicleTypeRuleEnum.valueOf(updateVehicleTypeRuleRequest.vehicleRuleName());
            } catch (IllegalArgumentException e) {
                throw new BadRequestException(
                        "Invalid vehicle rule type: " + updateVehicleTypeRuleRequest.vehicleRuleName(),
                        ErrorEnum.ENUM_INVALID.getErrorCode()
                );
            }
        }

        vehicleTypeRuleMapper.toVehicleTypeRuleEntity(updateVehicleTypeRuleRequest, existingEntity);

        VehicleTypeRuleEntity updatedEntity = vehicleTypeRuleEntityService.save(existingEntity);

        log.info("Vehicle rule updated successfully with ID: {}", updatedEntity.getId());
        return vehicleTypeRuleMapper.toVehicleTypeRuleResponse(updatedEntity);
    }

    @Override
    public void deleteVehicleTypeRule(UUID id) {

    }

    public List<VehicleTypeRuleEntity> suggestVehicles(BigDecimal orderWeight) {
        return vehicleTypeRuleEntityService.findAll().stream()
                // chỉ lấy xe đủ tải
                .filter(vehicle -> vehicle.getMaxWeight().compareTo(orderWeight) >= 0)
                // sort theo độ chênh lệch tải trọng
                .sorted(Comparator.comparing(vehicle -> vehicle.getMaxWeight().subtract(orderWeight)))
                .collect(Collectors.toList());
    }


}
