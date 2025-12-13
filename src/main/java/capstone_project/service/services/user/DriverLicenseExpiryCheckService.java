package capstone_project.service.services.user;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.repository.entityServices.user.DriverEntityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Background service to check driver license expiry dates
 * and automatically deactivate drivers with expired licenses.
 * 
 * Runs daily at 2:00 AM to check all active drivers.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DriverLicenseExpiryCheckService {

    private final DriverEntityService driverEntityService;

    /**
     * Check all active drivers for expired licenses
     * Runs daily at 2:00 AM
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void checkDriverLicenseExpiry() {
        log.info("🔍 [DriverLicenseExpiryCheckService] Bắt đầu kiểm tra hạn bằng lái tài xế...");

        LocalDate today = LocalDate.now();
        int expiredCount = 0;
        int warningCount = 0;

        // Get all active drivers
        List<DriverEntity> activeDrivers = driverEntityService.findByStatus(CommonStatusEnum.ACTIVE.name());

        for (DriverEntity driver : activeDrivers) {
            if (driver.getDateOfExpiry() == null) {
                log.warn("⚠️ Tài xế {} ({}) không có ngày hết hạn bằng lái",
                        driver.getUser().getFullName(), driver.getId());
                continue;
            }

            LocalDate expiryDate = driver.getDateOfExpiry().toLocalDate();

            // Check if license is expired
            if (expiryDate.isBefore(today)) {
                // Deactivate driver
                driver.setStatus(CommonStatusEnum.INACTIVE.name());
                driverEntityService.save(driver);

                log.warn("🚫 Tài xế {} ({}) đã bị vô hiệu hóa do bằng lái hết hạn ngày {}",
                        driver.getUser().getFullName(), driver.getId(), expiryDate);
                expiredCount++;
            }
            // Check if license expires within 7 days (critical warning)
            else if (expiryDate.isBefore(today.plusDays(7))) {
                log.warn("⚠️ KHẨN CẤP: Tài xế {} ({}) có bằng lái sẽ hết hạn trong {} ngày (ngày {})",
                        driver.getUser().getFullName(), driver.getId(),
                        java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate),
                        expiryDate);
                warningCount++;
            }
            // Check if license expires within 60 days (warning)
            else if (expiryDate.isBefore(today.plusDays(60))) {
                log.info("📢 Tài xế {} ({}) có bằng lái sẽ hết hạn trong {} ngày (ngày {})",
                        driver.getUser().getFullName(), driver.getId(),
                        java.time.temporal.ChronoUnit.DAYS.between(today, expiryDate),
                        expiryDate);
                warningCount++;
            }
        }

        log.info("🔍 [DriverLicenseExpiryCheckService] Hoàn thành kiểm tra. " +
                        "Đã vô hiệu hóa: {} tài xế, Cảnh báo: {} tài xế",
                expiredCount, warningCount);
    }

    /**
     * Manual check (can be called from API)
     */
    public void runManualCheck() {
        log.info("🔄 [DriverLicenseExpiryCheckService] Chạy kiểm tra thủ công...");
        checkDriverLicenseExpiry();
    }

    /**
     * Get count of drivers with expiring licenses
     * @param daysUntilExpiry Number of days until expiry
     * @return Count of drivers
     */
    public long getExpiringDriversCount(int daysUntilExpiry) {
        LocalDateTime cutoffDate = LocalDateTime.now().plusDays(daysUntilExpiry);
        List<DriverEntity> activeDrivers = driverEntityService.findByStatus(CommonStatusEnum.ACTIVE.name());
        
        return activeDrivers.stream()
                .filter(d -> d.getDateOfExpiry() != null)
                .filter(d -> d.getDateOfExpiry().isBefore(cutoffDate))
                .count();
    }
}
