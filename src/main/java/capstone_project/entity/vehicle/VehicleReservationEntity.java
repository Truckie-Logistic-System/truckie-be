package capstone_project.entity.vehicle;

import capstone_project.entity.common.BaseEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;

/**
 * Entity for Vehicle Reservation
 * Used to prevent overbooking of the "last available vehicle" between multiple customers
 * 
 * When a customer confirms deposit payment, a reservation is created to "lock" the vehicle
 * for that specific date. This prevents other orders from using the same vehicle on the same day.
 * 
 * Reservation lifecycle:
 * - Created (RESERVED) when customer confirms deposit payment
 * - Consumed (CONSUMED) when staff creates VehicleAssignment
 * - Cancelled (CANCELLED) when order/contract is cancelled or full payment deadline expires
 */
@Entity
@Table(name = "vehicle_reservations", schema = "public", catalog = "capstone-project",
       indexes = {
           @Index(name = "idx_vehicle_reservation_vehicle_date", columnList = "vehicle_id, trip_date"),
           @Index(name = "idx_vehicle_reservation_order", columnList = "order_id"),
           @Index(name = "idx_vehicle_reservation_status", columnList = "status")
       })
@Data
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class VehicleReservationEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private VehicleEntity vehicleEntity;

    @Column(name = "trip_date", nullable = false)
    private LocalDate tripDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private OrderEntity orderEntity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contract_id")
    private ContractEntity contractEntity;

    @Size(max = 20)
    @Column(name = "status", length = 20, nullable = false)
    private String status;

    @Size(max = 500)
    @Column(name = "notes", length = 500)
    private String notes;
}
