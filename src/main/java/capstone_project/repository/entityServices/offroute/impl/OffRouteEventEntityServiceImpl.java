package capstone_project.repository.entityServices.offroute.impl;

import capstone_project.common.enums.OffRouteWarningStatus;
import capstone_project.entity.offroute.OffRouteEventEntity;
import capstone_project.repository.entityServices.offroute.OffRouteEventEntityService;
import capstone_project.repository.repositories.offroute.OffRouteEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OffRouteEventEntityServiceImpl implements OffRouteEventEntityService {

    private final OffRouteEventRepository offRouteEventRepository;

    @Override
    public OffRouteEventEntity save(OffRouteEventEntity entity) {
        return offRouteEventRepository.save(entity);
    }

    @Override
    public Optional<OffRouteEventEntity> findEntityById(UUID id) {
        return offRouteEventRepository.findById(id);
    }

    @Override
    public List<OffRouteEventEntity> findAll() {
        return offRouteEventRepository.findAll();
    }

    @Override
    public Optional<OffRouteEventEntity> findActiveByVehicleAssignmentId(UUID vehicleAssignmentId) {
        return offRouteEventRepository.findActiveByVehicleAssignmentId(vehicleAssignmentId);
    }

    @Override
    public List<OffRouteEventEntity> findAllActiveEvents() {
        return offRouteEventRepository.findAllActiveEvents();
    }

    @Override
    public List<OffRouteEventEntity> findByWarningStatus(OffRouteWarningStatus status) {
        return offRouteEventRepository.findByWarningStatus(status);
    }

    @Override
    public List<OffRouteEventEntity> findByOrderId(UUID orderId) {
        return offRouteEventRepository.findByOrderId(orderId);
    }

    @Override
    public List<OffRouteEventEntity> findByVehicleAssignmentId(UUID vehicleAssignmentId) {
        return offRouteEventRepository.findByVehicleAssignmentId(vehicleAssignmentId);
    }

    @Override
    public Optional<OffRouteEventEntity> findByIdWithFullDetails(UUID eventId) {
        return offRouteEventRepository.findByIdWithFullDetails(eventId);
    }

    @Override
    public long countActiveByOrderId(UUID orderId) {
        return offRouteEventRepository.countActiveByOrderId(orderId);
    }

    @Override
    public Optional<OffRouteEventEntity> findByIssueId(UUID issueId) {
        return offRouteEventRepository.findByIssueId(issueId);
    }

    @Override
    public List<OffRouteEventEntity> findByWarningStatusAndGracePeriodExpiresBefore(
        OffRouteWarningStatus status, 
        java.time.LocalDateTime currentTime
    ) {
        return offRouteEventRepository.findByWarningStatusAndGracePeriodExpiresBefore(status, currentTime);
    }
}
