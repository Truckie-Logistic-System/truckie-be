package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.VehicleTypeRequest;
import capstone_project.dtos.response.vehicle.VehicleTypeResponse;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.service.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.vehicle.VehicleTypeMapper;
import capstone_project.service.services.vehicle.VehicleTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final VehicleTypeMapper vehicleTypeMapper;

    @Override
    public List<VehicleTypeResponse> getAllVehicleTypes() {
        log.info("Fetching all vehicle types");
        List<VehicleTypeEntity> vehicleTypes = vehicleTypeEntityService.findAll();
        if (vehicleTypes.isEmpty()) {
            log.warn("No vehicle types found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return vehicleTypes.stream()
                .map(vehicleTypeMapper::toVehicleTypeResponse)
                .toList();
    }

    @Override
    public VehicleTypeResponse getVehicleTypeById(UUID id) {
        log.info("Fetching vehicle type with ID: {}", id);
        Optional<VehicleTypeEntity> vehicleTypeEntity = vehicleTypeEntityService.findById(id);

        return vehicleTypeEntity
                .map(vehicleTypeMapper::toVehicleTypeResponse)
                .orElseThrow(() -> {
                    log.warn("Role with ID {} not found", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });
    }

    @Override
    @Transactional
    public VehicleTypeResponse createVehicleType(VehicleTypeRequest vehicleTypeRequest) {
        log.info("Creating new vehicle type");
        Optional<VehicleTypeEntity> existingVehicleType = vehicleTypeEntityService.findByVehicleTypeName(vehicleTypeRequest.vehicleTypeName());

        if (existingVehicleType.isPresent()) {
            log.warn("Vehicle type with name {} already exists", vehicleTypeRequest.vehicleTypeName());
            throw new BadRequestException(
                    ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
            );
        }
        VehicleTypeEntity vehicleTypeEntity = vehicleTypeMapper.mapRequestToVehicleTypeEntity(vehicleTypeRequest);
        VehicleTypeEntity savedVehicleType = vehicleTypeEntityService.save(vehicleTypeEntity);
        return vehicleTypeMapper.toVehicleTypeResponse(savedVehicleType);
    }

    @Override
    @Transactional
    public VehicleTypeResponse updateVehicleType(UUID id, VehicleTypeRequest vehicleTypeRequest) {
        log.info("Updating vehicle type with ID: {}", id);

        VehicleTypeEntity existingVehicleType = vehicleTypeEntityService.findById(id)
                .orElseThrow(() -> {
                    log.warn("Vehicle type with ID {} not found", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        Optional<VehicleTypeEntity> vehicleWithSameName =
                vehicleTypeEntityService.findByVehicleTypeName(vehicleTypeRequest.vehicleTypeName());

        if (vehicleWithSameName.isPresent() && !vehicleWithSameName.get().getId().equals(id)) {
            throw new BadRequestException("Vehicle type name already exists", ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        vehicleTypeMapper.toVehicleEntity(vehicleTypeRequest, existingVehicleType);
        VehicleTypeEntity updatedVehicleType = vehicleTypeEntityService.save(existingVehicleType);
        return vehicleTypeMapper.toVehicleTypeResponse(updatedVehicleType);
    }

    @Override
    public void deleteVehicleType(UUID id) {

    }

}
