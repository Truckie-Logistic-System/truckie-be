package capstone_project.service.services.device.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.device.DeviceRequest;
import capstone_project.dtos.request.device.UpdateDeviceRequest;
import capstone_project.dtos.response.device.DeviceBulkCreateForVehiclesResponse;
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

    @Override
    public DeviceBulkCreateForVehiclesResponse createDefaultDevicesForAllVehicles() {

        // Lấy tất cả xe hiện có
        java.util.List<VehicleEntity> vehicles = vehicleEntityService.findAll();
        long totalVehicles = vehicles.size();

        if (vehicles.isEmpty()) {
            log.warn("[createDefaultDevicesForAllVehicles] No vehicles found in system");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        // Tìm 2 loại thiết bị theo tên
        DeviceTypeEntity cameraType = deviceTypeEntityService.findByDeviceTypeName("Camera hành trình")
                .orElseThrow(() -> new NotFoundException(
                        "Device type 'Camera hành trình' not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        DeviceTypeEntity gpsType = deviceTypeEntityService.findByDeviceTypeName("Thiết bị GPS")
                .orElseThrow(() -> new NotFoundException(
                        "Device type 'Thiết bị GPS' not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        long cameraCreated = 0;
        long gpsCreated = 0;
        long skipped = 0;

        // Danh sách manufacturers và models đa dạng
        String[] cameraManufacturers = {"Hikvision", "Dahua", "VietTrack", "Axis", "Bosch"};
        String[] cameraModels = {"HK-DS-2CD2", "DH-IPC-HF", "VT-CAM-PRO", "AX-P3375", "BS-FLEXIDOME"};
        String[] gpsManufacturers = {"Garmin", "TomTom", "GPS Pro", "GoTrack", "NaviTech"};
        String[] gpsModels = {"GM-Fleet-780", "TT-BRIDGE", "GP-T5000", "GT-VL502", "NV-TR600"};
        
        for (VehicleEntity vehicle : vehicles) {
            UUID vehicleId = vehicle.getId();

            // Sinh IP giả lập nhưng ổn định theo xe (dựa trên hash của ID)
            // Dùng bit mask để tránh case Math.abs(Integer.MIN_VALUE) vẫn âm
            int hash = vehicleId.hashCode() & 0x7fffffff;
            int ipPart2 = (hash % 200) + 10;           // 10-209
            int ipPart3 = (hash / 200 % 200) + 10;     // 10-209
            int ipPart4Camera = (hash / 40000 % 200) + 10;   // 10-209
            int ipPart4Gps = ((hash + 1) / 40000 % 200) + 10; // Khác camera 1 chút
            
            String cameraIp = "10." + ipPart2 + "." + ipPart3 + "." + ipPart4Camera;
            String gpsIp = "10." + ipPart2 + "." + ipPart3 + "." + ipPart4Gps;
            
            // Chọn manufacturer/model đa dạng dựa trên hash
            int manufacturerIndex = hash % cameraManufacturers.length;
            String cameraManufacturer = cameraManufacturers[manufacturerIndex];
            String cameraModel = cameraModels[manufacturerIndex];
            String gpsManufacturer = gpsManufacturers[manufacturerIndex];
            String gpsModel = gpsModels[manufacturerIndex];
            
            // Serial number giả lập
            String cameraSerial = String.format("%06d", (hash % 900000) + 100000);
            String gpsSerial = String.format("%06d", ((hash + 1) % 900000) + 100000);

            // Camera device
            boolean hasCamera = deviceEntityService.findByDeviceTypeAndVehicle(cameraType.getId(), vehicleId).isPresent();
            if (!hasCamera) {
                DeviceEntity cameraDevice = DeviceEntity.builder()
                        .deviceCode("CAM-" + vehicle.getLicensePlateNumber() + "-" + cameraSerial)
                        .manufacturer(cameraManufacturer)
                        .model(cameraModel)
                        .status(CommonStatusEnum.ACTIVE.name())
                        .installedAt(java.time.LocalDateTime.now())
                        .ipAddress(cameraIp)
                        .firmwareVersion("1." + (hash % 5) + "." + (hash % 10))
                        .deviceTypeEntity(cameraType)
                        .vehicleEntity(vehicle)
                        .build();
                deviceEntityService.save(cameraDevice);
                cameraCreated++;
            } else {
                skipped++;
            }

            // GPS device
            boolean hasGps = deviceEntityService.findByDeviceTypeAndVehicle(gpsType.getId(), vehicleId).isPresent();
            if (!hasGps) {
                DeviceEntity gpsDevice = DeviceEntity.builder()
                        .deviceCode("GPS-" + vehicle.getLicensePlateNumber() + "-" + gpsSerial)
                        .manufacturer(gpsManufacturer)
                        .model(gpsModel)
                        .status(CommonStatusEnum.ACTIVE.name())
                        .installedAt(java.time.LocalDateTime.now())
                        .ipAddress(gpsIp)
                        .firmwareVersion("2." + ((hash + 1) % 5) + "." + ((hash + 1) % 10))
                        .deviceTypeEntity(gpsType)
                        .vehicleEntity(vehicle)
                        .build();
                deviceEntityService.save(gpsDevice);
                gpsCreated++;
            } else {
                skipped++;
            }
        }

        return new DeviceBulkCreateForVehiclesResponse(
                totalVehicles,
                totalVehicles,
                cameraCreated,
                gpsCreated,
                skipped
        );
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
        
        // Vehicle is optional - only validate if provided
        if (request.vehicleId() != null && !request.vehicleId().isBlank()) {
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

        // Only check duplicate device type + vehicle if vehicle is provided
        if (request.vehicleId() != null && !request.vehicleId().isBlank()) {
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
}
