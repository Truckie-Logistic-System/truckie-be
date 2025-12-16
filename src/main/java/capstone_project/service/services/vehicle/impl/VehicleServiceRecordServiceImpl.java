package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.VehicleServiceStatusEnum;
import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleServiceRecordRequest;
import capstone_project.dtos.request.vehicle.VehicleServiceRecordRequest;
import capstone_project.dtos.response.vehicle.PaginatedServiceRecordsResponse;
import capstone_project.dtos.response.vehicle.VehicleServiceRecordResponse;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleServiceRecordEntity;
import capstone_project.repository.entityServices.vehicle.VehicleServiceRecordEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.repositories.vehicle.VehicleServiceRecordRepository;
import capstone_project.service.mapper.vehicle.VehicleServiceRecordMapper;
import capstone_project.service.services.vehicle.VehicleServiceRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import capstone_project.common.utils.VietnamTimeUtils;

import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleServiceRecordServiceImpl implements VehicleServiceRecordService {

    private final VehicleServiceRecordEntityService entityService;
    private final VehicleServiceRecordRepository repository;
    private final VehicleServiceRecordMapper mapper;
    private final VehicleEntityService vehicleEntityService;
    
    @org.springframework.beans.factory.annotation.Value("${vehicle.service.types}")
    private String serviceTypesConfig;

    @Override
    public List<VehicleServiceRecordResponse> getAllRecords() {
        return Optional.of(entityService.findAll())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new NotFoundException(
                        "Không có bản ghi dịch vụ xe nào.",
                        ErrorEnum.NOT_FOUND.getErrorCode()))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public VehicleServiceRecordResponse getRecordById(UUID id) {
        // Use the new method to fetch the record with vehicle and vehicle type in a single query
        VehicleServiceRecordEntity entity = entityService.findByIdWithVehicleAndVehicleType(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public VehicleServiceRecordResponse createRecord(VehicleServiceRecordRequest req) {
        var record = mapper.toEntity(req);
        record.setServiceStatus(VehicleServiceStatusEnum.PLANNED);

        var vehicleId = UUID.fromString(req.vehicleId());
        var vehicle = vehicleEntityService.findByVehicleId(vehicleId)
                .orElseThrow(() ->
                        new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),
                                ErrorEnum.NOT_FOUND.getErrorCode()));

        var saved = entityService.save(record);
        
        // Update vehicle expiry dates based on service type
        if (req.nextServiceDate() != null) {
            String serviceType = req.serviceType();
            LocalDateTime nextServiceDate = req.nextServiceDate();
            
            log.info("Updating vehicle {} expiry dates for service type: {} with nextServiceDate: {}", 
                    vehicle.getLicensePlateNumber(), serviceType, nextServiceDate);
            
            if ("Đăng kiểm định kỳ".equals(serviceType)) {
                vehicle.setInspectionExpiryDate(nextServiceDate.toLocalDate());
                // Also update last inspection date if actual date is provided
                if (req.actualDate() != null) {
                    vehicle.setLastInspectionDate(req.actualDate().toLocalDate());
                }
            } else if ("Gia hạn bảo hiểm".equals(serviceType)) {
                vehicle.setInsuranceExpiryDate(nextServiceDate.toLocalDate());
            } else {
                // For all other maintenance types
                vehicle.setNextMaintenanceDate(nextServiceDate.toLocalDate());
                // Also update last maintenance date if actual date is provided
                if (req.actualDate() != null) {
                    vehicle.setLastMaintenanceDate(req.actualDate().toLocalDate());
                }
            }
            vehicleEntityService.save(vehicle);
            log.info("Successfully updated vehicle {} expiry dates", vehicle.getLicensePlateNumber());
        }
        
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleServiceRecordResponse updateRecord(UUID id, UpdateVehicleServiceRecordRequest req) {
        var existing = entityService.findEntityById(id).orElseThrow(() ->
                new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        mapper.toEntity(req, existing);
        
        // Update vehicle expiry dates if nextServiceDate is provided
        if (req.nextServiceDate() != null && existing.getVehicleEntity() != null) {
            var vehicle = existing.getVehicleEntity();
            String serviceType = existing.getServiceType();
            LocalDateTime nextServiceDate = req.nextServiceDate();
            
            log.info("Updating vehicle {} expiry dates for service type: {} with nextServiceDate: {}", 
                    vehicle.getLicensePlateNumber(), serviceType, nextServiceDate);
            
            if ("Đăng kiểm định kỳ".equals(serviceType)) {
                vehicle.setInspectionExpiryDate(nextServiceDate.toLocalDate());
                // Also update last inspection date if actual date is provided
                if (req.actualDate() != null) {
                    vehicle.setLastInspectionDate(req.actualDate().toLocalDate());
                }
            } else if ("Gia hạn bảo hiểm".equals(serviceType)) {
                vehicle.setInsuranceExpiryDate(nextServiceDate.toLocalDate());
            } else {
                // For all other maintenance types
                vehicle.setNextMaintenanceDate(nextServiceDate.toLocalDate());
                // Also update last maintenance date if actual date is provided
                if (req.actualDate() != null) {
                    vehicle.setLastMaintenanceDate(req.actualDate().toLocalDate());
                }
            }
            vehicleEntityService.save(vehicle);
            log.info("Successfully updated vehicle {} expiry dates", vehicle.getLicensePlateNumber());
        }

        var updated = entityService.save(existing);
        return mapper.toResponse(updated);
    }

    @Override
    public PaginatedServiceRecordsResponse getAllRecordsPaginated(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VehicleServiceRecordEntity> pageResult = repository.findAllByOrderByCreatedAtDesc(pageable);
        
        List<VehicleServiceRecordResponse> content = pageResult.getContent().stream()
                .map(mapper::toResponse)
                .toList();
        
        return new PaginatedServiceRecordsResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Override
    public PaginatedServiceRecordsResponse getRecordsByType(String serviceType, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VehicleServiceRecordEntity> pageResult = repository.findByServiceTypeOrderByCreatedAtDesc(serviceType, pageable);
        
        List<VehicleServiceRecordResponse> content = pageResult.getContent().stream()
                .map(mapper::toResponse)
                .toList();
        
        return new PaginatedServiceRecordsResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Override
    public PaginatedServiceRecordsResponse getRecordsByStatus(VehicleServiceStatusEnum serviceStatus, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<VehicleServiceRecordEntity> pageResult = repository.findByServiceStatusOrderByCreatedAtDesc(serviceStatus, pageable);
        
        List<VehicleServiceRecordResponse> content = pageResult.getContent().stream()
                .map(mapper::toResponse)
                .toList();
        
        return new PaginatedServiceRecordsResponse(
                content,
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages()
        );
    }

    @Override
    public List<VehicleServiceRecordResponse> getRecordsByVehicleId(UUID vehicleId) {
        return repository.findByVehicleEntityId(vehicleId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public VehicleServiceRecordResponse completeRecord(UUID id) {
        VehicleServiceRecordEntity record = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy bản ghi dịch vụ với ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (record.getServiceStatus() == VehicleServiceStatusEnum.COMPLETED) {
            throw new BadRequestException(
                    "Bản ghi này đã được hoàn thành trước đó",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        if (record.getServiceStatus() == VehicleServiceStatusEnum.CANCELLED) {
            throw new BadRequestException(
                    "Không thể hoàn thành bản ghi đã bị hủy",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        record.setServiceStatus(VehicleServiceStatusEnum.COMPLETED);
        record.setActualDate(VietnamTimeUtils.now());

        VehicleEntity vehicle = record.getVehicleEntity();
        if (vehicle != null) {
            String serviceType = record.getServiceType();
            LocalDateTime nextServiceDate = record.getNextServiceDate();
            
            if ("Đăng kiểm định kỳ".equals(serviceType)) {
                vehicle.setLastInspectionDate(record.getActualDate().toLocalDate());
                // Cập nhật ngày hết hạn đăng kiểm từ nextServiceDate
                if (nextServiceDate != null) {
                    vehicle.setInspectionExpiryDate(nextServiceDate.toLocalDate());
                }
                if (VehicleStatusEnum.INSPECTION_EXPIRED.name().equals(vehicle.getStatus()) ||
                    VehicleStatusEnum.INSPECTION_DUE.name().equals(vehicle.getStatus())) {
                    vehicle.setStatus(VehicleStatusEnum.ACTIVE.name());
                }
                log.info("✅ Đã cập nhật đăng kiểm cho xe {} - Ngày kiểm định: {}, Hạn tiếp theo: {}", 
                        vehicle.getLicensePlateNumber(), record.getActualDate().toLocalDate(), nextServiceDate);
                        
            } else if ("Gia hạn bảo hiểm".equals(serviceType)) {
                // Cập nhật ngày hết hạn bảo hiểm từ nextServiceDate
                if (nextServiceDate != null) {
                    vehicle.setInsuranceExpiryDate(nextServiceDate.toLocalDate());
                }
                if (VehicleStatusEnum.INSURANCE_EXPIRED.name().equals(vehicle.getStatus()) ||
                    VehicleStatusEnum.INSURANCE_DUE.name().equals(vehicle.getStatus())) {
                    vehicle.setStatus(VehicleStatusEnum.ACTIVE.name());
                }
                log.info("✅ Đã cập nhật bảo hiểm cho xe {} - Ngày gia hạn: {}, Hạn tiếp theo: {}", 
                        vehicle.getLicensePlateNumber(), record.getActualDate().toLocalDate(), nextServiceDate);
                        
            } else {
                // Bảo dưỡng định kỳ hoặc các loại khác
                vehicle.setLastMaintenanceDate(record.getActualDate().toLocalDate());
                // Cập nhật ngày bảo dưỡng tiếp theo
                if (nextServiceDate != null) {
                    vehicle.setNextMaintenanceDate(nextServiceDate.toLocalDate());
                }
                if (VehicleStatusEnum.MAINTENANCE.name().equals(vehicle.getStatus()) ||
                    VehicleStatusEnum.MAINTENANCE_DUE.name().equals(vehicle.getStatus())) {
                    vehicle.setStatus(VehicleStatusEnum.ACTIVE.name());
                }
                log.info("✅ Đã cập nhật bảo trì cho xe {} - Ngày bảo trì tiếp theo: {}", 
                        vehicle.getLicensePlateNumber(), nextServiceDate);
            }
            
            vehicleEntityService.save(vehicle);
        }

        var saved = entityService.save(record);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleServiceRecordResponse cancelRecord(UUID id) {
        VehicleServiceRecordEntity record = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy bản ghi dịch vụ với ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (record.getServiceStatus() == VehicleServiceStatusEnum.COMPLETED) {
            throw new BadRequestException(
                    "Không thể hủy bản ghi đã hoàn thành",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        if (record.getServiceStatus() == VehicleServiceStatusEnum.CANCELLED) {
            throw new BadRequestException(
                    "Bản ghi này đã bị hủy trước đó",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        record.setServiceStatus(VehicleServiceStatusEnum.CANCELLED);
        
        VehicleEntity vehicle = record.getVehicleEntity();
        if (vehicle != null && VehicleStatusEnum.MAINTENANCE.name().equals(vehicle.getStatus())) {
            vehicle.setStatus(VehicleStatusEnum.ACTIVE.name());
            vehicleEntityService.save(vehicle);
        }

        var saved = entityService.save(record);
        log.info("❌ Đã hủy bản ghi dịch vụ ID: {}", id);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleServiceRecordResponse startRecord(UUID id) {
        VehicleServiceRecordEntity record = entityService.findEntityById(id).orElse(null);
        if (record == null) {
            throw new BadRequestException(
                    "Không tìm thấy bản ghi dịch vụ với ID: " + id,
                    ErrorEnum.ENTITY_NOT_FOUND.getErrorCode()
            );
        }

        if (record.getServiceStatus() != VehicleServiceStatusEnum.PLANNED) {
            throw new BadRequestException(
                    "Chỉ có thể bắt đầu bản ghi có trạng thái PLANNED",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Cập nhật trạng thái service record
        record.setServiceStatus(VehicleServiceStatusEnum.IN_PROGRESS);
        
        // Cập nhật trạng thái xe thành MAINTENANCE
        VehicleEntity vehicle = record.getVehicleEntity();
        if (vehicle != null) {
            vehicle.setStatus(VehicleStatusEnum.MAINTENANCE.name());
            vehicleEntityService.save(vehicle);
            log.info("🔧 Xe {} đã chuyển sang trạng thái MAINTENANCE", vehicle.getLicensePlateNumber());
        }

        var saved = entityService.save(record);
        log.info("▶️ Đã bắt đầu bản ghi dịch vụ ID: {} - Loại: {}", id, record.getServiceType());
        return mapper.toResponse(saved);
    }

    @Override
    public List<String> getServiceTypes() {
        if (serviceTypesConfig == null || serviceTypesConfig.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.asList(serviceTypesConfig.split(","));
    }

    @Override
    @Transactional
    public int generateServiceRecordsForAllVehicles() {
        // Lấy tất cả xe đang có trong hệ thống
        List<VehicleEntity> vehicles = vehicleEntityService.findAll();
        if (vehicles == null || vehicles.isEmpty()) {
            log.warn("Không có xe nào trong hệ thống để tạo lịch dịch vụ");
            return 0;
        }

        // Lấy danh sách loại dịch vụ từ cấu hình, đảm bảo khớp FE
        List<String> serviceTypes = getServiceTypes();
        String inspectionType = "Đăng kiểm định kỳ";
        String maintenanceType = "Bảo dưỡng định kỳ";

        boolean hasInspectionType = serviceTypes.contains(inspectionType);
        boolean hasMaintenanceType = serviceTypes.contains(maintenanceType);

        if (!hasInspectionType && !hasMaintenanceType) {
            log.warn("Cấu hình vehicle.service.types không chứa 'Đăng kiểm định kỳ' hoặc 'Bảo dưỡng định kỳ', không thể tạo lịch mẫu");
            return 0;
        }

        LocalDateTime now = VietnamTimeUtils.now();
        Random random = new Random();
        List<VehicleServiceRecordEntity> recordsToSave = new ArrayList<>();

        for (VehicleEntity vehicle : vehicles) {
            // Đăng kiểm định kỳ
            if (hasInspectionType) {
                recordsToSave.add(buildRandomServiceRecord(vehicle, inspectionType, now, random));
            }

            // Bảo dưỡng định kỳ
            if (hasMaintenanceType) {
                recordsToSave.add(buildRandomServiceRecord(vehicle, maintenanceType, now, random));
            }
        }

        if (recordsToSave.isEmpty()) {
            return 0;
        }

        repository.saveAll(recordsToSave);
        log.info("Đã tạo {} lịch đăng kiểm/bảo trì mẫu cho {} xe", recordsToSave.size(), vehicles.size());
        return recordsToSave.size();
    }

    private VehicleServiceRecordEntity buildRandomServiceRecord(
            VehicleEntity vehicle,
            String serviceType,
            LocalDateTime now,
            Random random
    ) {
        // Trộn giữa lịch sắp tới và lịch đã qua
        boolean upcoming = random.nextBoolean();

        LocalDateTime plannedDate;

        if (upcoming) {
            // Lịch sắp tới: planned trong vòng 0-90 ngày tới
            int plannedOffsetDays = random.nextInt(91); // 0 .. 90
            plannedDate = now.plusDays(plannedOffsetDays);
        } else {
            // Lịch đã qua: planned cách đây 1-6 tháng
            int plannedOffsetMonths = 1 + random.nextInt(6); // 1..6
            plannedDate = now.minusMonths(plannedOffsetMonths).plusDays(random.nextInt(30));
        }

        String description;
        if ("Đăng kiểm định kỳ".equals(serviceType)) {
            description = "Đăng kiểm định kỳ cho xe " + vehicle.getLicensePlateNumber();
        } else if ("Bảo dưỡng định kỳ".equals(serviceType)) {
            description = "Bảo dưỡng định kỳ cho xe " + vehicle.getLicensePlateNumber();
        } else {
            description = "Dịch vụ " + serviceType + " cho xe " + vehicle.getLicensePlateNumber();
        }

        return VehicleServiceRecordEntity.builder()
                .serviceType(serviceType)
                .serviceStatus(VehicleServiceStatusEnum.PLANNED)
                .plannedDate(plannedDate)
                .description(description)
                .vehicleEntity(vehicle)
                .build();
    }

    @Override
    public List<VehicleServiceRecordResponse> getServicesDueSoon(int warningDays) {
        LocalDateTime today = VietnamTimeUtils.now();
        LocalDateTime beforeDate = today.plusDays(warningDays);
        
        List<VehicleServiceRecordEntity> records = repository.findServicesDueSoon(today, beforeDate);
        
        // Filter to avoid duplicates: if there's both PLANNED and COMPLETED for same vehicle/service type,
        // keep only the most recent one based on nextServiceDate
        Map<String, VehicleServiceRecordEntity> latestRecords = new HashMap<>();
        
        for (VehicleServiceRecordEntity record : records) {
            String key = record.getVehicleEntity().getId() + "-" + record.getServiceType();
            VehicleServiceRecordEntity existing = latestRecords.get(key);
            
            if (existing == null || record.getNextServiceDate().isAfter(existing.getNextServiceDate())) {
                latestRecords.put(key, record);
            }
        }
        
        return latestRecords.values().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<VehicleServiceRecordResponse> getOverdueServices() {
        LocalDateTime today = VietnamTimeUtils.now();
        
        List<VehicleServiceRecordEntity> records = repository.findOverdueServices(today);
        
        // Filter to avoid duplicates: if there's both PLANNED and COMPLETED for same vehicle/service type,
        // keep only the most recent one based on nextServiceDate
        Map<String, VehicleServiceRecordEntity> latestRecords = new HashMap<>();
        
        for (VehicleServiceRecordEntity record : records) {
            String key = record.getVehicleEntity().getId() + "-" + record.getServiceType();
            VehicleServiceRecordEntity existing = latestRecords.get(key);
            
            if (existing == null || record.getNextServiceDate().isAfter(existing.getNextServiceDate())) {
                latestRecords.put(key, record);
            }
        }
        
        return latestRecords.values().stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public int generateDemoAlertDataForBanner() {
        // Lấy tất cả xe hiện có trong hệ thống
        List<VehicleEntity> vehicles = vehicleEntityService.findAll();
        if (vehicles == null || vehicles.isEmpty()) {
            log.warn("[DemoAlertData] Không có phương tiện nào để tạo dữ liệu demo cho banner");
            return 0;
        }

        // Giả sử danh sách đã đủ ngẫu nhiên, chỉ cần lấy tối đa 3 xe đầu tiên
        int maxVehicles = Math.min(3, vehicles.size());
        LocalDateTime now = VietnamTimeUtils.now();
        int createdCount = 0;

        if (maxVehicles >= 1) {
            createdCount += createDemoRecordForVehicle(
                    vehicles.get(0),
                    "Đăng kiểm định kỳ",   // Overdue
                    now.minusDays(10),
                    now.minusDays(3)
            );
        }

        if (maxVehicles >= 2) {
            createdCount += createDemoRecordForVehicle(
                    vehicles.get(1),
                    "Gia hạn bảo hiểm",   // ≤7 ngày
                    now.minusDays(5),
                    now.plusDays(3)
            );
        }

        if (maxVehicles >= 3) {
            createdCount += createDemoRecordForVehicle(
                    vehicles.get(2),
                    "Bảo dưỡng định kỳ",   // 8–30 ngày
                    now.minusDays(2),
                    now.plusDays(15)
            );
        }

        log.info("[DemoAlertData] Đã tạo {} bản ghi demo cho banner cảnh báo bảo trì/đăng kiểm", createdCount);
        return createdCount;
    }

    private int createDemoRecordForVehicle(
            VehicleEntity vehicle,
            String serviceType,
            LocalDateTime actualDate,
            LocalDateTime nextServiceDate
    ) {
        try {
            VehicleServiceRecordEntity record = new VehicleServiceRecordEntity();
            record.setVehicleEntity(vehicle);
            record.setServiceType(serviceType);
            record.setServiceStatus(VehicleServiceStatusEnum.COMPLETED);
            record.setPlannedDate(actualDate.minusDays(1));
            record.setActualDate(actualDate);
            record.setNextServiceDate(nextServiceDate);
            record.setDescription("[DEMO] Bản ghi phục vụ test banner cảnh báo");

            VehicleServiceRecordEntity savedRecord = entityService.save(record);

            // Cập nhật các trường hạn trên VehicleEntity tương tự completeRecord
            LocalDateTime nsd = savedRecord.getNextServiceDate();
            if (nsd != null) {
                if ("Đăng kiểm định kỳ".equals(serviceType)) {
                    vehicle.setLastInspectionDate(savedRecord.getActualDate().toLocalDate());
                    vehicle.setInspectionExpiryDate(nsd.toLocalDate());
                } else if ("Gia hạn bảo hiểm".equals(serviceType)) {
                    vehicle.setInsuranceExpiryDate(nsd.toLocalDate());
                } else {
                    vehicle.setLastMaintenanceDate(savedRecord.getActualDate().toLocalDate());
                    vehicle.setNextMaintenanceDate(nsd.toLocalDate());
                }
                vehicleEntityService.save(vehicle);
            }

            return 1;
        } catch (Exception ex) {
            log.error("[DemoAlertData] Lỗi khi tạo bản ghi demo cho xe {}", vehicle.getId(), ex);
            return 0;
        }
    }
}
