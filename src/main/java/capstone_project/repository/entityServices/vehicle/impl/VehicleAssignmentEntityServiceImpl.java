package capstone_project.repository.entityServices.vehicle.impl;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.repositories.vehicle.VehicleAssignmentRepository;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VehicleAssignmentEntityServiceImpl implements VehicleAssignmentEntityService {

    private final VehicleAssignmentRepository vehicleAssignmentRepository;

    @Override
    public VehicleAssignmentEntity save(VehicleAssignmentEntity entity) {
        return vehicleAssignmentRepository.save(entity);
    }

    @Override
    public Optional<VehicleAssignmentEntity> findEntityById(UUID uuid) {
        return vehicleAssignmentRepository.findById(uuid);
    }

    @Override
    public List<VehicleAssignmentEntity> findAll() {
        return vehicleAssignmentRepository.findAll();
    }

    @Override
    public List<VehicleAssignmentEntity> findByStatus(String status) {
        return vehicleAssignmentRepository.findByStatus(status);
    }

    @Override
    public List<VehicleAssignmentEntity> findByVehicleEntityId(UUID vehicleID) {
        return vehicleAssignmentRepository.findByVehicleEntityId(vehicleID);
    }

    @Override
    public List<VehicleAssignmentEntity> findVehicleWithOrder(UUID vehicleType ) {
        return vehicleAssignmentRepository.findAssignmentsOrderByActiveCountAscAndVehicleType(vehicleType);
    }

    @Override
    public List<VehicleAssignmentEntity> findVehicleAssignmentsWithOrderID(UUID orderID) {
        return vehicleAssignmentRepository.findVehicleAssignmentsWithOrderID(orderID);
    }

    @Override
    public Optional<VehicleAssignmentEntity> findVehicleAssignmentByVehicleEntityAndStatus(VehicleEntity vehicle, String status) {
        return vehicleAssignmentRepository.findVehicleAssignmentByVehicleEntityAndStatus(vehicle,status);
    }

    @Override
    public List<VehicleAssignmentEntity> findAssignmentsByVehicleOrderByCreatedAtDesc(VehicleEntity vehicle) {
        return vehicleAssignmentRepository.findAssignmentsByVehicleOrderByCreatedAtDesc(vehicle);
    }

    @Override
    public  List<Object[]> countAssignmentsThisMonthForVehicles(List<UUID> vehicleIds,
                                                                LocalDateTime startOfMonth,
                                                                LocalDateTime endOfMonth) {
        return vehicleAssignmentRepository.countAssignmentsThisMonthForVehicles(vehicleIds,startOfMonth,endOfMonth);
    }

    @Override
    public boolean existsActiveAssignmentForDriver(UUID driverId) {
        return vehicleAssignmentRepository.existsByDriver1IdAndStatus(driverId, "ACTIVE") ||
               vehicleAssignmentRepository.existsByDriver2IdAndStatus(driverId, "ACTIVE");
    }

    @Override
    public int countCompletedTripsAsDriver1(UUID driverId) {
        return vehicleAssignmentRepository.countCompletedTripsAsDriver1(driverId);
    }

    @Override
    public int countCompletedTripsAsDriver2(UUID driverId) {
        return vehicleAssignmentRepository.countCompletedTripsAsDriver2(driverId);
    }

    @Override
    public Optional<VehicleAssignmentEntity> findLatestAssignmentByDriver1Id(UUID driverId) {
        return vehicleAssignmentRepository.findLatestAssignmentByDriver1Id(driverId);
    }

    @Override
    public Optional<VehicleAssignmentEntity> findLatestAssignmentByDriver2Id(UUID driverId) {
        return vehicleAssignmentRepository.findLatestAssignmentByDriver2Id(driverId);
    }

    @Override
    public Optional<VehicleAssignmentEntity> findLatestAssignmentByDriverId(UUID driverId) {
        // Tìm assignment gần nhất mà tài xế là driver1
        Optional<VehicleAssignmentEntity> asDriver1 = findLatestAssignmentByDriver1Id(driverId);

        // Tìm assignment gần nhất mà tài xế là driver2
        Optional<VehicleAssignmentEntity> asDriver2 = findLatestAssignmentByDriver2Id(driverId);

        // Nếu không có assignment nào, trả về empty
        if (asDriver1.isEmpty() && asDriver2.isEmpty()) {
            return Optional.empty();
        }

        // Nếu chỉ có một trong hai, trả về cái đó
        if (asDriver1.isEmpty()) {
            return asDriver2;
        }
        if (asDriver2.isEmpty()) {
            return asDriver1;
        }

        // Nếu cả hai đều có, trả về cái gần nhất
        VehicleAssignmentEntity assignment1 = asDriver1.get();
        VehicleAssignmentEntity assignment2 = asDriver2.get();

        if (assignment1.getCreatedAt().isAfter(assignment2.getCreatedAt())) {
            return asDriver1;
        } else {
            return asDriver2;
        }
    }

    @Override
    public List<VehicleAssignmentEntity> findAssignmentsForDriverSince(UUID driverId, LocalDateTime cutoffDate) {
        return vehicleAssignmentRepository.findAssignmentsForDriverSince(driverId, cutoffDate);
    }
}
