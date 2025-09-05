package capstone_project.service.services.device.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.device.DeviceRequest;
import capstone_project.dtos.request.device.UpdateDeviceRequest;
import capstone_project.dtos.response.device.DeviceResponse;
import capstone_project.entity.device.DeviceEntity;
import capstone_project.repository.entityServices.device.DeviceEntityService;
import capstone_project.service.mapper.device.DeviceMapper;
import capstone_project.service.services.device.DeviceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceServiceImpl implements DeviceService {

    private final DeviceEntityService deviceEntityService;
    private final DeviceMapper deviceMapper;

    @Override
    public List<DeviceResponse> getAllDevices() {
        log.info("getAllDevices()");
        List<DeviceEntity> deviceEntities = deviceEntityService.findAll();

        if (deviceEntities.isEmpty()) {
            log.warn("No devices found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return deviceEntities.stream()
                .map(deviceMapper::toDeviceResponse)
                .toList();
    }

    @Override
    public DeviceResponse getDeviceById(UUID id) {
        log.info("getDeviceById() - id: {}", id);

        if (id == null) {
            log.warn("ID is null");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        DeviceEntity deviceEntity = deviceEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.warn("Device not found - id: {}", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        return deviceMapper.toDeviceResponse(deviceEntity);
    }

    @Override
    public DeviceResponse getDeviceByDeviceCode(String deviceCode) {
        log.info("getDeviceByDeviceCode() - deviceCode: {}", deviceCode);
        if (deviceCode == null || deviceCode.isBlank()) {
            log.warn("Device code is null or blank");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        DeviceEntity deviceEntity = deviceEntityService.findByDeviceCode(deviceCode)
                .orElseThrow(() -> {
                    log.warn("Device not found - deviceCode: {}", deviceCode);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        return deviceMapper.toDeviceResponse(deviceEntity);
    }

    @Override
    public DeviceResponse createDevice(DeviceRequest request) {
        log.info("createDevice() - request: {}", request);

        checkValidationForCreate(request);

        DeviceEntity deviceEntity = deviceMapper.mapRequestToEntity(request);
        deviceEntity.setStatus(CommonStatusEnum.ACTIVE.name());
        DeviceEntity savedEntity = deviceEntityService.save(deviceEntity);

        return deviceMapper.toDeviceResponse(savedEntity);
    }

    @Override
    public DeviceResponse updateDevice(UUID id, UpdateDeviceRequest request) {
        log.info("updateDevice() - id: {}, request: {}", id, request);

        checkValidationForUpdate(request);

        DeviceEntity existingDevice = deviceEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.warn("Device not found - id: {}", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        deviceMapper.toDeviceEntity(request, existingDevice);
        DeviceEntity updatedEntity = deviceEntityService.save(existingDevice);

        return deviceMapper.toDeviceResponse(updatedEntity);

    }

    private void checkValidationForUpdate(UpdateDeviceRequest request) {
        if (request == null) {
            log.warn("Request is null");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        deviceEntityService.findByDeviceCode(request.deviceCode())
                .ifPresent(device -> {
                    log.warn("Device code already exists - deviceCode: {}", request.deviceCode());
                    throw new BadRequestException(
                            "Device code already exists",
                            ErrorEnum.ALREADY_EXISTED.getErrorCode()
                    );
                });

        UUID deviceTypeId = UUID.fromString(request.deviceTypeId());
        UUID vehicleId = UUID.fromString(request.vehicleId());

        deviceEntityService.findByDeviceTypeAndVehicle(
                        deviceTypeId,
                        vehicleId
                )
                .ifPresent(device -> {
                    log.warn("Device with the same type and vehicle already exists - deviceTypeId: {}, vehicleId: {}",
                            request.deviceTypeId(), request.vehicleId());
                    throw new BadRequestException(
                            "Device with the same type and vehicle already exists",
                            ErrorEnum.ALREADY_EXISTED.getErrorCode()
                    );
                });
    }

    private void checkValidationForCreate(DeviceRequest request) {
        if (request == null) {
            log.warn("Request is null");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        deviceEntityService.findByDeviceCode(request.deviceCode())
                .ifPresent(
                        device -> {
                            log.warn("Device code already exists - deviceCode: {}", request.deviceCode());
                            throw new BadRequestException(
                                    "Device code already exists",
                                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
                            );
                        }
                );

        UUID deviceTypeId = UUID.fromString(request.deviceTypeId());
        UUID vehicleId = UUID.fromString(request.vehicleId());

        deviceEntityService.findByDeviceTypeAndVehicle(
                        deviceTypeId,
                        vehicleId
                )
                .ifPresent(device -> {
                    log.warn("Device with the same type and vehicle already exists - deviceTypeId: {}, vehicleId: {}",
                            request.deviceTypeId(), request.vehicleId());
                    throw new BadRequestException(
                            "Device with the same type and vehicle already exists",
                            ErrorEnum.ALREADY_EXISTED.getErrorCode()
                    );
                });
    }
}
