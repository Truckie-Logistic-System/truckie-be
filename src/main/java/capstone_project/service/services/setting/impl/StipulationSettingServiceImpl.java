package capstone_project.service.services.setting.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.setting.StipulationSettingRequest;
import capstone_project.dtos.response.setting.StipulationSettingResponse;
import capstone_project.entity.setting.StipulationSettingEntity;
import capstone_project.repository.entityServices.setting.StipulationSettingEntityService;
import capstone_project.service.mapper.setting.StipulationSettingMapper;
import capstone_project.service.services.setting.StipulationSettingService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class StipulationSettingServiceImpl implements StipulationSettingService {

    private final StipulationSettingEntityService stipulationSettingEntityService;
    private final StipulationSettingMapper stipulationSettingMapper;

    @Override
    public StipulationSettingResponse getAllStipulationSettings() {

        StipulationSettingEntity entity = stipulationSettingEntityService.findTopByOrderByIdAsc()
                .orElseThrow(() -> {
                    
                    return new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
                });

        return stipulationSettingMapper.toStipulationSettingResponse(entity);
    }

    @Override
    public StipulationSettingResponse getStipulationSettingById(UUID id) {

        if (id == null) {
            
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        StipulationSettingEntity entity = stipulationSettingEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    
                    return new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
                });

        return stipulationSettingMapper.toStipulationSettingResponse(entity);
    }

    @Override
    @Transactional
    public StipulationSettingResponse createOrUpdateStipulationSettings(StipulationSettingRequest request) {

        if (request == null) {
            throw new NotFoundException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        Optional<StipulationSettingEntity> oldOpt =
                stipulationSettingEntityService.findTopByOrderByIdAsc();

        StipulationSettingEntity newEntity = stipulationSettingMapper.mapRequestToEntity(request);

        StipulationSettingEntity saved = stipulationSettingEntityService.save(newEntity);

        oldOpt.ifPresent(old -> stipulationSettingEntityService.deleteById(old.getId()));

        return stipulationSettingMapper.toStipulationSettingResponse(saved);
    }

    @Override
    public void deleteStipulationSetting(UUID id) {

        if (id == null) {
            
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
