package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
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
        var cached = redis.getList(KEY_ALL, VehicleAssignmentEntity.class);
        var list   = cached != null ? cached : entityService.findAll();
        if (list.isEmpty())
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        if (cached == null) redis.save(KEY_ALL, list);
        return list.stream().map(mapper::toResponse).toList();
    }

    @Override
    public VehicleAssignmentResponse getAssignmentById(UUID id) {
        var key = KEY_ID + id;
        var entity = redis.get(key, VehicleAssignmentEntity.class);
        if (entity == null) {
            entity = entityService.findContractRuleEntitiesById(id).orElseThrow(() ->
                    new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
            redis.save(key, entity);
        }
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
        var existing = entityService.findContractRuleEntitiesById(id).orElseThrow(() ->
                new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
        mapper.toEntity(req, existing);
        var updated = entityService.save(existing);
        redis.delete(KEY_ALL);
        redis.save(KEY_ID + updated.getId(), updated);
        return mapper.toResponse(updated);
    }
}