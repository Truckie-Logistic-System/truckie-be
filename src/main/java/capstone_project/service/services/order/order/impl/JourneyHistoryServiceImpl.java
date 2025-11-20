package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.JourneyHistoryRequest;
import capstone_project.dtos.request.order.UpdateJourneyHistoryRequest;
import capstone_project.dtos.response.order.JourneyHistoryResponse;
import capstone_project.entity.order.order.JourneyHistoryEntity;
import capstone_project.dtos.response.order.JourneyHistoryResponse;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService;
import capstone_project.repository.repositories.order.order.JourneyHistoryRepository;
import capstone_project.service.mapper.order.JourneyHistoryMapper;
import capstone_project.service.services.order.order.JourneyHistoryService;

import capstone_project.service.services.redis.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JourneyHistoryServiceImpl implements JourneyHistoryService {

    private final JourneyHistoryEntityService entityService;
    private final JourneyHistoryRepository repository;
    private final JourneyHistoryMapper mapper;
    private final RedisService redis;

    private static final String KEY_ALL = "journeyHistory:all";
    private static final String KEY_ID  = "journeyHistory:"; // + uuid

    @Override
    public List<JourneyHistoryResponse> getAll() {
        
        List<JourneyHistoryEntity> entities = entityService.findAll();
        if (entities.isEmpty()) {
            log.warn("No journey histories found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return entities.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public JourneyHistoryResponse getById(UUID id) {
        
        JourneyHistoryEntity entity = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public JourneyHistoryResponse create(JourneyHistoryRequest req) {
        
        JourneyHistoryEntity entity = mapper.toEntity(req);
        JourneyHistoryEntity saved = entityService.save(entity);

        JourneyHistoryResponse response = mapper.toResponse(saved);

        redis.delete(KEY_ALL);
        redis.save(KEY_ID + saved.getId(), response);

        return response;
    }

    @Override
    @Transactional
    public JourneyHistoryResponse update(UUID id, UpdateJourneyHistoryRequest req) {
        
        JourneyHistoryEntity entity = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        mapper.toEntity(req, entity);
        JourneyHistoryEntity updated = entityService.save(entity);
        JourneyHistoryResponse response = mapper.toResponse(updated);

        redis.delete(KEY_ALL);
        redis.save(KEY_ID + id, response);

        return response;
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        
        if (!repository.existsById(id)) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        repository.deleteById(id);

        redis.delete(KEY_ALL);
        redis.delete(KEY_ID + id);
    }

    @Override
    public List<JourneyHistoryResponse> getByVehicleAssignmentId(UUID vehicleAssignmentId) {
        List<JourneyHistoryEntity> entities = entityService.findByVehicleAssignmentId(vehicleAssignmentId);
        if (entities.isEmpty()) {
            log.warn("No journey history found for vehicleAssignmentId: {}", vehicleAssignmentId);
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return entities.stream()
                .map(mapper::toResponse)
                .toList();
    }
    
    @Override
    public List<JourneyHistoryResponse> getByVehicleAssignmentIdSorted(UUID vehicleAssignmentId) {
        List<JourneyHistoryEntity> entities = entityService.findByVehicleAssignmentIdSorted(vehicleAssignmentId);
        if (entities.isEmpty()) {
            log.warn("No journey history found for vehicleAssignmentId: {}", vehicleAssignmentId);
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        
        return entities.stream()
                .map(mapper::toResponse)
                .toList();
    }
    
    @Override
    public java.util.Optional<JourneyHistoryResponse> getLatestActiveJourney(UUID vehicleAssignmentId) {
        return entityService.findLatestActiveJourney(vehicleAssignmentId)
                .map(entity -> {
                    
                    return mapper.toResponse(entity);
                });
    }
}
