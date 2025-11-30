package capstone_project.repository.entityServices.order.order.impl;

import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService;
import capstone_project.repository.repositories.order.order.JourneyHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JourneyHistoryEntityServiceImpl implements JourneyHistoryEntityService {

    private final JourneyHistoryRepository journeyHistoryRepository;

    @Override
    public JourneyHistoryEntity save(JourneyHistoryEntity entity) {
        return journeyHistoryRepository.save(entity);
    }

    @Override
    public Optional<JourneyHistoryEntity> findEntityById(UUID uuid) {
        return journeyHistoryRepository.findById(uuid);
    }

    @Override
    public List<JourneyHistoryEntity> findAll() {
        return journeyHistoryRepository.findAll();
    }

    @Override
    public List<JourneyHistoryEntity> findByVehicleAssignmentId(UUID vehicleAssignmentId) {
        return journeyHistoryRepository.findByVehicleAssignment_Id(vehicleAssignmentId);
    }
    
    @Override
    public List<JourneyHistoryEntity> findByVehicleAssignmentIdSorted(UUID vehicleAssignmentId) {
        return journeyHistoryRepository.findByVehicleAssignment_IdOrderByCreatedAtDesc(vehicleAssignmentId);
    }
    
    @Override
    public Optional<JourneyHistoryEntity> findLatestActiveJourney(UUID vehicleAssignmentId) {
        // Find most recent ACTIVE or COMPLETED journey
        List<String> activeStatuses = Arrays.asList("ACTIVE", "COMPLETED");
        return journeyHistoryRepository.findFirstByVehicleAssignment_IdAndStatusInOrderByCreatedAtDesc(
                vehicleAssignmentId, 
                activeStatuses
        );
    }
    
    @Override
    public Optional<JourneyHistoryEntity> findLatestJourney(UUID vehicleAssignmentId) {
        return journeyHistoryRepository.findFirstByVehicleAssignment_IdOrderByCreatedAtDesc(vehicleAssignmentId);
    }
    
    @Override
    public void delete(JourneyHistoryEntity entity) {
        journeyHistoryRepository.delete(entity);
    }
    
    @Override
    public void deleteById(UUID id) {
        journeyHistoryRepository.deleteById(id);
    }
}
