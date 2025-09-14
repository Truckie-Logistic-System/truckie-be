package capstone_project.service.services.setting.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.setting.UpdateWeightUnitSettingRequest;
import capstone_project.dtos.request.setting.WeightUnitSettingRequest;
import capstone_project.dtos.response.setting.WeightUnitSettingResponse;
import capstone_project.entity.setting.WeightUnitSettingEntity;
import capstone_project.repository.entityServices.setting.WeightUnitSettingEntityService;
import capstone_project.service.mapper.setting.WeightUnitSettingMapper;
import capstone_project.service.services.setting.WeightUnitSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeightUnitSettingServiceImpl implements WeightUnitSettingService {

    private final WeightUnitSettingEntityService weightUnitSettingEntityService;
    private final WeightUnitSettingMapper weightUnitSettingMapper;

    @Override
    public List<WeightUnitSettingResponse> getAllWeightUnitSettings() {
        log.info("Getting all weight unit settings");

        List<WeightUnitSettingEntity> weightUnitSettingResponseList = weightUnitSettingEntityService.findAll();

        if (weightUnitSettingResponseList.isEmpty()) {
            log.info("No weight unit settings found");
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        return weightUnitSettingResponseList.stream()
                .map(weightUnitSettingMapper::toWeightUnitSettingResponse)
                .toList();
    }

    @Override
    public WeightUnitSettingResponse getWeightUnitSettingById(UUID id) {
        log.info("getWeightUnitSettingById");

        if (id == null) {
            log.info("getWeightUnitSettingById: id is null");
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        WeightUnitSettingEntity weightUnitSettingEntity = weightUnitSettingEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.info("Contract setting not found with id: {}", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode());
                });

        return weightUnitSettingMapper.toWeightUnitSettingResponse(weightUnitSettingEntity);
    }

    @Override
    public WeightUnitSettingResponse createContractSetting(WeightUnitSettingRequest weightUnitSettingRequest) {
        log.info("createContractSetting");

        if (weightUnitSettingRequest == null) {
            log.info("createContractSetting: contractSettingRequest is null");
            throw  new NotFoundException(ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        weightUnitSettingEntityService.getByWeightUnit(weightUnitSettingRequest.weightUnit())
                .ifPresent(entity -> {
                    log.warn("Weight unit setting already exists - name: {}", weightUnitSettingRequest.weightUnit());
                    throw new BadRequestException(
                            ErrorEnum.ALREADY_EXISTED.getMessage(),
                            ErrorEnum.ALREADY_EXISTED.getErrorCode()
                    );
                });

        WeightUnitSettingEntity weightUnitSettingEntity = weightUnitSettingMapper.mapRequestToEntity(weightUnitSettingRequest);

        WeightUnitSettingEntity savedEntity = weightUnitSettingEntityService.save(weightUnitSettingEntity);

        return weightUnitSettingMapper.toWeightUnitSettingResponse(savedEntity);
    }

    @Override
    public WeightUnitSettingResponse updateWeightUnitSetting(UUID id, UpdateWeightUnitSettingRequest updateWeightUnitSettingRequest) {
        log.info("updateWeightUnitSetting");

        if (id == null || updateWeightUnitSettingRequest == null) {
            log.info("updateWeightUnitSetting: id or updateWeightUnitSettingRequest is null");
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        WeightUnitSettingEntity existingEntity = weightUnitSettingEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.info("Weight unit setting not found with id: {}", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode());
                });

        if (updateWeightUnitSettingRequest.weightUnit() != null && !updateWeightUnitSettingRequest.weightUnit().isBlank()) {
            weightUnitSettingEntityService.getByWeightUnit(updateWeightUnitSettingRequest.weightUnit())
                    .filter(entity -> !entity.getId().equals(id))
                    .ifPresent(entity -> {
                        log.warn("Weight unit setting already exists - name: {}", updateWeightUnitSettingRequest.weightUnit());
                        throw new BadRequestException(
                                ErrorEnum.ALREADY_EXISTED.getMessage(),
                                ErrorEnum.ALREADY_EXISTED.getErrorCode()
                        );
                    });
        }

        weightUnitSettingMapper.toWeightUnitSettingEntity(updateWeightUnitSettingRequest, existingEntity);

        WeightUnitSettingEntity updatedEntity = weightUnitSettingEntityService.save(existingEntity);

        return weightUnitSettingMapper.toWeightUnitSettingResponse(updatedEntity);
    }

    @Override
    public void deleteWeightUnitSetting(UUID id) {

    }
}
