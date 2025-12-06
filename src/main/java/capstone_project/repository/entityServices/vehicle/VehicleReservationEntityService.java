package capstone_project.repository.entityServices.vehicle;

import capstone_project.entity.vehicle.VehicleReservationEntity;
import capstone_project.repository.entityServices.common.BaseEntityService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Entity Service for Vehicle Reservation
 * Provides methods to check and manage vehicle reservations to prevent overbooking
 */
public interface VehicleReservationEntityService extends BaseEntityService<VehicleReservationEntity, UUID> {

    /**
     * Find all reservations for a specific vehicle on a specific date
     */
    List<VehicleReservationEntity> findByVehicleAndDate(UUID vehicleId, LocalDate tripDate);

    /**
     * Find all reservations for a specific vehicle on a specific date with a specific status
     */
    List<VehicleReservationEntity> findByVehicleAndDateAndStatus(UUID vehicleId, LocalDate tripDate, String status);

    /**
     * Check if a vehicle has any RESERVED reservation on a specific date (excluding a specific order)
     * Used to check availability when suggesting vehicles
     */
    boolean existsReservedByVehicleAndDateExcludingOrder(UUID vehicleId, LocalDate tripDate, UUID excludeOrderId);

    /**
     * Check if a vehicle has any RESERVED reservation on a specific date
     * Used to check availability when suggesting vehicles (no order exclusion)
     */
    boolean existsReservedByVehicleAndDate(UUID vehicleId, LocalDate tripDate);

    /**
     * Find all reservations for a specific order
     */
    List<VehicleReservationEntity> findByOrderId(UUID orderId);

    /**
     * Find reservation for a specific order with a specific status
     */
    Optional<VehicleReservationEntity> findByOrderIdAndStatus(UUID orderId, String status);

    /**
     * Find all RESERVED reservations for a specific order
     */
    List<VehicleReservationEntity> findReservedByOrderId(UUID orderId);

    /**
     * Find reservation by vehicle, date, and order
     */
    Optional<VehicleReservationEntity> findByVehicleAndDateAndOrder(UUID vehicleId, LocalDate tripDate, UUID orderId);

    /**
     * Create a new reservation for a vehicle on a specific date for an order
     * @param vehicleId Vehicle ID
     * @param tripDate Trip date
     * @param orderId Order ID
     * @param contractId Contract ID (optional)
     * @param notes Notes (optional)
     * @return Created reservation entity
     */
    VehicleReservationEntity createReservation(UUID vehicleId, LocalDate tripDate, UUID orderId, UUID contractId, String notes);

    /**
     * Cancel all RESERVED reservations for an order
     * Called when order/contract is cancelled or full payment deadline expires
     */
    void cancelReservationsByOrderId(UUID orderId);

    /**
     * Consume a reservation (mark as CONSUMED)
     * Called when staff creates VehicleAssignment
     */
    void consumeReservation(UUID reservationId);
}
