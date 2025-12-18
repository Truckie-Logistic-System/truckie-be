package capstone_project.repository.repositories.vehicle;

import capstone_project.entity.vehicle.VehicleReservationEntity;
import capstone_project.repository.repositories.common.BaseRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for Vehicle Reservation
 * Provides methods to check and manage vehicle reservations to prevent overbooking
 */
public interface VehicleReservationRepository extends BaseRepository<VehicleReservationEntity> {

    /**
     * Find all reservations for a specific vehicle on a specific date
     */
    List<VehicleReservationEntity> findByVehicleEntityIdAndTripDate(UUID vehicleId, LocalDate tripDate);

    /**
     * Find all reservations for a specific vehicle on a specific date with a specific status
     */
    List<VehicleReservationEntity> findByVehicleEntityIdAndTripDateAndStatus(UUID vehicleId, LocalDate tripDate, String status);

    /**
     * Check if a vehicle has any RESERVED reservation on a specific date (excluding a specific order)
     * Used to check availability when suggesting vehicles
     */
    @Query(value = """
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM vehicle_reservations r
        WHERE r.vehicle_id = :vehicleId
        AND r.trip_date = :tripDate
        AND r.status = 'RESERVED'
        AND r.order_id != :excludeOrderId
    """, nativeQuery = true)
    boolean existsReservedByVehicleAndDateExcludingOrder(
            @Param("vehicleId") UUID vehicleId,
            @Param("tripDate") LocalDate tripDate,
            @Param("excludeOrderId") UUID excludeOrderId);

    /**
     * Check if a vehicle has any RESERVED reservation on a specific date
     * Used to check availability when suggesting vehicles (no order exclusion)
     */
    @Query(value = """
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM vehicle_reservations r
        WHERE r.vehicle_id = :vehicleId
        AND r.trip_date = :tripDate
        AND r.status = 'RESERVED'
    """, nativeQuery = true)
    boolean existsReservedByVehicleAndDate(
            @Param("vehicleId") UUID vehicleId,
            @Param("tripDate") LocalDate tripDate);

    /**
     * Find reservation for a specific order
     */
    List<VehicleReservationEntity> findByOrderEntityId(UUID orderId);

    /**
     * Find reservation for a specific order with a specific status
     */
    Optional<VehicleReservationEntity> findByOrderEntityIdAndStatus(UUID orderId, String status);

    /**
     * Find all RESERVED reservations for a specific order
     */
    @Query("""
        SELECT r FROM VehicleReservationEntity r
        WHERE r.orderEntity.id = :orderId
        AND r.status = 'RESERVED'
    """)
    List<VehicleReservationEntity> findReservedByOrderId(@Param("orderId") UUID orderId);

    /**
     * Find reservation by vehicle, date, and order
     */
    Optional<VehicleReservationEntity> findByVehicleEntityIdAndTripDateAndOrderEntityId(
            UUID vehicleId, LocalDate tripDate, UUID orderId);
}
