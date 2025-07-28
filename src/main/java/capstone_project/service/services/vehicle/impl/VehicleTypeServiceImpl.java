package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.VehicleTypeRequest;
import capstone_project.dtos.response.vehicle.VehicleTypeResponse;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.service.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.vehicle.VehicleTypeMapper;
import capstone_project.service.services.service.RedisService;
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
    private final RedisService redisService;

    private static final String VEHICLE_TYPE_ALL_CACHE_KEY = "vehicleTypes:all";
    private static final String VEHICLE_TYPE_BY_ID_CACHE_KEY_PREFIX = "vehicleType:";

    @Override
    public List<VehicleTypeResponse> getAllVehicleTypes() {
        List<VehicleTypeEntity> cachedEntities = redisService.getList(
                VEHICLE_TYPE_ALL_CACHE_KEY, VehicleTypeEntity.class
        );

        List<VehicleTypeEntity> entities;

        if (cachedEntities != null) {
            entities = cachedEntities;
        } else {
            entities = vehicleTypeEntityService.findAll();
            if (entities.isEmpty()) {
                throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
            }

            redisService.save(VEHICLE_TYPE_ALL_CACHE_KEY, entities);
        }

        return entities.stream()
                .map(vehicleTypeMapper::toVehicleTypeResponse)
                .toList();
    }

    @Override
    public VehicleTypeResponse getVehicleTypeById(UUID id) {
        String cacheKey = VEHICLE_TYPE_BY_ID_CACHE_KEY_PREFIX + id;

        VehicleTypeEntity cachedEntity = redisService.get(cacheKey, VehicleTypeEntity.class);
        if (cachedEntity != null) {
            return vehicleTypeMapper.toVehicleTypeResponse(cachedEntity);
        }

        VehicleTypeEntity entity = vehicleTypeEntityService.findById(id)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));

        redisService.save(cacheKey, entity);
        return vehicleTypeMapper.toVehicleTypeResponse(entity);
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

        redisService.delete(VEHICLE_TYPE_ALL_CACHE_KEY);

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

        redisService.delete(VEHICLE_TYPE_ALL_CACHE_KEY);
        redisService.save(VEHICLE_TYPE_BY_ID_CACHE_KEY_PREFIX + existingVehicleType.getId(), existingVehicleType);

        return vehicleTypeMapper.toVehicleTypeResponse(updatedVehicleType);
    }

    @Override
    public void deleteVehicleType(UUID id) {

    }

}
