package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleRequest;
import capstone_project.dtos.request.vehicle.VehicleRequest;
import capstone_project.dtos.response.vehicle.VehicleResponse;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.service.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.mapper.vehicle.VehicleMapper;
import capstone_project.service.services.service.RedisService;
import capstone_project.service.services.vehicle.VehicleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VehicleServiceImpl implements VehicleService {

    private final VehicleEntityService vehicleEntityService;
    private final VehicleMapper vehicleMapper;
    private final RedisService redis;

    private static final String CACHE_ALL = "vehicles:all";
    private static final String CACHE_BY_ID = "vehicle:";   // +id

    @Override
    public List<VehicleResponse> getAllVehicles() {
        List<VehicleEntity> cached = redis.getList(CACHE_ALL, VehicleEntity.class);
        List<VehicleEntity> list = cached != null ? cached : vehicleEntityService.findAll();
        if (cached == null) redis.save(CACHE_ALL, list);
        return list.stream().map(vehicleMapper::toVehicleResponse).toList();
    }

    @Override
    public VehicleResponse getVehicleById(UUID id) {
        VehicleEntity entity = redis.get(CACHE_BY_ID + id, VehicleEntity.class);
        if (entity == null)
            entity = vehicleEntityService.findContractRuleEntitiesById(id)
                    .orElseThrow(() -> new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
        redis.save(CACHE_BY_ID + id, entity);
        return vehicleMapper.toVehicleResponse(entity);
    }

    @Override @Transactional
    public VehicleResponse createVehicle(VehicleRequest req) {
        VehicleEntity entity = vehicleMapper.toVehicleEntity(req);
        VehicleEntity saved = vehicleEntityService.save(entity);
        redis.delete(CACHE_ALL);
        return vehicleMapper.toVehicleResponse(saved);
    }

    @Override @Transactional
    public VehicleResponse updateVehicle(UUID id, UpdateVehicleRequest req) {

        VehicleEntity existing = vehicleEntityService.findContractRuleEntitiesById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));

        // MapStruct fills in provided fields
        vehicleMapper.toVehicleEntity(req, existing);

        // If either coordinate was provided, update timestamp
        if (req.currentLatitude() != null || req.currentLongitude() != null) {
            existing.setLastUpdated(LocalDateTime.now());
        }

        VehicleEntity saved = vehicleEntityService.save(existing);
        redis.delete(CACHE_ALL);
        redis.save(CACHE_BY_ID + id, saved);
        return vehicleMapper.toVehicleResponse(saved);
    }

}