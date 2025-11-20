package capstone_project.service.services.setting.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.setting.CarrierSettingRequest;
import capstone_project.dtos.response.setting.CarrierSettingResponse;
import capstone_project.entity.setting.CarrierSettingEntity;
import capstone_project.repository.entityServices.setting.CarrierSettingEntityService;
import capstone_project.service.mapper.setting.CarrierSettingMapper;
import capstone_project.service.services.setting.CarrierSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CarrierSettingServiceImpl implements CarrierSettingService {

    private final CarrierSettingEntityService entityService;
    private final CarrierSettingMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CarrierSettingResponse> findAll() {
        
        List<CarrierSettingEntity> entities = entityService.findAll();
        if (entities.isEmpty()) {
            log.warn("No carrier settings found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return entities.stream()
                .map(mapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CarrierSettingResponse findById(Long id) {
        
        if (id == null) {
            log.warn("Provided id is null");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        CarrierSettingEntity entity = entityService.findById(id)
                .orElseThrow(() -> {
                    log.warn("CarrierSetting not found - id: {}", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public CarrierSettingResponse create(CarrierSettingRequest request) {
        
        if (request == null) {
            log.warn("Create request is null");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        CarrierSettingEntity entity = mapper.toEntity(request);
        CarrierSettingEntity saved = entityService.save(entity);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public CarrierSettingResponse update(Long id, CarrierSettingRequest request) {
        
        if (id == null || request == null) {
            log.warn("Update id or request is null - id: {}, request: {}", id, request);
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        CarrierSettingEntity existing = entityService.findById(id)
                .orElseThrow(() -> {
                    log.warn("CarrierSetting not found - id: {}", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        mapper.updateEntityFromRequest(request, existing);
        CarrierSettingEntity saved = entityService.save(existing);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        
        if (id == null) {
            log.warn("Delete id is null");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        if (!entityService.existsById(id)) {
            log.warn("CarrierSetting not found for delete - id: {}", id);
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        entityService.deleteById(id);
    }
}