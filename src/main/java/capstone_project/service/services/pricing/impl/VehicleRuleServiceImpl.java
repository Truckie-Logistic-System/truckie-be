package capstone_project.service.services.pricing.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.VehicleRuleEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.pricing.UpdateVehicleRuleRequest;
import capstone_project.dtos.request.pricing.VehicleRuleRequest;
import capstone_project.dtos.response.pricing.VehicleRuleResponse;
import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.service.entityServices.pricing.VehicleRuleEntityService;
import capstone_project.service.mapper.order.VehicleRuleMapper;
import capstone_project.service.services.pricing.VehicleRuleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleRuleServiceImpl implements VehicleRuleService {

    private final VehicleRuleEntityService vehicleRuleEntityService;
    private final VehicleRuleMapper vehicleRuleMapper;

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
    public VehicleRuleResponse getVehicleRuleById(UUID id) {
        log.info("Fetching vehicle rule by ID: {}", id);
        VehicleRuleEntity vehicleRuleEntity = vehicleRuleEntityService.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return vehicleRuleMapper.toVehicleRuleResponse(vehicleRuleEntity);
    }

    @Override
    public VehicleRuleResponse createVehicleRule(VehicleRuleRequest vehicleRuleRequest) {
        log.info("Creating new vehicle rule");

        if (vehicleRuleRequest.categoryId() == null || vehicleRuleRequest.vehicleTypeId() == null) {
            log.error("Category ID and Vehicle Type ID are required for creating a vehicle rule");
            throw new BadRequestException("Category ID and Vehicle Type ID are required", ErrorEnum.REQUIRED.getErrorCode());
        }

        try {
            VehicleRuleEnum.valueOf(vehicleRuleRequest.vehicleRuleName());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid vehicle rule type: " + vehicleRuleRequest.vehicleRuleName(),
                    ErrorEnum.ENUM_INVALID.getErrorCode());
        }

        UUID categoryUuid = UUID.fromString(vehicleRuleRequest.categoryId());
        UUID vehicleTypeUuid = UUID.fromString(vehicleRuleRequest.vehicleTypeId());

        if (vehicleRuleEntityService.findByCategoryIdAndVehicleTypeEntityIdAndVehicleRuleName(categoryUuid, vehicleTypeUuid, vehicleRuleRequest.vehicleRuleName()).isPresent()) {
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

        if (updateVehicleRuleRequest.categoryId() == null || updateVehicleRuleRequest.vehicleTypeId() == null) {
            log.error("Category ID and Vehicle Type ID are required for updating a vehicle rule");
            throw new BadRequestException("Category ID and Vehicle Type ID are required", ErrorEnum.REQUIRED.getErrorCode());
        }

        VehicleRuleEntity existingEntity = vehicleRuleEntityService.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        try {
            VehicleRuleEnum.valueOf(updateVehicleRuleRequest.vehicleRuleName());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid vehicle rule type: " + updateVehicleRuleRequest.vehicleRuleName(),
                    ErrorEnum.ENUM_INVALID.getErrorCode());
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
