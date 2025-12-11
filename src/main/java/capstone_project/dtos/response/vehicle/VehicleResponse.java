package capstone_project.dtos.response.vehicle;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VehicleResponse(
        String id,
        String licensePlateNumber,
        String model,
        String manufacturer,
        Integer year,
        String status,
        String vehicleTypeId,
        String vehicleTypeDescription,
        // Đăng kiểm
        LocalDate lastInspectionDate,
        LocalDate inspectionExpiryDate,
        // Bảo hiểm
        LocalDate insuranceExpiryDate,
        String insurancePolicyNumber,
        // Bảo trì
        LocalDate lastMaintenanceDate,
        LocalDate nextMaintenanceDate,
        // Cảnh báo (tính toán từ ngày hết hạn)
        Boolean isInspectionExpiringSoon,
        Boolean isInsuranceExpiringSoon,
        Boolean isMaintenanceDueSoon,
        Integer daysUntilInspectionExpiry,
        Integer daysUntilInsuranceExpiry,
        Integer daysUntilNextMaintenance
) {}