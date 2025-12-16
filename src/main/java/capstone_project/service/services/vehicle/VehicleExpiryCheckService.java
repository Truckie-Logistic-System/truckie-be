package capstone_project.service.services.vehicle;

import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.dtos.response.vehicle.VehicleServiceRecordResponse;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.repositories.vehicle.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

/**
 * Background service để kiểm tra và cập nhật trạng thái xe
 * dựa trên ngày hết hạn đăng kiểm, bảo hiểm, bảo trì
 * 
 * Logic ưu tiên status:
 * 1. INSPECTION_EXPIRED (cao nhất - không được phân công)
 * 2. INSURANCE_EXPIRED (không được phân công)
 * 3. INSPECTION_DUE (cảnh báo - vẫn được phân công)
 * 4. INSURANCE_DUE (cảnh báo - vẫn được phân công)
 * 5. MAINTENANCE_DUE (cảnh báo - vẫn được phân công)
 * 6. ACTIVE (bình thường)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleExpiryCheckService {

    private final VehicleRepository vehicleRepository;
    private final VehicleServiceRecordService vehicleServiceRecordService;

    /**
     * Số ngày trước khi hết hạn để cảnh báo (mặc định 30 ngày)
     */
    @Value("${vehicle.expiry.warning-days:30}")
    private int warningDays;

    /**
     * Các status không nên thay đổi tự động (xe đang bận hoặc có vấn đề)
     */
    private static final Set<String> PROTECTED_STATUSES = Set.of(
            VehicleStatusEnum.IN_TRANSIT.name(),
            VehicleStatusEnum.MAINTENANCE.name(),
            VehicleStatusEnum.BREAKDOWN.name(),
            VehicleStatusEnum.ACCIDENT.name(),
            VehicleStatusEnum.INACTIVE.name()
    );

    /**
     * Chạy mỗi ngày lúc 1:00 AM để kiểm tra và cập nhật trạng thái xe
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void checkVehicleExpiry() {
        // Tạo lịch bảo trì tự động cho xe đã quá hạn bảo trì
        createOverdueMaintenanceRecords();
        
        log.info("🚗 [VehicleExpiryCheckService] Bắt đầu kiểm tra hạn đăng kiểm/bảo hiểm/bảo trì xe...");
        
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(warningDays);
        
        int expiredInspectionCount = 0;
        int expiredInsuranceCount = 0;
        int dueInspectionCount = 0;
        int dueInsuranceCount = 0;
        int dueMaintenanceCount = 0;
        int restoredCount = 0;

        List<VehicleEntity> allVehicles = vehicleRepository.findAll();
        
        for (VehicleEntity vehicle : allVehicles) {
            // Bỏ qua xe có status được bảo vệ
            if (PROTECTED_STATUSES.contains(vehicle.getStatus())) {
                continue;
            }
            
            String newStatus = determineVehicleStatus(vehicle, today, warningDate);
            String currentStatus = vehicle.getStatus();
            
            if (!newStatus.equals(currentStatus)) {
                vehicle.setStatus(newStatus);
                vehicleRepository.save(vehicle);
                
                // Log và đếm theo loại thay đổi
                switch (newStatus) {
                    case "INSPECTION_EXPIRED":
                        log.warn("⚠️ Xe {} ({}) đã HẾT HẠN đăng kiểm ngày {}",
                                vehicle.getLicensePlateNumber(), vehicle.getId(), vehicle.getInspectionExpiryDate());
                        expiredInspectionCount++;
                        break;
                    case "INSURANCE_EXPIRED":
                        log.warn("⚠️ Xe {} ({}) đã HẾT HẠN bảo hiểm ngày {}",
                                vehicle.getLicensePlateNumber(), vehicle.getId(), vehicle.getInsuranceExpiryDate());
                        expiredInsuranceCount++;
                        break;
                    case "INSPECTION_DUE":
                        log.info("📢 Xe {} ({}) sẽ hết hạn đăng kiểm ngày {} (còn {} ngày)",
                                vehicle.getLicensePlateNumber(), vehicle.getId(), 
                                vehicle.getInspectionExpiryDate(),
                                ChronoUnit.DAYS.between(today, vehicle.getInspectionExpiryDate()));
                        dueInspectionCount++;
                        break;
                    case "INSURANCE_DUE":
                        log.info("📢 Xe {} ({}) sẽ hết hạn bảo hiểm ngày {} (còn {} ngày)",
                                vehicle.getLicensePlateNumber(), vehicle.getId(), 
                                vehicle.getInsuranceExpiryDate(),
                                ChronoUnit.DAYS.between(today, vehicle.getInsuranceExpiryDate()));
                        dueInsuranceCount++;
                        break;
                    case "MAINTENANCE_DUE":
                        log.info("📢 Xe {} ({}) sẽ đến hạn bảo dưỡng ngày {} (còn {} ngày)",
                                vehicle.getLicensePlateNumber(), vehicle.getId(), 
                                vehicle.getNextMaintenanceDate(),
                                ChronoUnit.DAYS.between(today, vehicle.getNextMaintenanceDate()));
                        dueMaintenanceCount++;
                        break;
                    case "ACTIVE":
                        log.info("✅ Xe {} ({}) đã được gia hạn. Khôi phục status về ACTIVE",
                                vehicle.getLicensePlateNumber(), vehicle.getId());
                        restoredCount++;
                        break;
                }
            }
        }

        log.info("🚗 [VehicleExpiryCheckService] Hoàn thành kiểm tra. " +
                "Hết hạn đăng kiểm: {}, Hết hạn bảo hiểm: {}, " +
                "Sắp hết hạn đăng kiểm: {}, Sắp hết hạn bảo hiểm: {}, Sắp hết hạn bảo dưỡng: {}, " +
                "Đã khôi phục: {}",
                expiredInspectionCount, expiredInsuranceCount,
                dueInspectionCount, dueInsuranceCount, dueMaintenanceCount,
                restoredCount);
    }

    /**
     * Xác định status phù hợp cho xe dựa trên các ngày hết hạn
     * Ưu tiên: EXPIRED > DUE > ACTIVE
     */
    private String determineVehicleStatus(VehicleEntity vehicle, LocalDate today, LocalDate warningDate) {
        // 1. Kiểm tra hết hạn đăng kiểm (ưu tiên cao nhất)
        if (vehicle.getInspectionExpiryDate() != null && 
            vehicle.getInspectionExpiryDate().isBefore(today)) {
            return VehicleStatusEnum.INSPECTION_EXPIRED.name();
        }
        
        // 2. Kiểm tra hết hạn bảo hiểm
        if (vehicle.getInsuranceExpiryDate() != null && 
            vehicle.getInsuranceExpiryDate().isBefore(today)) {
            return VehicleStatusEnum.INSURANCE_EXPIRED.name();
        }
        
        // 3. Kiểm tra sắp hết hạn đăng kiểm (trong vòng warningDays)
        if (vehicle.getInspectionExpiryDate() != null && 
            !vehicle.getInspectionExpiryDate().isBefore(today) &&
            !vehicle.getInspectionExpiryDate().isAfter(warningDate)) {
            return VehicleStatusEnum.INSPECTION_DUE.name();
        }
        
        // 4. Kiểm tra sắp hết hạn bảo hiểm (trong vòng warningDays)
        if (vehicle.getInsuranceExpiryDate() != null && 
            !vehicle.getInsuranceExpiryDate().isBefore(today) &&
            !vehicle.getInsuranceExpiryDate().isAfter(warningDate)) {
            return VehicleStatusEnum.INSURANCE_DUE.name();
        }
        
        // 5. Kiểm tra sắp đến hạn bảo dưỡng (trong vòng warningDays)
        if (vehicle.getNextMaintenanceDate() != null && 
            !vehicle.getNextMaintenanceDate().isBefore(today) &&
            !vehicle.getNextMaintenanceDate().isAfter(warningDate)) {
            return VehicleStatusEnum.MAINTENANCE_DUE.name();
        }
        
        // 6. Mặc định: ACTIVE
        return VehicleStatusEnum.ACTIVE.name();
    }

    /**
     * Kiểm tra thủ công (có thể gọi từ API)
     */
    public void runManualCheck() {
        log.info("🔄 [VehicleExpiryCheckService] Chạy kiểm tra thủ công...");
        checkVehicleExpiry();
    }
    
    /**
     * Tạo tự động các lịch bảo trì/đăng kiểm cho xe đã quá hạn
     * Được gọi tự động mỗi ngày trong checkVehicleExpiry()
     */
    @Transactional
    public void createOverdueMaintenanceRecords() {
        LocalDate today = LocalDate.now();
        log.info("📅 [VehicleExpiryCheckService] Kiểm tra và tạo lịch bảo trì/đăng kiểm quá hạn...");
        
        // Lấy danh sách xe có ngày bảo trì tiếp theo đã quá hạn
        List<VehicleEntity> overdueMaintenanceVehicles = vehicleRepository.findAll().stream()
            .filter(v -> v.getNextMaintenanceDate() != null && v.getNextMaintenanceDate().isBefore(today))
            .filter(v -> !PROTECTED_STATUSES.contains(v.getStatus()))
            .toList();
        
        // Lấy danh sách xe có ngày đăng kiểm đã quá hạn
        List<VehicleEntity> overdueInspectionVehicles = vehicleRepository.findAll().stream()
            .filter(v -> v.getInspectionExpiryDate() != null && v.getInspectionExpiryDate().isBefore(today))
            .filter(v -> !PROTECTED_STATUSES.contains(v.getStatus()))
            .toList();
        
        int maintenanceCreated = 0;
        int inspectionCreated = 0;
        
        // Tạo lịch bảo trì quá hạn
        for (VehicleEntity vehicle : overdueMaintenanceVehicles) {
            // Kiểm tra xem đã có lịch bảo trì PLANNED hoặc OVERDUE nào chưa
            List<VehicleServiceRecordResponse> existingRecords = vehicleServiceRecordService.getRecordsByVehicleId(vehicle.getId());
            boolean hasPlannedMaintenance = existingRecords.stream()
                .anyMatch(r -> "Bảo dưỡng định kỳ".equals(r.serviceType()) && 
                          ("PLANNED".equals(r.serviceStatus()) || "OVERDUE".equals(r.serviceStatus())));
            
            if (!hasPlannedMaintenance) {
                // Tạo lịch bảo trì mới với trạng thái OVERDUE
                try {
                    // Tạo request để gọi service
                    LocalDateTime plannedDate = vehicle.getNextMaintenanceDate().atStartOfDay();
                    capstone_project.dtos.request.vehicle.VehicleServiceRecordRequest req = new capstone_project.dtos.request.vehicle.VehicleServiceRecordRequest(
                        "Bảo dưỡng định kỳ",
                        "OVERDUE", // Trạng thái quá hạn
                        plannedDate, // Ngày dự kiến là ngày đã quá hạn
                        null, // Chưa có ngày thực hiện
                        null, // Chưa có ngày tiếp theo
                        "Tạo tự động cho xe quá hạn bảo trì", // Mô tả
                        null, // Chưa có số đồng hồ công tơ mét
                        null, // Chưa có ghi chú
                        vehicle.getId().toString() // ID xe
                    );
                    
                    vehicleServiceRecordService.createRecord(req);
                    maintenanceCreated++;
                    log.info("🔧 Tạo tự động lịch bảo trì quá hạn cho xe {} ({})", 
                            vehicle.getLicensePlateNumber(), vehicle.getId());
                } catch (Exception e) {
                    log.error("❌ Lỗi khi tạo lịch bảo trì quá hạn cho xe {}: {}", 
                            vehicle.getLicensePlateNumber(), e.getMessage());
                }
            }
        }
        
        // Tạo lịch đăng kiểm quá hạn
        for (VehicleEntity vehicle : overdueInspectionVehicles) {
            // Kiểm tra xem đã có lịch đăng kiểm PLANNED hoặc OVERDUE nào chưa
            List<VehicleServiceRecordResponse> existingRecords = vehicleServiceRecordService.getRecordsByVehicleId(vehicle.getId());
            boolean hasPlannedInspection = existingRecords.stream()
                .anyMatch(r -> "Đăng kiểm định kỳ".equals(r.serviceType()) && 
                          ("PLANNED".equals(r.serviceStatus()) || "OVERDUE".equals(r.serviceStatus())));
            
            if (!hasPlannedInspection) {
                // Tạo lịch đăng kiểm mới với trạng thái OVERDUE
                try {
                    // Tạo request để gọi service
                    LocalDateTime plannedDate = vehicle.getInspectionExpiryDate().atStartOfDay();
                    capstone_project.dtos.request.vehicle.VehicleServiceRecordRequest req = new capstone_project.dtos.request.vehicle.VehicleServiceRecordRequest(
                        "Đăng kiểm định kỳ",
                        "OVERDUE", // Trạng thái quá hạn
                        plannedDate, // Ngày dự kiến là ngày đã quá hạn
                        null, // Chưa có ngày thực hiện
                        null, // Chưa có ngày tiếp theo
                        "Tạo tự động cho xe quá hạn đăng kiểm", // Mô tả
                        null, // Chưa có số đồng hồ công tơ mét
                        null, // Chưa có ghi chú
                        vehicle.getId().toString() // ID xe
                    );
                    
                    vehicleServiceRecordService.createRecord(req);
                    inspectionCreated++;
                    log.info("🛠️ Tạo tự động lịch đăng kiểm quá hạn cho xe {} ({})", 
                            vehicle.getLicensePlateNumber(), vehicle.getId());
                } catch (Exception e) {
                    log.error("❌ Lỗi khi tạo lịch đăng kiểm quá hạn cho xe {}: {}", 
                            vehicle.getLicensePlateNumber(), e.getMessage());
                }
            }
        }
        
        log.info("📅 [VehicleExpiryCheckService] Đã tạo {} lịch bảo trì và {} lịch đăng kiểm quá hạn", 
                maintenanceCreated, inspectionCreated);
    }
}
