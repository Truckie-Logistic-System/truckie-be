package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.VehicleReservationStatusEnum;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.vehicle.VehicleReservationEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleReservationEntityService;
import capstone_project.service.services.vehicle.VehicleReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of VehicleReservationService
 * Manages vehicle reservations to prevent overbooking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleReservationServiceImpl implements VehicleReservationService {

    private final VehicleReservationEntityService vehicleReservationEntityService;
    private final ContractEntityService contractEntityService;

    @Override
    @Transactional
    public void createReservationsForOrder(UUID orderId, List<UUID> vehicleIds, LocalDate tripDate, String notes) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            log.warn("No vehicle IDs provided for reservation, orderId={}", orderId);
            return;
        }

        // Get contract ID if exists
        UUID contractId = null;
        try {
            ContractEntity contract = contractEntityService.getContractByOrderId(orderId).orElse(null);
            if (contract != null) {
                contractId = contract.getId();
            }
        } catch (Exception e) {
            log.warn("Could not get contract for order {}: {}", orderId, e.getMessage());
        }

        for (UUID vehicleId : vehicleIds) {
            try {
                // Check if reservation already exists
                if (vehicleReservationEntityService.findByVehicleAndDateAndOrder(vehicleId, tripDate, orderId).isPresent()) {
                    log.info("Reservation already exists for vehicle={}, date={}, order={}", vehicleId, tripDate, orderId);
                    continue;
                }

                // Check if vehicle is reserved by another order
                if (vehicleReservationEntityService.existsReservedByVehicleAndDateExcludingOrder(vehicleId, tripDate, orderId)) {
                    log.warn("⚠️ Vehicle {} is already reserved on {} by another order, skipping", vehicleId, tripDate);
                    continue;
                }

                // Create reservation
                vehicleReservationEntityService.createReservation(vehicleId, tripDate, orderId, contractId, notes);
                
            } catch (Exception e) {
                log.error("❌ Failed to create reservation for vehicle={}, order={}: {}", vehicleId, orderId, e.getMessage());
            }
        }
    }

    @Override
    @Transactional
    public void cancelReservationsForOrder(UUID orderId) {
        try {
            vehicleReservationEntityService.cancelReservationsByOrderId(orderId);
            log.info("✅ Cancelled all reservations for order {}", orderId);
        } catch (Exception e) {
            log.error("❌ Failed to cancel reservations for order {}: {}", orderId, e.getMessage());
        }
    }

    @Override
    @Transactional
    public void consumeReservationsForOrder(UUID orderId) {
        try {
            List<VehicleReservationEntity> reservations = vehicleReservationEntityService.findReservedByOrderId(orderId);
            
            for (VehicleReservationEntity reservation : reservations) {
                reservation.setStatus(VehicleReservationStatusEnum.CONSUMED.name());
                vehicleReservationEntityService.save(reservation);
                log.info("✅ Consumed reservation {} for order {}", reservation.getId(), orderId);
            }
        } catch (Exception e) {
            log.error("❌ Failed to consume reservations for order {}: {}", orderId, e.getMessage());
        }
    }

    @Override
    public boolean isVehicleAvailable(UUID vehicleId, LocalDate tripDate, UUID excludeOrderId) {
        if (excludeOrderId != null) {
            return !vehicleReservationEntityService.existsReservedByVehicleAndDateExcludingOrder(vehicleId, tripDate, excludeOrderId);
        } else {
            return !vehicleReservationEntityService.existsReservedByVehicleAndDate(vehicleId, tripDate);
        }
    }
}
