package capstone_project.common.enums;

/**
 * Status enum for Vehicle Reservation
 * Used to prevent overbooking of the "last available vehicle" between multiple customers
 * 
 * Lifecycle:
 * - RESERVED: Vehicle is reserved for a specific order/contract on a specific date
 *             Created when customer confirms deposit payment
 * - CONSUMED: Reservation is consumed when staff creates VehicleAssignment
 * - CANCELLED: Reservation is cancelled when order/contract is cancelled or full payment deadline expires
 */
public enum VehicleReservationStatusEnum {
    RESERVED,   // Vehicle is reserved, not yet assigned
    CONSUMED,   // Reservation consumed - VehicleAssignment created
    CANCELLED   // Reservation cancelled - order cancelled or payment timeout
}
