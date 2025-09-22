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


}
