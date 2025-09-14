package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleRequest;
import capstone_project.dtos.request.vehicle.VehicleRequest;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleGetDetailsResponse;
import capstone_project.dtos.response.vehicle.VehicleMaintenanceResponse;
import capstone_project.dtos.response.vehicle.VehicleResponse;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.entity.vehicle.VehicleMaintenanceEntity;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleMaintenanceEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import capstone_project.service.mapper.vehicle.VehicleMaintenanceMapper;
import capstone_project.service.mapper.vehicle.VehicleMapper;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import capstone_project.service.services.vehicle.VehicleMaintenanceService;
import capstone_project.service.services.vehicle.VehicleService;
import capstone_project.service.services.vehicle.VehicleTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleEntityService vehicleEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final VehicleMaintenanceEntityService vehicleMaintenanceEntityService;
    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final VehicleMapper vehicleMapper;
    private final VehicleAssignmentMapper vehicleAssignmentMapper;
    private final VehicleMaintenanceMapper vehicleMaintenanceMapper;

    @Override
    public List<VehicleResponse> getAllVehicles() {
        log.info("Fetching all vehicles");

        List<VehicleEntity> entities = vehicleEntityService.findAll();
        if (entities.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        return entities.stream()
                .map(vehicleMapper::toVehicleResponse)
                .toList();
    }

    @Override
    public VehicleGetDetailsResponse getVehicleById(UUID id) {
        VehicleEntity entity = vehicleEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.warn("Vehicle with ID {} not found", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        List<VehicleAssignmentEntity> vha =
                vehicleAssignmentEntityService.findByVehicleEntityId(entity.getId());

        // map vehicle entity first
        VehicleGetDetailsResponse response = vehicleMapper.toVehicleDetailResponse(entity);

        // map assignments and attach to response
        List<VehicleAssignmentResponse> assignmentResponses = vha.stream()
                .map(vehicleAssignmentMapper::toResponse)
                .toList();

        response.setVehicleAssignmentResponse(assignmentResponses);

        List<VehicleMaintenanceEntity> vhm  =
                vehicleMaintenanceEntityService.findByVehicleEntityId(entity.getId());

        List<VehicleMaintenanceResponse> maintenanceResponses = vhm.stream()
                .map(vehicleMaintenanceMapper::toResponse)
                .toList();

        response.setVehicleMaintenanceResponse(maintenanceResponses);

        return response;
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        log.info("Creating new vehicle");

        VehicleEntity vehicleEntity = vehicleMapper.toVehicleEntity(request);
        VehicleEntity savedVehicle = vehicleEntityService.save(vehicleEntity);

        return vehicleMapper.toVehicleResponse(savedVehicle);
    }

    @Override
    @Transactional
    public VehicleResponse updateVehicle(UUID id, UpdateVehicleRequest request) {
        log.info("Updating vehicle with ID: {}", id);

        VehicleEntity existingVehicle = vehicleEntityService.findByVehicleId(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if (request.licensePlateNumber() != null) {
            vehicleEntityService.findByLicensePlateNumber(request.licensePlateNumber())
                    .filter(v -> !v.getId().equals(id))
                    .ifPresent(v -> {
                        throw new BadRequestException(
                                "License plate number already exists",
                                ErrorEnum.ALREADY_EXISTED.getErrorCode()
                        );
                    });
        }

        vehicleMapper.toVehicleEntity(request, existingVehicle);

        if (request.currentLatitude() != null || request.currentLongitude() != null) {
            existingVehicle.setLastUpdated(LocalDateTime.now());
        }

        VehicleEntity updatedVehicle = vehicleEntityService.save(existingVehicle);

        return vehicleMapper.toVehicleResponse(updatedVehicle);
    }

}
