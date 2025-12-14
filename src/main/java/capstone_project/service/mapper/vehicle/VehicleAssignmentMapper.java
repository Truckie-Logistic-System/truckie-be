package capstone_project.service.mapper.vehicle;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.vehicle.GetVehicleAssignmentForBillOfLandingResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@Mapper(componentModel = "spring")
public abstract class VehicleAssignmentMapper {

    @Autowired protected VehicleEntityService  vehicleService;
    @Autowired protected DriverEntityService   driverService;

    @Mapping(target = "vehicleEntity", source = "vehicleId", qualifiedByName = "vehicleFromId")
    @Mapping(target = "driver1", source = "driverId_1", qualifiedByName = "driverFromId")
    @Mapping(target = "driver2", source = "driverId_2", qualifiedByName = "driverFromId")
    @Mapping(target = "status", constant = "ACTIVE")  // Tự động set status là ACTIVE
    @Mapping(target = "vehicleAssignmentDevices", ignore = true)  // Don't map devices when creating entity
    @Mapping(target = "id", ignore = true)  // Auto-generated
    @Mapping(target = "createdAt", ignore = true)  // Auto-generated
    @Mapping(target = "modifiedAt", ignore = true)  // Auto-generated
    @Mapping(target = "createdBy", ignore = true)  // Auto-generated
    @Mapping(target = "modifiedBy", ignore = true)  // Auto-generated
    @Mapping(target = "isDemoData", ignore = true)  // Auto-generated
    @Mapping(target = "trackingCode", ignore = true)  // Auto-generated
    public abstract VehicleAssignmentEntity toEntity(VehicleAssignmentRequest req);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "vehicleEntity", source = "vehicleId", qualifiedByName = "vehicleFromId")
    @Mapping(target = "driver2", source = "driverId", qualifiedByName = "driverFromId")
    @Mapping(target = "driver1", ignore = true)  // Don't update driver1 in update method
    @Mapping(target = "vehicleAssignmentDevices", ignore = true)  // Don't update devices in update method
    @Mapping(target = "id", ignore = true)  // Auto-generated
    @Mapping(target = "createdAt", ignore = true)  // Auto-generated
    @Mapping(target = "modifiedAt", ignore = true)  // Auto-generated
    @Mapping(target = "createdBy", ignore = true)  // Auto-generated
    @Mapping(target = "modifiedBy", ignore = true)  // Auto-generated
    @Mapping(target = "isDemoData", ignore = true)  // Auto-generated
    @Mapping(target = "trackingCode", ignore = true)  // Auto-generated
    public abstract void toEntity(UpdateVehicleAssignmentRequest req,
                                  @MappingTarget VehicleAssignmentEntity entity);

    @Mapping(target = "id", source = "id")
    @Mapping(target = "trackingCode", source = "trackingCode")
    @Mapping(target = "vehicleId", source = "vehicleEntity.id")
    @Mapping(target = "driver_id_1",  source = "driver1.id")
    @Mapping(target = "driver_id_2",  source = "driver2.id")
    @Mapping(target = "vehicle", source = "vehicleEntity", qualifiedByName = "mapVehicleInfo")
    @Mapping(target = "driver1", source = "driver1", qualifiedByName = "mapDriverInfo")
    @Mapping(target = "driver2", source = "driver2", qualifiedByName = "mapDriverInfo")
    @Mapping(target = "devices", source = "devices", qualifiedByName = "mapDevices")
    public abstract VehicleAssignmentResponse toResponse(VehicleAssignmentEntity entity);

    public abstract GetVehicleAssignmentForBillOfLandingResponse toGetVehicleAssignmentForBillOfLandingResponse(VehicleAssignmentEntity entity);

    /* helpers */
    @Named("vehicleFromId")
    protected VehicleEntity vehicleFromId(String id){
        if(id==null) return null;
        return vehicleService.findEntityById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
    }
    @Named("driverFromId")
    protected DriverEntity driverFromId(String id){
        if(id==null) return null;
        return driverService.findEntityById(UUID.fromString(id))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
    }
    
    @Named("mapVehicleInfo")
    protected VehicleAssignmentResponse.VehicleInfo mapVehicleInfo(VehicleEntity vehicle) {
        if (vehicle == null) return null;
        
        var vehicleType = vehicle.getVehicleTypeEntity() != null
            ? new VehicleAssignmentResponse.VehicleTypeInfo(
                vehicle.getVehicleTypeEntity().getId(),
                vehicle.getVehicleTypeEntity().getVehicleTypeName(),
                vehicle.getVehicleTypeEntity().getDescription()
            )
            : null;
        
        return new VehicleAssignmentResponse.VehicleInfo(
            vehicle.getId(),
            vehicle.getLicensePlateNumber(),
            vehicle.getModel(),
            vehicle.getManufacturer(),
            vehicle.getYear(),
            vehicleType
        );
    }
    
    @Named("mapDriverInfo")
    protected VehicleAssignmentResponse.DriverInfo mapDriverInfo(DriverEntity driver) {
        if (driver == null) return null;
        
        // Calculate experience years from dateOfPassing if available
        String experienceYears = null;
        if (driver.getDateOfPassing() != null) {
            long years = java.time.temporal.ChronoUnit.YEARS.between(
                driver.getDateOfPassing(), 
                java.time.LocalDateTime.now()
            );
            experienceYears = String.valueOf(years);
        }
        
        return new VehicleAssignmentResponse.DriverInfo(
            driver.getId(),
            driver.getUser() != null ? driver.getUser().getFullName() : null,
            driver.getUser() != null ? driver.getUser().getPhoneNumber() : null,
            driver.getDriverLicenseNumber(),
            driver.getLicenseClass(),
            experienceYears
        );
    }
    
    @Named("mapDevices")
    protected java.util.List<VehicleAssignmentResponse.DeviceInfo> mapDevices(java.util.Set<capstone_project.entity.device.DeviceEntity> devices) {
        if (devices == null || devices.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        
        return devices.stream()
            .map(this::mapSingleDevice)
            .collect(java.util.stream.Collectors.toList());
    }
    
    private VehicleAssignmentResponse.DeviceInfo mapSingleDevice(capstone_project.entity.device.DeviceEntity device) {
        if (device == null) return null;
        
        var deviceType = device.getDeviceTypeEntity() != null
            ? new VehicleAssignmentResponse.DeviceTypeInfo(
                device.getDeviceTypeEntity().getId(),
                device.getDeviceTypeEntity().getDeviceTypeName(),
                device.getDeviceTypeEntity().getDescription()
            )
            : null;
        
        return new VehicleAssignmentResponse.DeviceInfo(
            device.getId(),
            device.getDeviceCode(),
            device.getManufacturer(),
            device.getModel(),
            device.getStatus(),
            device.getIpAddress(),
            device.getFirmwareVersion(),
            deviceType
        );
    }
}