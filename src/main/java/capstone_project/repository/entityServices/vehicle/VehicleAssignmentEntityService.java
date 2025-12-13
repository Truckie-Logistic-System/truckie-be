package capstone_project.repository.entityServices.vehicle;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VehicleAssignmentEntityService extends BaseEntityService<VehicleAssignmentEntity, UUID> {
    List<VehicleAssignmentEntity> findByStatus(String status);

    List<VehicleAssignmentEntity> findByVehicleEntityId(UUID vehicleID);

    List<VehicleAssignmentEntity> findVehicleWithOrder(UUID type);

    List<VehicleAssignmentEntity> findVehicleAssignmentsWithOrderID(UUID orderID);

    /**
     * Optimized method for WebSocket tracking - eagerly loads all required relationships
     * @param orderID the order ID to find vehicle assignments for
     * @return list of vehicle assignments with eagerly loaded relationships
     */
    List<VehicleAssignmentEntity> findVehicleAssignmentsWithOrderIDOptimized(UUID orderID);

    Optional<VehicleAssignmentEntity> findVehicleAssignmentByVehicleEntityAndStatus(VehicleEntity vehicle, String status);

    List<VehicleAssignmentEntity> findAssignmentsByVehicleOrderByCreatedAtDesc(VehicleEntity vehicle);

    List<Object[]> countAssignmentsThisMonthForVehicles(List<UUID> vehicleIds,
                                                        LocalDateTime startOfMonth,
                                                        LocalDateTime endOfMonth);
    boolean existsActiveAssignmentForDriver(UUID driverId);
    /**
     * Đếm số chuyến đã hoàn thành mà tài xế là driver1
     * @param driverId ID của tài xế
     * @return Số chuyến đã hoàn thành
     */
    int countCompletedTripsAsDriver1(UUID driverId);
    /**
     * Đếm số chuyến đã hoàn thành mà tài xế là driver2
     * @param driverId ID của tài xế
     * @return Số chuyến đã hoàn thành
     */
    int countCompletedTripsAsDriver2(UUID driverId);

    /**
     * Tìm assignment gần nhất của tài xế (driver1)
     * @param driverId ID của tài xế
     * @return Assignment gần nhất hoặc empty nếu không tìm thấy
     */
    Optional<VehicleAssignmentEntity> findLatestAssignmentByDriver1Id(UUID driverId);

    /**
     * Tìm assignment gần nhất của tài xế (driver2)
     * @param driverId ID của tài xế
     * @return Assignment gần nhất hoặc empty nếu không tìm thấy
     */
    Optional<VehicleAssignmentEntity> findLatestAssignmentByDriver2Id(UUID driverId);

    /**
     * Tìm assignment gần nhất của tài xế (bất kể là driver1 hay driver2)
     * @param driverId ID của tài xế
     * @return Assignment gần nhất hoặc empty nếu không tìm thấy
     */
    Optional<VehicleAssignmentEntity> findLatestAssignmentByDriverId(UUID driverId);

    /**
     * Tìm tất cả các assignment của tài xế kể từ một thời điểm cụ thể
     * @param driverId ID của tài xế
     * @param cutoffDate Ngày bắt đầu tìm kiếm
     * @return Danh sách các assignment của tài xế từ cutoffDate đến hiện tại
     */
    List<VehicleAssignmentEntity> findAssignmentsForDriverSince(UUID driverId, LocalDateTime cutoffDate);

    /**
     * Find a vehicle assignment by ID
     * @param id the UUID of the vehicle assignment
     * @return Optional containing the vehicle assignment if found, or empty if not found
     */
    default Optional<VehicleAssignmentEntity> findById(UUID id) {
        return findEntityById(id);
    }
    
    /**
     * Find vehicle assignment by ID with eagerly loaded driver and device relationships
     * @param id the UUID of the vehicle assignment
     * @return Optional containing the vehicle assignment with eagerly loaded relationships if found, or empty if not found
     */
    Optional<VehicleAssignmentEntity> findByIdWithDriversAndDevices(UUID id);

    /**
     * Check if a driver has any vehicle assignment on a specific date (B3 - Hard constraint: 1 driver per trip per day)
     * This checks if the driver (as driver1 or driver2) has any assignment where the trip date matches the given date.
     * Trip date is determined from the first OrderDetail's estimatedStartTime in the assignment.
     * 
     * @param driverId ID của tài xế
     * @param tripDate Ngày chuyến (LocalDate)
     * @return true nếu tài xế đã có assignment trong ngày đó, false nếu không
     */
    boolean existsAssignmentForDriverOnDate(UUID driverId, LocalDate tripDate);
    
    /**
     * Find vehicle assignment by tracking code
     * @param trackingCode the tracking code to search for
     * @return Optional containing the vehicle assignment if found, or empty if not found
     */
    Optional<VehicleAssignmentEntity> findByTrackingCode(String trackingCode);
}
