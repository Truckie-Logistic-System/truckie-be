package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
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
@Slf4j
@RequiredArgsConstructor
public class VehicleServiceImpl implements VehicleService {

    private final VehicleEntityService vehicleEntityService;
    private final VehicleMapper vehicleMapper;
    private final RedisService redisService;

    private static final String VEHICLE_ALL_CACHE_KEY = "vehicles:all";
    private static final String VEHICLE_BY_ID_CACHE_KEY_PREFIX = "vehicle:";

    @Override
    public List<VehicleResponse> getAllVehicles() {
        log.info("Fetching all vehicles");

        List<VehicleResponse> cachedResponses = redisService.getList(
                VEHICLE_ALL_CACHE_KEY, VehicleResponse.class
        );

        if (cachedResponses != null) {
            log.info("Returning cached vehicles");
            return cachedResponses;
        }

        log.info("No cached vehicles found, fetching from database");
        List<VehicleEntity> entities = vehicleEntityService.findAll();
        if (entities.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        List<VehicleResponse> responses = entities.stream()
                .map(vehicleMapper::toVehicleResponse)
                .toList();

        redisService.save(VEHICLE_ALL_CACHE_KEY, responses);

        return responses;
    }


    @Override
    public VehicleResponse getVehicleById(UUID id) {
        String cacheKey = VEHICLE_BY_ID_CACHE_KEY_PREFIX + id;

        VehicleResponse cachedResponse = redisService.get(cacheKey, VehicleResponse.class);
        if (cachedResponse != null) {
            log.info("Returning cached vehicle with ID: {}", id);
            return cachedResponse;
        }

        VehicleEntity entity = vehicleEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.warn("Vehicle with ID {} not found", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        VehicleResponse response = vehicleMapper.toVehicleResponse(entity);

        redisService.save(cacheKey, response);

        return response;
    }

    @Override
    @Transactional
    public VehicleResponse createVehicle(VehicleRequest request) {
        log.info("Creating new vehicle");

        VehicleEntity vehicleEntity = vehicleMapper.toVehicleEntity(request);
        VehicleEntity savedVehicle = vehicleEntityService.save(vehicleEntity);

        redisService.delete(VEHICLE_ALL_CACHE_KEY);

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

        redisService.delete(VEHICLE_ALL_CACHE_KEY);
        redisService.save(VEHICLE_BY_ID_CACHE_KEY_PREFIX + id, updatedVehicle);

        return vehicleMapper.toVehicleResponse(updatedVehicle);
    }

}
