package capstone_project.service.mapper.vehicle;

import capstone_project.dtos.request.vehicle.UpdateVehicleRequest;
import capstone_project.dtos.request.vehicle.VehicleRequest;
import capstone_project.dtos.response.vehicle.*;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleServiceRecordEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import org.mapstruct.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    /**
     * Map VehicleEntity to VehicleResponse với các field cảnh báo được tính toán
     */
    default VehicleResponse toResponse(VehicleEntity entity) {
        if (entity == null) {
            return null;
        }
        
        LocalDate today = LocalDate.now();
        int warningDays = 30;
        
        // Tính số ngày còn lại và cảnh báo (bao gồm cả quá hạn và sắp hết hạn)
        Integer daysUntilInspectionExpiry = null;
        Boolean isInspectionExpiringSoon = false;
        if (entity.getInspectionExpiryDate() != null) {
            daysUntilInspectionExpiry = (int) ChronoUnit.DAYS.between(today, entity.getInspectionExpiryDate());
            isInspectionExpiringSoon = daysUntilInspectionExpiry < 0 || daysUntilInspectionExpiry <= warningDays;
        }
        
        Integer daysUntilInsuranceExpiry = null;
        Boolean isInsuranceExpiringSoon = false;
        if (entity.getInsuranceExpiryDate() != null) {
            daysUntilInsuranceExpiry = (int) ChronoUnit.DAYS.between(today, entity.getInsuranceExpiryDate());
            isInsuranceExpiringSoon = daysUntilInsuranceExpiry < 0 || daysUntilInsuranceExpiry <= warningDays;
        }
        
        Integer daysUntilNextMaintenance = null;
        Boolean isMaintenanceDueSoon = false;
        if (entity.getNextMaintenanceDate() != null) {
            daysUntilNextMaintenance = (int) ChronoUnit.DAYS.between(today, entity.getNextMaintenanceDate());
            isMaintenanceDueSoon = daysUntilNextMaintenance < 0 || daysUntilNextMaintenance <= warningDays; // 30 ngày như maintenance page
        }
        
        return new VehicleResponse(
                entity.getId() != null ? entity.getId().toString() : null,
                entity.getLicensePlateNumber(),
                entity.getModel(),
                entity.getManufacturer(),
                entity.getYear(),
                entity.getStatus(),
                entity.getVehicleTypeEntity() != null ? entity.getVehicleTypeEntity().getId().toString() : null,
                entity.getVehicleTypeEntity() != null ? entity.getVehicleTypeEntity().getDescription() : null,
                entity.getLastInspectionDate(),
                entity.getInspectionExpiryDate(),
                entity.getInsuranceExpiryDate(),
                entity.getInsurancePolicyNumber(),
                entity.getLastMaintenanceDate(),
                entity.getNextMaintenanceDate(),
                isInspectionExpiringSoon,
                isInsuranceExpiringSoon,
                isMaintenanceDueSoon,
                daysUntilInspectionExpiry,
                daysUntilInsuranceExpiry,
                daysUntilNextMaintenance
        );
    }

    /**
     * Map VehicleEntity to VehicleGetDetailsResponse với các field cảnh báo được tính toán
     */
    default VehicleGetDetailsResponse toVehicleDetailResponse(VehicleEntity entity) {
        if (entity == null) {
            return null;
        }
        
        VehicleGetDetailsResponse response = new VehicleGetDetailsResponse();
        
        // Basic fields
        response.setId(entity.getId() != null ? entity.getId().toString() : null);
        response.setLicensePlateNumber(entity.getLicensePlateNumber());
        response.setModel(entity.getModel());
        response.setManufacturer(entity.getManufacturer());
        response.setYear(entity.getYear());
        response.setStatus(entity.getStatus());
        response.setCurrentLatitude(entity.getCurrentLatitude());
        response.setCurrentLongitude(entity.getCurrentLongitude());
        
        // Vehicle type
        if (entity.getVehicleTypeEntity() != null) {
            response.setVehicleTypeId(entity.getVehicleTypeEntity().getId().toString());
            response.setVehicleTypeDescription(entity.getVehicleTypeEntity().getDescription());
        }
        
        // Maintenance/Inspection fields
        response.setLastInspectionDate(entity.getLastInspectionDate());
        response.setInspectionExpiryDate(entity.getInspectionExpiryDate());
        response.setInsuranceExpiryDate(entity.getInsuranceExpiryDate());
        response.setInsurancePolicyNumber(entity.getInsurancePolicyNumber());
        response.setLastMaintenanceDate(entity.getLastMaintenanceDate());
        response.setNextMaintenanceDate(entity.getNextMaintenanceDate());
        
        // Calculate warning flags using EXACT same logic as MaintenanceAlertBanner
        LocalDate today = LocalDate.now();
        int warningDays = 30; // Same as maintenance page
        
        // Inspection warnings (match MaintenanceAlertBanner logic)
        if (entity.getInspectionExpiryDate() != null) {
            int daysUntilInspectionExpiry = (int) ChronoUnit.DAYS.between(today, entity.getInspectionExpiryDate());
            response.setDaysUntilInspectionExpiry(daysUntilInspectionExpiry);
            response.setIsInspectionExpiringSoon(daysUntilInspectionExpiry < 0 || daysUntilInspectionExpiry <= warningDays);
        }
        
        // Insurance warnings (match MaintenanceAlertBanner logic)
        if (entity.getInsuranceExpiryDate() != null) {
            int daysUntilInsuranceExpiry = (int) ChronoUnit.DAYS.between(today, entity.getInsuranceExpiryDate());
            response.setDaysUntilInsuranceExpiry(daysUntilInsuranceExpiry);
            response.setIsInsuranceExpiringSoon(daysUntilInsuranceExpiry < 0 || daysUntilInsuranceExpiry <= warningDays);
        }
        
        // Maintenance warnings (match MaintenanceAlertBanner logic - 30 days not 14)
        if (entity.getNextMaintenanceDate() != null) {
            int daysUntilNextMaintenance = (int) ChronoUnit.DAYS.between(today, entity.getNextMaintenanceDate());
            response.setDaysUntilNextMaintenance(daysUntilNextMaintenance);
            response.setIsMaintenanceDueSoon(daysUntilNextMaintenance < 0 || daysUntilNextMaintenance <= warningDays); // 30 ngày như maintenance page
        }
        
        return response;
    }

    GetVehicleResponseForBillOfLandingResponse toGetVehicleResponseForBillOfLanding(VehicleEntity entity);

    // Add these methods to handle nested object mapping
    VehicleAssignmentResponse toVehicleAssignmentResponse(VehicleAssignmentEntity entity);
    VehicleServiceRecordResponse toVehicleServiceRecordResponse(VehicleServiceRecordEntity entity);
    VehicleTypeResponse toVehicleTypeResponse(VehicleTypeEntity entity);



    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "modifiedAt", ignore = true)
    @Mapping(source = "vehicleTypeId", target = "vehicleTypeEntity.id")
    VehicleEntity toVehicleEntity(VehicleRequest request);



    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
//    @Mapping(source = "vehicleTypeId", target = "vehicleTypeEntity.id")
    @Mapping(source = "currentLatitude",  target = "currentLatitude")
    @Mapping(source = "currentLongitude", target = "currentLongitude")
    void toVehicleEntity(UpdateVehicleRequest req,
                         @MappingTarget VehicleEntity entity);
}