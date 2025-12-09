package capstone_project.service.services.penalty.impl;

import capstone_project.dtos.response.user.PenaltyHistoryResponse;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.repository.repositories.user.PenaltyHistoryRepository;
import capstone_project.service.services.penalty.PenaltyHistoryService;
import capstone_project.service.mapper.user.PenaltyHistoryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service("penaltyPenaltyHistoryServiceImpl")
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PenaltyHistoryServiceImpl implements PenaltyHistoryService {

    private final PenaltyHistoryRepository penaltyHistoryRepository;
    private final PenaltyHistoryMapper penaltyHistoryMapper;

    @Override
    @Transactional(readOnly = true)
    public List<PenaltyHistoryResponse> getAllPenalties() {
        log.info("Fetching all traffic violations");
        List<PenaltyHistoryEntity> penalties = penaltyHistoryRepository.findAll();
        
        return penalties.stream()
                .map(penaltyHistoryMapper::toPenaltyHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PenaltyHistoryResponse getPenaltyById(UUID penaltyId) {
        log.info("Fetching traffic violation by id: {}", penaltyId);
        PenaltyHistoryEntity penalty = penaltyHistoryRepository.findDetailById(penaltyId)
                .orElseThrow(() -> new RuntimeException("Traffic violation not found with id: " + penaltyId));
        
        return penaltyHistoryMapper.toPenaltyHistoryResponse(penalty);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PenaltyHistoryResponse> getPenaltiesByDriver(UUID driverId) {
        log.info("Fetching traffic violations for driver: {}", driverId);
        List<PenaltyHistoryEntity> penalties = penaltyHistoryRepository.findByDriverId(driverId);
        
        return penalties.stream()
                .map(penaltyHistoryMapper::toPenaltyHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<PenaltyHistoryResponse> getPenaltiesByVehicleAssignment(UUID vehicleAssignmentId) {
        log.info("Fetching traffic violations for vehicle assignment: {}", vehicleAssignmentId);
        List<PenaltyHistoryEntity> penalties = penaltyHistoryRepository.findByVehicleAssignmentId(vehicleAssignmentId);
        
        return penalties.stream()
                .map(penaltyHistoryMapper::toPenaltyHistoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public PenaltyHistoryResponse createPenalty(PenaltyHistoryEntity penalty) {
        log.info("Creating new traffic violation for driver: {}", penalty.getIssueBy().getId());
        PenaltyHistoryEntity savedPenalty = penaltyHistoryRepository.save(penalty);
        
        return penaltyHistoryMapper.toPenaltyHistoryResponse(savedPenalty);
    }

    @Override
    public PenaltyHistoryResponse updatePenalty(UUID penaltyId, PenaltyHistoryEntity penalty) {
        log.info("Updating traffic violation: {}", penaltyId);
        
        PenaltyHistoryEntity existingPenalty = penaltyHistoryRepository.findById(penaltyId)
                .orElseThrow(() -> new RuntimeException("Traffic violation not found with id: " + penaltyId));
        
        // Update fields
        existingPenalty.setViolationType(penalty.getViolationType());
        existingPenalty.setPenaltyDate(penalty.getPenaltyDate());
        existingPenalty.setTrafficViolationRecordImageUrl(penalty.getTrafficViolationRecordImageUrl());
        existingPenalty.setIssueBy(penalty.getIssueBy());
        existingPenalty.setVehicleAssignmentEntity(penalty.getVehicleAssignmentEntity());
        
        PenaltyHistoryEntity updatedPenalty = penaltyHistoryRepository.save(existingPenalty);
        
        return penaltyHistoryMapper.toPenaltyHistoryResponse(updatedPenalty);
    }

    @Override
    public void deletePenalty(UUID penaltyId) {
        log.info("Deleting traffic violation: {}", penaltyId);
        
        if (!penaltyHistoryRepository.existsById(penaltyId)) {
            throw new RuntimeException("Traffic violation not found with id: " + penaltyId);
        }
        
        penaltyHistoryRepository.deleteById(penaltyId);
    }
}
