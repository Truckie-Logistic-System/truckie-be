package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleMaintenanceRequest;
import capstone_project.dtos.request.vehicle.VehicleMaintenanceRequest;
import capstone_project.dtos.response.vehicle.VehicleMaintenanceResponse;
import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.service.entityServices.vehicle.VehicleMaintenanceEntityService;
import capstone_project.service.mapper.vehicle.VehicleMaintenanceMapper;
import capstone_project.service.services.service.RedisService;
import capstone_project.service.services.vehicle.VehicleMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleMaintenanceServiceImpl implements VehicleMaintenanceService {

    private final VehicleMaintenanceEntityService entityService;
    private final VehicleMaintenanceMapper mapper;
    private final RedisService redis;

    private static final String KEY_ALL = "vehicleMaintenance:all";
    private static final String KEY_ID  = "vehicleMaintenance:";   // + uuid

    @Override
    public List<VehicleMaintenanceResponse> getAllMaintenance() {
        var cached = redis.getList(KEY_ALL, VehicleMaintenanceEntity.class);
        var list   = cached != null ? cached : entityService.findAll();
        if (list.isEmpty())
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        if (cached == null) redis.save(KEY_ALL, list);
        return list.stream().map(mapper::toResponse).toList();
    }

    @Override
    public VehicleMaintenanceResponse getMaintenanceById(UUID id) {
        var key = KEY_ID + id;
        var entity = redis.get(key, VehicleMaintenanceEntity.class);
        if (entity == null) {
            entity = entityService.findContractRuleEntitiesById(id).orElseThrow(() ->
                    new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
            redis.save(key, entity);
        }
        return mapper.toResponse(entity);
    }

    @Override
    public VehicleMaintenanceResponse createMaintenance(VehicleMaintenanceRequest req) {
        var saved = entityService.save(mapper.toEntity(req));
        redis.delete(KEY_ALL);
        redis.save(KEY_ID + saved.getId(), saved);
        return mapper.toResponse(saved);
    }

    @Override
    public VehicleMaintenanceResponse updateMaintenance(UUID id, UpdateVehicleMaintenanceRequest req) {
        var existing = entityService.findContractRuleEntitiesById(id).orElseThrow(() ->
                new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
        mapper.toEntity(req, existing);
        var updated = entityService.save(existing);
        redis.delete(KEY_ALL);
        redis.save(KEY_ID + updated.getId(), updated);
        return mapper.toResponse(updated);
    }
}