package capstone_project.service.services.setting.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.setting.StipulationSettingRequest;
import capstone_project.dtos.response.setting.StipulationSettingResponse;
import capstone_project.entity.setting.StipulationSettingEntity;
import capstone_project.repository.entityServices.setting.StipulationSettingEntityService;
import capstone_project.service.mapper.setting.StipulationSettingMapper;
import capstone_project.service.services.setting.StipulationSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StipulationSettingServiceImpl implements StipulationSettingService {

    private final StipulationSettingEntityService stipulationSettingEntityService;
    private final StipulationSettingMapper stipulationSettingMapper;

    @Override
    public StipulationSettingResponse getAllStipulationSettings() {
        log.info("StipulationSettingServiceImpl.getAllStipulationSettings()");

        StipulationSettingEntity entity = stipulationSettingEntityService.findTopByOrderByIdAsc()
                .orElseThrow(() -> {
                    log.info("No stipulation settings found");
                    return new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
                });

        return stipulationSettingMapper.toStipulationSettingResponse(entity);
    }

    @Override
    public StipulationSettingResponse getStipulationSettingById(UUID id) {
        log.info("StipulationSettingServiceImpl.getStipulationSettingById: {}", id);

        if (id == null) {
            log.info("getStipulationSettingById: id is null");
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        StipulationSettingEntity entity = stipulationSettingEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.info("Stipulation setting not found with id: {}", id);
                    return new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
                });

        return stipulationSettingMapper.toStipulationSettingResponse(entity);
    }

    @Override
    public StipulationSettingResponse createOrUpdateStipulationSettings(StipulationSettingRequest request) {
        log.info("StipulationSettingServiceImpl.saveOrUpdateStipulationSettings: {}", request);

        if (request == null) {
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        StipulationSettingEntity entity = stipulationSettingEntityService
                .findTopByOrderByIdAsc()
                .orElse(null);

        if (entity == null) {
            entity = stipulationSettingMapper.mapRequestToEntity(request);
        } else {
            if (entity.getContents() == null) {
                entity.setContents(new HashMap<>());
            }
            if (request.contents() != null) {
                entity.getContents().putAll(request.contents());
            }
        }

        StipulationSettingEntity savedEntity = stipulationSettingEntityService.save(entity);
        return stipulationSettingMapper.toStipulationSettingResponse(savedEntity);
    }

    @Override
    public void deleteStipulationSetting(UUID id) {
        log.info("StipulationSettingServiceImpl.deleteStipulationSetting: {}", id);

        if (id == null) {
            log.info("deleteStipulationSetting: id is null");
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        StipulationSettingEntity existingEntity = stipulationSettingEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        stipulationSettingEntityService.deleteById(existingEntity.getId());
    }

}
