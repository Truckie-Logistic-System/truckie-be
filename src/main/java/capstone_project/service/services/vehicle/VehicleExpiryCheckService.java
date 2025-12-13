package capstone_project.service.services.vehicle;

import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.repositories.vehicle.VehicleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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
}
