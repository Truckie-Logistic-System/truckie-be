package capstone_project.service.services.vehicle;

import capstone_project.common.enums.VehicleServiceStatusEnum;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleServiceRecordEntity;
import capstone_project.repository.repositories.vehicle.VehicleRepository;
import capstone_project.repository.repositories.vehicle.VehicleServiceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import capstone_project.common.utils.VietnamTimeUtils;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@PropertySource(value = "classpath:list-config.properties", encoding = "UTF-8")
public class VehicleServiceRecordDemoService {

    private final VehicleRepository vehicleRepository;
    private final VehicleServiceRecordRepository serviceRecordRepository;

    // Service types from config
    @Value("#{'${vehicle.service.types}'.split(',')}")
    private List<String> serviceTypes;

    @PostConstruct
    public void init() {
        log.info("Loaded {} service types from config: {}", serviceTypes.size(), serviceTypes);
    }

    @Transactional
    public Map<String, Object> generateDemoDataForAllVehicles() {
        List<VehicleEntity> vehicles = vehicleRepository.findAll();
        List<VehicleServiceRecordEntity> createdRecords = new ArrayList<>();
        
        LocalDateTime now = VietnamTimeUtils.now();
        Random random = new Random();

        for (VehicleEntity vehicle : vehicles) {
            // Generate 3-5 records per vehicle
            int recordCount = 3 + random.nextInt(3);
            
            // Generate historical records
            for (int i = 0; i < recordCount; i++) {
                VehicleServiceRecordEntity record = generateServiceRecord(vehicle, now, i, recordCount, random);
                createdRecords.add(record);
            }
            
            // Generate 1-2 upcoming records
            int upcomingCount = 1 + random.nextInt(2);
            for (int i = 0; i < upcomingCount; i++) {
                VehicleServiceRecordEntity record = generateUpcomingServiceRecord(vehicle, now, i, random);
                createdRecords.add(record);
            }
        }

        // Save all records
        serviceRecordRepository.saveAll(createdRecords);
        
        Map<String, Object> result = new HashMap<>();
        result.put("vehiclesProcessed", vehicles.size());
        result.put("recordsCreated", createdRecords.size());
        
        // Statistics
        Map<String, Long> statusStats = new HashMap<>();
        createdRecords.stream()
            .map(r -> r.getServiceStatus().toString())
            .forEach(status -> statusStats.merge(status, 1L, Long::sum));
        result.put("statusDistribution", statusStats);
        
        return result;
    }

    private VehicleServiceRecordEntity generateServiceRecord(VehicleEntity vehicle, LocalDateTime now, 
            int index, int total, Random random) {
        // Calculate planned date (historical)
        int monthsBack = (total - index) * 6; // Records every 6 months
        LocalDateTime plannedDate = now.minusMonths(monthsBack);
        
        // Random service type
        String serviceType = serviceTypes.get(random.nextInt(serviceTypes.size()));
        
        // Determine status based on date and random
        VehicleServiceStatusEnum status;
        LocalDateTime actualDate = null;
        LocalDateTime nextServiceDate = null;
        
        // 70% completed, 20% overdue, 10% cancelled
        int statusRoll = random.nextInt(100);
        if (statusRoll < 70) {
            status = VehicleServiceStatusEnum.COMPLETED;
            // Actual date within 1-7 days after planned
            actualDate = plannedDate.plusDays(1 + random.nextInt(7));
            // Next service date 3-6 months after completion
            nextServiceDate = actualDate.plusMonths(3 + random.nextInt(4));
        } else if (statusRoll < 90) {
            status = VehicleServiceStatusEnum.OVERDUE;
            // Overdue means planned date passed but not completed
            nextServiceDate = plannedDate.plusMonths(3 + random.nextInt(4));
        } else {
            status = VehicleServiceStatusEnum.CANCELLED;
            // Cancelled records don't have next service date
        }
        
        return VehicleServiceRecordEntity.builder()
            .vehicleEntity(vehicle)
            .serviceType(serviceType)
            .serviceStatus(status)
            .plannedDate(plannedDate)
            .actualDate(actualDate)
            .nextServiceDate(nextServiceDate)
            .description(generateDescription(serviceType, status))
            .odometerReading(50000 + random.nextInt(150000)) // 50k-200k km
            .isDemoData(false)
            .build();
    }

    private VehicleServiceRecordEntity generateUpcomingServiceRecord(VehicleEntity vehicle, 
            LocalDateTime now, int index, Random random) {
        // Calculate future planned date
        int monthsAhead = 1 + index * 3; // 1 month, 4 months, etc.
        LocalDateTime plannedDate = now.plusMonths(monthsAhead);
        
        // Random service type
        String serviceType = serviceTypes.get(random.nextInt(serviceTypes.size()));
        
        // Calculate next service date
        LocalDateTime nextServiceDate = plannedDate.plusMonths(3 + random.nextInt(4));
        
        return VehicleServiceRecordEntity.builder()
            .vehicleEntity(vehicle)
            .serviceType(serviceType)
            .serviceStatus(VehicleServiceStatusEnum.PLANNED)
            .plannedDate(plannedDate)
            .actualDate(null)
            .nextServiceDate(nextServiceDate)
            .description(generateDescription(serviceType, VehicleServiceStatusEnum.PLANNED))
            .odometerReading(50000 + random.nextInt(150000))
            .isDemoData(false)
            .build();
    }

    private String generateDescription(String serviceType, VehicleServiceStatusEnum status) {
        switch (serviceType) {
            case "Đăng kiểm định kỳ":
                if (status == VehicleServiceStatusEnum.COMPLETED) {
                    return "Đăng kiểm thành công, giấy chứng nhận hợp lệ 2 năm";
                } else if (status == VehicleServiceStatusEnum.OVERDUE) {
                    return "Quá hạn đăng kiểm, cần hoàn thành ASAP";
                } else if (status == VehicleServiceStatusEnum.CANCELLED) {
                    return "Hủy lịch đăng kiểm do xe đang sửa chữa";
                }
                return "Lịch đăng kiểm định kỳ theo quy định";
                
            case "Gia hạn bảo hiểm":
                if (status == VehicleServiceStatusEnum.COMPLETED) {
                    return "Gia hạn bảo hiểm thành công, hiệu lực 1 năm";
                } else if (status == VehicleServiceStatusEnum.OVERDUE) {
                    return "Quá hạn bảo hiểm, cần gia hạn ngay";
                }
                return "Lịch gia hạn bảo hiểm xe";
                
            case "Bảo dưỡng định kỳ":
                if (status == VehicleServiceStatusEnum.COMPLETED) {
                    return "Bảo dưỡng thành công, thay thế lọc dầu, kiểm tra hệ thống";
                } else if (status == VehicleServiceStatusEnum.OVERDUE) {
                    return "Quá hạn bảo dưỡng, cần thực hiện sớm";
                }
                return "Bảo dưỡng tổng thể theo lịch";
                
            case "Sửa chữa":
                if (status == VehicleServiceStatusEnum.COMPLETED) {
                    return "Sửa chữa thành công, xe đã sẵn sàng vận hành";
                } else if (status == VehicleServiceStatusEnum.OVERDUE) {
                    return "Quá hạn sửa chữa, cần xử lý ngay";
                }
                return "Lịch sửa chữa xe";
                
            case "Khác":
                return "Dịch vụ khác";
                
            default:
                return "Bảo dưỡng xe";
        }
    }

    @Transactional
    public void clearAllDemoData() {
        List<VehicleServiceRecordEntity> demoRecords = serviceRecordRepository.findByIsDemoDataTrue();
        serviceRecordRepository.deleteAll(demoRecords);
        log.info("Đã xóa {} bản ghi demo", demoRecords.size());
    }

    public Map<String, Object> getDemoStatistics() {
        List<VehicleServiceRecordEntity> demoRecords = serviceRecordRepository.findByIsDemoDataTrue();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDemoRecords", demoRecords.size());
        
        // Status distribution
        Map<String, Long> statusStats = new HashMap<>();
        demoRecords.stream()
            .map(r -> r.getServiceStatus().toString())
            .forEach(status -> statusStats.merge(status, 1L, Long::sum));
        stats.put("statusDistribution", statusStats);
        
        // Service type distribution
        Map<String, Long> typeStats = new HashMap<>();
        demoRecords.stream()
            .map(VehicleServiceRecordEntity::getServiceType)
            .forEach(type -> typeStats.merge(type, 1L, Long::sum));
        stats.put("serviceTypeDistribution", typeStats);
        
        // Vehicles with demo data
        long vehiclesWithDemo = demoRecords.stream()
            .map(VehicleServiceRecordEntity::getVehicleEntity)
            .map(VehicleEntity::getId)
            .distinct()
            .count();
        stats.put("vehiclesWithDemoData", vehiclesWithDemo);
        
        // Upcoming vs overdue
        LocalDateTime now = VietnamTimeUtils.now();
        long upcoming = demoRecords.stream()
            .filter(r -> r.getPlannedDate().isAfter(now))
            .count();
        long pastDue = demoRecords.stream()
            .filter(r -> r.getPlannedDate().isBefore(now) && 
                        r.getServiceStatus() != VehicleServiceStatusEnum.COMPLETED &&
                        r.getServiceStatus() != VehicleServiceStatusEnum.CANCELLED &&
                        r.getServiceStatus() != VehicleServiceStatusEnum.PLANNED)
            .count();
        stats.put("overdueServices", pastDue);
        
        return stats;
    }

    /**
     * Method to get a random service status
     */
    @Transactional
    public void updateOverdueServiceRecords() {
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
