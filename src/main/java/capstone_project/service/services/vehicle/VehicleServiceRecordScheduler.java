package capstone_project.service.services.vehicle;

import capstone_project.common.enums.VehicleServiceStatusEnum;
import capstone_project.entity.vehicle.VehicleServiceRecordEntity;
import capstone_project.repository.repositories.vehicle.VehicleServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import capstone_project.common.utils.VietnamTimeUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceRecordScheduler {

    private final VehicleServiceRecordRepository serviceRecordRepository;

    /**
     * Chạy mỗi giờ để cập nhật các bản ghi PLANNED đã quá hạn thành OVERDUE
     * Cron: 0 0 * * * * (giờ thứ 0 của mỗi phút)
     */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void updateOverdueServiceRecords() {
        log.info("Bắt đầu cập nhật trạng thái quá hạn cho service records...");
        
        LocalDateTime now = VietnamTimeUtils.now();
        
        // Tìm tất cả bản ghi PLANNED có planned_date < now
        List<VehicleServiceRecordEntity> plannedRecords = serviceRecordRepository
                .findByServiceStatusAndPlannedDateBefore(VehicleServiceStatusEnum.PLANNED, now);
        
        if (plannedRecords.isEmpty()) {
            log.info("Không có bản ghi PLANNED nào cần cập nhật");
            return;
        }
        
        // Cập nhật tất cả thành OVERDUE
        plannedRecords.forEach(record -> {
            record.setServiceStatus(VehicleServiceStatusEnum.OVERDUE);
            log.debug("Cập nhật bản ghi ID {} từ PLANNED thành OVERDUE (planned_date: {})", 
                    record.getId(), record.getPlannedDate());
        });
        
        // Lưu tất cả các bản ghi đã cập nhật
        serviceRecordRepository.saveAll(plannedRecords);
        
        log.info("Đã cập nhật {} bản ghi từ PLANNED thành OVERDUE", plannedRecords.size());
    }
}
