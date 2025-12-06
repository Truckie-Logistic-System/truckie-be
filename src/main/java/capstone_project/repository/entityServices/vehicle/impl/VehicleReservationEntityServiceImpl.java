package capstone_project.repository.entityServices.vehicle.impl;

import capstone_project.common.enums.VehicleReservationStatusEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleReservationEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleReservationEntityService;
import capstone_project.repository.repositories.vehicle.VehicleReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Implementation of VehicleReservationEntityService
 * Manages vehicle reservations to prevent overbooking
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleReservationEntityServiceImpl implements VehicleReservationEntityService {

    private final VehicleReservationRepository repository;
    private final VehicleEntityService vehicleEntityService;
    private final OrderEntityService orderEntityService;
    private final ContractEntityService contractEntityService;

    @Override
    public VehicleReservationEntity save(VehicleReservationEntity entity) {
        return repository.save(entity);
    }

    @Override
    public Optional<VehicleReservationEntity> findEntityById(UUID uuid) {
        return repository.findById(uuid);
    }

    @Override
    public List<VehicleReservationEntity> findAll() {
        return repository.findAll();
    }

    @Override
    public List<VehicleReservationEntity> findByVehicleAndDate(UUID vehicleId, LocalDate tripDate) {
        return repository.findByVehicleEntityIdAndTripDate(vehicleId, tripDate);
    }

    @Override
    public List<VehicleReservationEntity> findByVehicleAndDateAndStatus(UUID vehicleId, LocalDate tripDate, String status) {
        return repository.findByVehicleEntityIdAndTripDateAndStatus(vehicleId, tripDate, status);
    }

    @Override
    public boolean existsReservedByVehicleAndDateExcludingOrder(UUID vehicleId, LocalDate tripDate, UUID excludeOrderId) {
        return repository.existsReservedByVehicleAndDateExcludingOrder(vehicleId, tripDate, excludeOrderId);
    }

    @Override
    public boolean existsReservedByVehicleAndDate(UUID vehicleId, LocalDate tripDate) {
        return repository.existsReservedByVehicleAndDate(vehicleId, tripDate);
    }

    @Override
    public List<VehicleReservationEntity> findByOrderId(UUID orderId) {
        return repository.findByOrderEntityId(orderId);
    }

    @Override
    public Optional<VehicleReservationEntity> findByOrderIdAndStatus(UUID orderId, String status) {
        return repository.findByOrderEntityIdAndStatus(orderId, status);
    }

    @Override
    public List<VehicleReservationEntity> findReservedByOrderId(UUID orderId) {
        return repository.findReservedByOrderId(orderId);
    }

    @Override
    public Optional<VehicleReservationEntity> findByVehicleAndDateAndOrder(UUID vehicleId, LocalDate tripDate, UUID orderId) {
        return repository.findByVehicleEntityIdAndTripDateAndOrderEntityId(vehicleId, tripDate, orderId);
    }

    @Override
    @Transactional
    public VehicleReservationEntity createReservation(UUID vehicleId, LocalDate tripDate, UUID orderId, UUID contractId, String notes) {
        // Check if vehicle exists
        VehicleEntity vehicle = vehicleEntityService.findEntityById(vehicleId)
                .orElseThrow(() -> new NotFoundException("Vehicle not found: " + vehicleId, 404));

        // Check if order exists
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderId, 404));

        // Check if contract exists (optional)
        ContractEntity contract = null;
        if (contractId != null) {
            contract = contractEntityService.findEntityById(contractId).orElse(null);
        }

        // Check if reservation already exists for this vehicle/date/order
        Optional<VehicleReservationEntity> existing = findByVehicleAndDateAndOrder(vehicleId, tripDate, orderId);
        if (existing.isPresent()) {
            log.warn("Reservation already exists for vehicle={}, date={}, order={}", vehicleId, tripDate, orderId);
            return existing.get();
        }

        // Create new reservation
        VehicleReservationEntity reservation = VehicleReservationEntity.builder()
                .vehicleEntity(vehicle)
                .tripDate(tripDate)
                .orderEntity(order)
                .contractEntity(contract)
                .status(VehicleReservationStatusEnum.RESERVED.name())
                .notes(notes)
                .build();

        VehicleReservationEntity saved = repository.save(reservation);
        log.info("✅ Created vehicle reservation: vehicle={}, date={}, order={}, id={}", 
                vehicleId, tripDate, orderId, saved.getId());

        return saved;
    }

    @Override
    @Transactional
    public void cancelReservationsByOrderId(UUID orderId) {
        List<VehicleReservationEntity> reservations = findReservedByOrderId(orderId);
        
        for (VehicleReservationEntity reservation : reservations) {
            reservation.setStatus(VehicleReservationStatusEnum.CANCELLED.name());
            repository.save(reservation);
            log.info("🚫 Cancelled vehicle reservation: id={}, vehicle={}, date={}, order={}", 
                    reservation.getId(), 
                    reservation.getVehicleEntity().getId(), 
                    reservation.getTripDate(), 
                    orderId);
        }
    }

    @Override
    @Transactional
    public void consumeReservation(UUID reservationId) {
        VehicleReservationEntity reservation = findEntityById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found: " + reservationId, 404));

        reservation.setStatus(VehicleReservationStatusEnum.CONSUMED.name());
        repository.save(reservation);
        
        log.info("✅ Consumed vehicle reservation: id={}, vehicle={}, date={}, order={}", 
                reservationId, 
                reservation.getVehicleEntity().getId(), 
                reservation.getTripDate(), 
                reservation.getOrderEntity().getId());
    }
}
