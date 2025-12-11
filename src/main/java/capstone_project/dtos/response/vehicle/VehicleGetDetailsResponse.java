package capstone_project.dtos.response.vehicle;

import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class VehicleGetDetailsResponse {
    private String id;
    private String licensePlateNumber;
    private String model;
    private String manufacturer;
    private Integer year;
    private String status;
    private String vehicleTypeId;
    private String vehicleTypeDescription;
    private BigDecimal currentLatitude;
    private BigDecimal currentLongitude;
    
    // Đăng kiểm (Inspection)
    private LocalDate lastInspectionDate;
    private LocalDate inspectionExpiryDate;
    
    // Bảo hiểm (Insurance)
    private LocalDate insuranceExpiryDate;
    private String insurancePolicyNumber;
    
    // Bảo trì (Maintenance)
    private LocalDate lastMaintenanceDate;
    private LocalDate nextMaintenanceDate;
    
    // Cảnh báo (tính toán từ backend)
    private Boolean isInspectionExpiringSoon;
    private Boolean isInsuranceExpiringSoon;
    private Boolean isMaintenanceDueSoon;
    private Integer daysUntilInspectionExpiry;
    private Integer daysUntilInsuranceExpiry;
    private Integer daysUntilNextMaintenance;
    
    // Related data
    private List<VehicleAssignmentResponse> vehicleAssignmentResponse;
    private List<VehicleServiceRecordResponse> vehicleMaintenanceResponse;
    private VehicleTypeResponse vehicleTypeResponse;
    
    // Enhanced fields
    private List<TopDriverResponse> topDrivers;
    private List<PenaltyHistoryResponse> penalties;
}