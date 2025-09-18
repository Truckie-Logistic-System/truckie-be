package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.VehicleStatusEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleMaintenanceRequest;
import capstone_project.dtos.request.vehicle.VehicleMaintenanceRequest;
import capstone_project.dtos.response.vehicle.VehicleMaintenanceResponse;
import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.repository.entityServices.vehicle.VehicleMaintenanceEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.mapper.vehicle.VehicleMaintenanceMapper;
import capstone_project.service.services.redis.RedisService;
import capstone_project.service.services.vehicle.VehicleMaintenanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleMaintenanceServiceImpl implements VehicleMaintenanceService {

    private final VehicleMaintenanceEntityService entityService;
    private final VehicleMaintenanceMapper mapper;
    private final VehicleEntityService vehicleEntityService;

    @Override
    public List<VehicleMaintenanceResponse> getAllMaintenance() {
        log.info("Fetching all vehicle maintenances");
        return Optional.of(entityService.findAll())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new NotFoundException(
                        "There are no vehicle maintenances available.",
                        ErrorEnum.NOT_FOUND.getErrorCode()))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public VehicleMaintenanceResponse getMaintenanceById(UUID id) {
        log.info("Fetching vehicle maintenance by ID: {}", id);
        VehicleMaintenanceEntity entity = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        return mapper.toResponse(entity);
    }

    @Override
    @Transactional
    public VehicleMaintenanceResponse createMaintenance(VehicleMaintenanceRequest req) {
        log.info("Creating new vehicle maintenance");
        var maintenance = mapper.toEntity(req);

        var vehicleId = UUID.fromString(req.vehicleId());
        var vehicle = vehicleEntityService.findByVehicleId(vehicleId)
                .orElseThrow(() ->
                        new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),
                                ErrorEnum.NOT_FOUND.getErrorCode()));
        vehicle.setStatus(VehicleStatusEnum.MAINTENANCE.name());
        vehicleEntityService.save(vehicle);

        var saved = entityService.save(maintenance);
        return mapper.toResponse(saved);
    }

    @Override
    @Transactional
    public VehicleMaintenanceResponse updateMaintenance(UUID id, UpdateVehicleMaintenanceRequest req) {
        log.info("Updating vehicle maintenance with ID: {}", id);
        var existing = entityService.findEntityById(id).orElseThrow(() ->
                new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        mapper.toEntity(req, existing);

        var updated = entityService.save(existing);
        return mapper.toResponse(updated);
    }

}
