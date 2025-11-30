package capstone_project.service.services.device.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.device.DeviceRequest;
import capstone_project.dtos.request.device.UpdateDeviceRequest;
import capstone_project.dtos.response.device.DeviceResponse;
import capstone_project.entity.device.DeviceEntity;
import capstone_project.entity.device.DeviceTypeEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.device.DeviceEntityService;
import capstone_project.repository.entityServices.device.DeviceTypeEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
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
    private final VehicleEntityService vehicleEntityService;
    private final DeviceTypeEntityService deviceTypeEntityService;
    private final DeviceMapper deviceMapper;

    @Override
    public List<DeviceResponse> getAllDevices() {
        
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

        checkValidationForCreate(request);

        DeviceEntity deviceEntity = deviceMapper.mapRequestToEntity(request);
        deviceEntity.setStatus(CommonStatusEnum.ACTIVE.name());
        DeviceEntity savedEntity = deviceEntityService.save(deviceEntity);

        return deviceMapper.toDeviceResponse(savedEntity);
    }

    @Override
    public DeviceResponse updateDevice(UUID id, UpdateDeviceRequest request) {

        checkValidationForUpdate(request, id);

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

    private void checkValidationForUpdate(UpdateDeviceRequest request, UUID id) {
        if (request == null) {
            log.warn("Request is null");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Check deviceCode nếu có truyền
        if (request.deviceCode() != null && !request.deviceCode().isBlank()) {
            deviceEntityService.findByDeviceCode(request.deviceCode())
                    .ifPresent(device -> {
                        if (!device.getId().equals(id)) { // exclude chính nó
                            log.warn("Device code already exists - deviceCode: {}", request.deviceCode());
                            throw new BadRequestException(
                                    "Device code already exists",
                                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
                            );
                        }
                    });
        }

        // Check cặp deviceTypeId + vehicleId nếu có truyền
        if (request.deviceTypeId() != null && request.vehicleId() != null
                && !request.deviceTypeId().isBlank() && !request.vehicleId().isBlank()) {
            UUID deviceTypeId = UUID.fromString(request.deviceTypeId());
            UUID vehicleId = UUID.fromString(request.vehicleId());

            deviceEntityService.findByDeviceTypeAndVehicle(deviceTypeId, vehicleId)
                    .ifPresent(device -> {
                        if (!device.getId().equals(id)) {
                            log.warn("Device with the same type and vehicle already exists - deviceTypeId: {}, vehicleId: {}",
                                    request.deviceTypeId(), request.vehicleId());
                            throw new BadRequestException(
                                    "Device with the same type and vehicle already exists",
                                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
                            );
                        }
                    });
        }
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

        VehicleEntity vehicleEntity = vehicleEntityService.findEntityById(vehicleId)
                .orElseThrow(() -> {
                    log.warn("Vehicle not found - vehicleId: {}", request.vehicleId());
                    return new NotFoundException(
                            "Vehicle not found",
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        if (!CommonStatusEnum.ACTIVE.name().equals(vehicleEntity.getStatus())) {
            log.warn("Vehicle is not active - vehicleId: {}, status: {}", request.vehicleId(), vehicleEntity.getStatus());
            throw new BadRequestException(
                    "Vehicle is not active",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        DeviceTypeEntity deviceTypeEntity =  deviceTypeEntityService.findEntityById(deviceTypeId)
                .orElseThrow(() -> {
                    log.warn("Device type not found - deviceTypeId: {}", request.deviceTypeId());
                    return new NotFoundException(
                            "Device type not found",
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        if (deviceTypeEntity.getIsActive() != true) {
            log.warn("Device type is not active - deviceTypeId: {}", request.deviceTypeId());
            throw new BadRequestException(
                    "Device type is not active",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

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
