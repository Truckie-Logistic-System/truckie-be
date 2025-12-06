package capstone_project.service.services.vehicle;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing Vehicle Reservations
 * Used to prevent overbooking of the "last available vehicle" between multiple customers
 * 
 * Reservation lifecycle:
 * - Created (RESERVED) when customer confirms deposit payment
 * - Consumed (CONSUMED) when staff creates VehicleAssignment
 * - Cancelled (CANCELLED) when order/contract is cancelled or full payment deadline expires
 */
public interface VehicleReservationService {

    /**
     * Create reservations for vehicles based on order's suggested assignments
     * Called when customer confirms deposit payment
     * 
     * @param orderId Order ID
     * @param vehicleIds List of vehicle IDs to reserve
     * @param tripDate Trip date
     * @param notes Optional notes
     */
    void createReservationsForOrder(UUID orderId, List<UUID> vehicleIds, LocalDate tripDate, String notes);

    /**
     * Cancel all reservations for an order
     * Called when order/contract is cancelled or full payment deadline expires
     * 
     * @param orderId Order ID
     */
    void cancelReservationsForOrder(UUID orderId);

    /**
     * Consume reservations for an order
     * Called when staff creates VehicleAssignment
     * 
     * @param orderId Order ID
     */
    void consumeReservationsForOrder(UUID orderId);

    /**
     * Check if a vehicle is available (not reserved by other orders) on a specific date
     * 
     * @param vehicleId Vehicle ID
     * @param tripDate Trip date
     * @param excludeOrderId Order ID to exclude from check (optional)
     * @return true if vehicle is available, false if reserved
     */
    boolean isVehicleAvailable(UUID vehicleId, LocalDate tripDate, UUID excludeOrderId);
}
