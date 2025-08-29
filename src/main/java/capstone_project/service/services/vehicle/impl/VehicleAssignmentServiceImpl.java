package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleResponse;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.service.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import capstone_project.service.services.service.RedisService;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleAssignmentServiceImpl implements VehicleAssignmentService {

    private final VehicleAssignmentEntityService entityService;
    private final VehicleAssignmentMapper mapper;
    private final RedisService redis;

    private static final String KEY_ALL = "vehicleAssignment:all";
    private static final String KEY_ID  = "vehicleAssignment:";   // + uuid

    @Override
    public List<VehicleAssignmentResponse> getAllAssignments() {
        log.info("Fetching all vehicles");

        List<VehicleAssignmentResponse> cachedResponses = redis.getList(
                KEY_ALL, VehicleAssignmentResponse.class
        );

        if (cachedResponses != null) {
            log.info("Returning cached vehicles");
            return cachedResponses;
        }

        log.info("No cached vehicles found, fetching from database");
        List<VehicleAssignmentEntity> entities = entityService.findAll();
        if (entities.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        List<VehicleAssignmentResponse> responses = entities.stream()
                .map(mapper::toResponse)
                .toList();

        redis.save(KEY_ALL, responses);

        return responses;
    }

    @Override
    public VehicleAssignmentResponse getAssignmentById(UUID id) {
        log.info("Fetching vehicle assingment by ID: {}", id);
        VehicleAssignmentEntity entity = entityService.findContractRuleEntitiesById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return mapper.toResponse(entity);
    }

    @Override
    public VehicleAssignmentResponse createAssignment(VehicleAssignmentRequest req) {
        var saved = entityService.save(mapper.toEntity(req));
        redis.delete(KEY_ALL);
        redis.save(KEY_ID + saved.getId(), saved);
        return mapper.toResponse(saved);
    }

    @Override
    public VehicleAssignmentResponse updateAssignment(UUID id, UpdateVehicleAssignmentRequest req) {
        // Fetch existing entity
        var existing = entityService.findContractRuleEntitiesById(id).orElseThrow(() ->
                new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));

        // Map request into existing entity
        mapper.toEntity(req, existing);

        // Save updated entity
        var updated = entityService.save(existing);

        // Map to response (DTO) before caching
        var response = mapper.toResponse(updated);

        // Invalidate and refresh cache
        redis.delete(KEY_ALL);
        redis.save(KEY_ID + id, response);

        return response;
    }
}