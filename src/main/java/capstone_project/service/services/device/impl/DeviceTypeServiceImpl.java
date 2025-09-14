package capstone_project.service.services.device.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.device.DeviceTypeRequest;
import capstone_project.dtos.request.device.UpdateDeviceTypeRequest;
import capstone_project.dtos.response.device.DeviceTypeResponse;
import capstone_project.entity.device.DeviceTypeEntity;
import capstone_project.repository.entityServices.device.DeviceTypeEntityService;
import capstone_project.service.mapper.device.DeviceTypeMapper;
import capstone_project.service.services.device.DeviceTypeService;
import capstone_project.service.services.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceTypeServiceImpl implements DeviceTypeService {

    private final DeviceTypeEntityService deviceTypeEntityService;
    private final DeviceTypeMapper deviceTypeMapper;
    private final RedisService redisService;

    private static final String DEVICE_TYPE_ALL_CACHE_KEY = "device-types:all";
    private static final String DEVICE_TYPE_ALL_BY_NAME_CACHE_KEY = "device-types:all:name:";
    private static final String DEVICE_TYPE_BY_ID_CACHE_KEY_PREFIX = "device-type:";
    private static final String DEVICE_TYPE_BY_NAME_CACHE_KEY_PREFIX = "device-type:name:";


    @Override
    public List<DeviceTypeResponse> getAllDeviceTypes() {
        log.info("getAllDeviceTypes()");

        List<DeviceTypeEntity> cachedDeviceTypes = redisService.getList(DEVICE_TYPE_ALL_CACHE_KEY, DeviceTypeEntity.class);
        if (cachedDeviceTypes != null && !cachedDeviceTypes.isEmpty()) {
            log.info("Returning {} device types from cache", cachedDeviceTypes.size());
            return cachedDeviceTypes.stream()
                    .map(deviceTypeMapper::toDeviceTypeResponse)
                    .toList();
        }

        List<DeviceTypeEntity> deviceTypeEntities = deviceTypeEntityService.findAll();
        if (deviceTypeEntities.isEmpty()) {
            log.warn("No device types found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        redisService.save(DEVICE_TYPE_ALL_CACHE_KEY, deviceTypeEntities);

        return deviceTypeEntities.stream()
                .map(deviceTypeMapper::toDeviceTypeResponse)
                .toList();
    }

    @Override
    public List<DeviceTypeResponse> getListDeviceTypesByNameLike(String name) {
        log.info("getListDeviceTypesByNameLike() - name: {}", name);

        if (name == null || name.isBlank()) {
            log.warn("Device type name is null or blank");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        String cacheKey = DEVICE_TYPE_ALL_BY_NAME_CACHE_KEY + name;
        List<DeviceTypeEntity> cachedDeviceTypes = redisService.getList(cacheKey, DeviceTypeEntity.class);

        if (cachedDeviceTypes != null && !cachedDeviceTypes.isEmpty()) {
            log.info("Returning {} device types from cache for name like: {}", cachedDeviceTypes.size(), name);
            return cachedDeviceTypes.stream()
                    .map(deviceTypeMapper::toDeviceTypeResponse)
                    .toList();
        }

        List<DeviceTypeEntity> deviceTypeEntities = deviceTypeEntityService.getAllDeviceTypesByNameLike(name);

        if (deviceTypeEntities.isEmpty()) {
            log.warn("No device types found with name like: {}", name);
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        redisService.save(cacheKey, deviceTypeEntities);

        return deviceTypeEntities.stream()
                .map(deviceTypeMapper::toDeviceTypeResponse)
                .toList();
    }

    @Override
    public DeviceTypeResponse getDeviceTypeById(UUID id) {
        log.info("getDeviceTypeById() - id: {}", id);

        if (id == null) {
            log.warn("Device type ID is null");
            throw new BadRequestException(
                    ErrorEnum.REQUIRED.getMessage(),
                    ErrorEnum.REQUIRED.getErrorCode()
            );
        }

        String cacheKey = DEVICE_TYPE_BY_ID_CACHE_KEY_PREFIX + id;
        DeviceTypeEntity cachedEntity = redisService.get(cacheKey, DeviceTypeEntity.class);
        if (cachedEntity != null) {
            log.info("Returning cached device type for ID: {}", id);
            return deviceTypeMapper.toDeviceTypeResponse(cachedEntity);
        }

        DeviceTypeEntity deviceTypeEntity = deviceTypeEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.warn("Device type not found - id: {}", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        redisService.save(cacheKey, deviceTypeEntity);

        return deviceTypeMapper.toDeviceTypeResponse(deviceTypeEntity);
    }

    @Override
    public DeviceTypeResponse createDeviceType(DeviceTypeRequest request) {
        log.info("createDeviceType() - request: {}", request);

        checkValidationForCreate(request);

        DeviceTypeEntity deviceTypeEntity = deviceTypeMapper.mapRequestToEntity(request);
        deviceTypeEntity.setIsActive(Boolean.TRUE);

        DeviceTypeEntity savedEntity = deviceTypeEntityService.save(deviceTypeEntity);

        redisService.delete(DEVICE_TYPE_ALL_CACHE_KEY);

        return deviceTypeMapper.toDeviceTypeResponse(savedEntity);
    }

    @Override
    public DeviceTypeResponse updateDeviceType(UUID id, UpdateDeviceTypeRequest request) {
        log.info("updateDeviceType() - id: {}", id);

        checkValidationForUpdate(id, request);

        DeviceTypeEntity existingEntity = deviceTypeEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.warn("Device type not found - id: {}", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        deviceTypeMapper.toDeviceTypeEntity(request, existingEntity);
        DeviceTypeEntity updatedEntity = deviceTypeEntityService.save(existingEntity);

        redisService.delete(DEVICE_TYPE_ALL_CACHE_KEY);
        redisService.delete(DEVICE_TYPE_BY_ID_CACHE_KEY_PREFIX + id);

        return deviceTypeMapper.toDeviceTypeResponse(updatedEntity);
    }

    @Override
    public void deleteDeviceType(UUID id) {

    }

    private void checkValidationForCreate(DeviceTypeRequest request) {
        if (request == null) {
            log.warn("Create device type request is null");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        if (request.deviceTypeName() == null || request.deviceTypeName().isBlank()) {
            log.warn("Device type name is null or blank");
            throw new BadRequestException(
                    ErrorEnum.REQUIRED.getMessage(),
                    ErrorEnum.REQUIRED.getErrorCode()
            );
        }

        deviceTypeEntityService.findByDeviceTypeName(request.deviceTypeName())
                .ifPresent(entity -> {
                    log.warn("Device type already exists - name: {}", request.deviceTypeName());
                    throw new BadRequestException(
                            ErrorEnum.ALREADY_EXISTED.getMessage(),
                            ErrorEnum.ALREADY_EXISTED.getErrorCode()
                    );
                });
    }

    private void checkValidationForUpdate(UUID id, UpdateDeviceTypeRequest request) {
        if (request == null) {
            log.warn("Update device type request is null");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        deviceTypeEntityService.findByDeviceTypeName(request.deviceTypeName())
                .filter(entity -> !entity.getId().equals(id))
                .ifPresent(entity -> {
                    log.warn("Device type already exists - name: {}", request.deviceTypeName());
                    throw new BadRequestException(
                            ErrorEnum.ALREADY_EXISTED.getMessage(),
                            ErrorEnum.ALREADY_EXISTED.getErrorCode()
                    );
                });
    }
}
