package capstone_project.repository.repositories.vehicle;

import capstone_project.common.enums.VehicleServiceStatusEnum;
import capstone_project.entity.vehicle.VehicleServiceRecordEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleServiceRecordRepository extends BaseRepository<VehicleServiceRecordEntity> {

    List<VehicleServiceRecordEntity> findByVehicleEntityId(UUID vehicleEntityId);

    /**
     * Lấy tất cả records sắp xếp theo ngày tạo giảm dần với vehicle và vehicle type
     * Note: Using countQuery to avoid issues with JOIN FETCH and pagination
     */
    @Query(value = "SELECT vsr FROM VehicleServiceRecordEntity vsr " +
           "LEFT JOIN FETCH vsr.vehicleEntity v " +
           "LEFT JOIN FETCH v.vehicleTypeEntity vt " +
           "ORDER BY vsr.createdAt DESC",
           countQuery = "SELECT COUNT(vsr) FROM VehicleServiceRecordEntity vsr")
    Page<VehicleServiceRecordEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Lấy records theo loại dịch vụ (String) với vehicle và vehicle type
     */
    @Query(value = "SELECT vsr FROM VehicleServiceRecordEntity vsr " +
           "LEFT JOIN FETCH vsr.vehicleEntity v " +
           "LEFT JOIN FETCH v.vehicleTypeEntity vt " +
           "WHERE vsr.serviceType = :serviceType " +
           "ORDER BY vsr.createdAt DESC",
           countQuery = "SELECT COUNT(vsr) FROM VehicleServiceRecordEntity vsr WHERE vsr.serviceType = :serviceType")
    Page<VehicleServiceRecordEntity> findByServiceTypeOrderByCreatedAtDesc(
            @Param("serviceType") String serviceType, Pageable pageable);

    /**
     * Lấy records theo trạng thái với vehicle và vehicle type
     */
    @Query(value = "SELECT vsr FROM VehicleServiceRecordEntity vsr " +
           "LEFT JOIN FETCH vsr.vehicleEntity v " +
           "LEFT JOIN FETCH v.vehicleTypeEntity vt " +
           "WHERE vsr.serviceStatus = :serviceStatus " +
           "ORDER BY vsr.createdAt DESC",
           countQuery = "SELECT COUNT(vsr) FROM VehicleServiceRecordEntity vsr WHERE vsr.serviceStatus = :serviceStatus")
    Page<VehicleServiceRecordEntity> findByServiceStatusOrderByCreatedAtDesc(
            @Param("serviceStatus") VehicleServiceStatusEnum serviceStatus, Pageable pageable);

    /**
     * Lấy records theo xe và loại dịch vụ
     */
    List<VehicleServiceRecordEntity> findByVehicleEntityIdAndServiceTypeOrderByCreatedAtDesc(
            UUID vehicleId, String serviceType);

    /**
     * Lấy record đăng kiểm gần nhất của xe (COMPLETED)
     */
    @Query(value = "SELECT r.* FROM vehicle_service_records r " +
           "WHERE r.vehicle_id = :vehicleId " +
           "AND r.service_type = 'Đăng kiểm định kỳ' " +
           "AND r.service_status = 'COMPLETED' " +
           "ORDER BY r.actual_date DESC LIMIT 1", nativeQuery = true)
    Optional<VehicleServiceRecordEntity> findLatestCompletedInspection(@Param("vehicleId") UUID vehicleId);

    /**
     * Lấy các lịch PLANNED sắp đến hạn
     */
    @Query("SELECT r FROM VehicleServiceRecordEntity r " +
           "WHERE r.serviceStatus = 'PLANNED' " +
           "AND r.plannedDate <= :beforeDate " +
           "ORDER BY r.plannedDate ASC")
    List<VehicleServiceRecordEntity> findUpcomingPlannedServices(@Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Đếm số records theo loại dịch vụ
     */
    long countByServiceType(String serviceType);

    /**
     * Đếm số records theo trạng thái
     */
    long countByServiceStatus(VehicleServiceStatusEnum serviceStatus);

    /**
     * Lấy các lịch sắp đến hạn (nextServiceDate trong khoảng thời gian)
     * Updated: Include both COMPLETED and PLANNED records
     */
    @Query("SELECT r FROM VehicleServiceRecordEntity r " +
           "WHERE (r.serviceStatus = 'COMPLETED' OR r.serviceStatus = 'PLANNED') " +
           "AND r.nextServiceDate IS NOT NULL " +
           "AND r.nextServiceDate >= :today " +
           "AND r.nextServiceDate <= :beforeDate " +
           "ORDER BY r.nextServiceDate ASC")
    List<VehicleServiceRecordEntity> findServicesDueSoon(
            @Param("today") LocalDateTime today, 
            @Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Lấy các lịch đã quá hạn (nextServiceDate < today)
     * Updated: Include both COMPLETED and PLANNED records
     */
    @Query("SELECT r FROM VehicleServiceRecordEntity r " +
           "WHERE (r.serviceStatus = 'COMPLETED' OR r.serviceStatus = 'PLANNED') " +
           "AND r.nextServiceDate IS NOT NULL " +
           "AND r.nextServiceDate < :today " +
           "ORDER BY r.nextServiceDate ASC")
    List<VehicleServiceRecordEntity> findOverdueServices(@Param("today") LocalDateTime today);

    /**
     * Lấy tất cả bản ghi demo (isDemoData = true)
     */
    List<VehicleServiceRecordEntity> findByIsDemoDataTrue();

    /**
     * Lấy các bản ghi PLANNED có ngày dự kiến nhỏ hơn ngày cho trước
     */
    List<VehicleServiceRecordEntity> findByServiceStatusAndPlannedDateBefore(
            VehicleServiceStatusEnum serviceStatus, LocalDateTime date);
            
    /**
     * Find a vehicle service record by ID with eagerly fetched vehicle and vehicle type
     */
    @Query("SELECT vsr FROM VehicleServiceRecordEntity vsr " +
           "LEFT JOIN FETCH vsr.vehicleEntity v " +
           "LEFT JOIN FETCH v.vehicleTypeEntity vt " +
           "WHERE vsr.id = :recordId")
    Optional<VehicleServiceRecordEntity> findByIdWithVehicleAndVehicleType(@org.springframework.data.repository.query.Param("recordId") UUID recordId);
}
