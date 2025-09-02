package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleMaintenanceRequest;
import capstone_project.dtos.request.vehicle.VehicleMaintenanceRequest;
import capstone_project.dtos.response.vehicle.VehicleMaintenanceResponse;
import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.service.entityServices.vehicle.VehicleMaintenanceEntityService;
import capstone_project.service.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.mapper.vehicle.VehicleMaintenanceMapper;
import capstone_project.service.services.service.RedisService;
import capstone_project.service.services.vehicle.VehicleMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleMaintenanceServiceImpl implements VehicleMaintenanceService {

    private final VehicleMaintenanceEntityService entityService;
    private final VehicleMaintenanceMapper mapper;
    private final RedisService redis;
    private final VehicleEntityService vehicleEntityService;

    private static final String KEY_ALL = "vehicleMaintenance:all";
    private static final String KEY_ID  = "vehicleMaintenance:";   // + uuid
    private static final String VEHICLE_ALL_CACHE_KEY = "vehicles:all";
    private static final String VEHICLE_BY_ID_CACHE_KEY_PREFIX = "vehicle:";

    @Override
    public List<VehicleMaintenanceResponse> getAllMaintenance() {
        log.info("Fetching all vehicle mantenances");
        List<VehicleMaintenanceEntity> vehicleMaintenanceEntitíe = entityService.findAll();
        if (vehicleMaintenanceEntitíe.isEmpty()) {
            log.warn("No vehicle mantenance found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return vehicleMaintenanceEntitíe.stream()
                .map(mapper::toResponse)
                .toList();
    }


    @Override
    public VehicleMaintenanceResponse getMaintenanceById(UUID id) {
        log.info("Fetching vehicle rule by ID: {}", id);
        VehicleMaintenanceEntity vehicleRuleEntity = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return mapper.toResponse(vehicleRuleEntity);
    }

    @Override
    @Transactional
    public VehicleMaintenanceResponse createMaintenance(VehicleMaintenanceRequest req) {
        var maintenance = mapper.toEntity(req);

        var vehicleId = UUID.fromString(req.vehicleId());
        var vehicle = vehicleEntityService.findByVehicleId(vehicleId).orElseThrow(() ->
                new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
        vehicle.setStatus(VehicleStatusEnum.MAINTENANCE.name());
        vehicleEntityService.save(vehicle);

        var saved = entityService.save(maintenance);

        redis.delete(KEY_ALL);
        redis.save(KEY_ID + saved.getId(), saved);
        redis.delete(VEHICLE_ALL_CACHE_KEY);
        redis.delete(VEHICLE_BY_ID_CACHE_KEY_PREFIX + vehicleId);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleMaintenanceResponse updateMaintenance(UUID id, UpdateVehicleMaintenanceRequest req) {
        // Fetch existing entity
        var existing = entityService.findEntityById(id).orElseThrow(() ->
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