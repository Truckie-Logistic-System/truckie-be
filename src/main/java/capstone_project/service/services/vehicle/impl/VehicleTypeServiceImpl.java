package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.VehicleTypeRequest;
import capstone_project.dtos.response.vehicle.VehicleTypeResponse;
import capstone_project.entity.order.order.FuelTypeEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;

import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.mapper.vehicle.VehicleTypeMapper;
import capstone_project.service.mapper.vehicle.VehicleTypeMapperHelper;
import capstone_project.service.services.vehicle.VehicleTypeService;
import capstone_project.repository.repositories.vehicle.FuelTypeRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.ArrayList;

/**
 * ...
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VehicleTypeServiceImpl implements VehicleTypeService {

    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final VehicleTypeMapper vehicleTypeMapper;
    private final VehicleTypeMapperHelper vehicleTypeMapperHelper;
    private final FuelTypeRepository fuelTypeRepository;

    @Override
    public List<VehicleTypeResponse> getAllVehicleTypes() {
        List<VehicleTypeEntity> entities = vehicleTypeEntityService.findAll();
        if (entities.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        return entities.stream()
                .map(entity -> {
                    long vehicleCount = vehicleEntityService.countByVehicleTypeId(entity.getId());
                    return vehicleTypeMapperHelper.toVehicleTypeResponseWithCount(entity, vehicleCount);
                })
                .toList();
    }

    @Override
    public VehicleTypeResponse getVehicleTypeById(UUID id) {
        VehicleTypeEntity entity = vehicleTypeEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
        long vehicleCount = vehicleEntityService.countByVehicleTypeId(id);
        return vehicleTypeMapperHelper.toVehicleTypeResponseWithCount(entity, vehicleCount);
    }

    @Override
    @Transactional
    public VehicleTypeResponse createVehicleType(VehicleTypeRequest vehicleTypeRequest) {
        
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

        VehicleTypeEntity existingVehicleType = vehicleTypeEntityService.findEntityById(id)
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
            throw new BadRequestException("Tên loại phương tiện đã tồn tại", ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        vehicleTypeMapper.toVehicleEntity(vehicleTypeRequest, existingVehicleType);
        VehicleTypeEntity updatedVehicleType = vehicleTypeEntityService.save(existingVehicleType);
        return vehicleTypeMapper.toVehicleTypeResponse(updatedVehicleType);
    }

    @Override
    public void deleteVehicleType(UUID id) {

    }

    @Override
    @Transactional
    public List<VehicleTypeResponse> assignDefaultFuelTypesForTruckRange() {

        FuelTypeEntity petrol = fuelTypeRepository.findByName("Xăng RON 95")
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        FuelTypeEntity diesel = fuelTypeRepository.findByName("Dầu Diesel")
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        List<String> petrolTypes = List.of(
                "TRUCK_0_5_TON",
                "TRUCK_1_25_TON"
        );

        List<String> dieselTypes = List.of(
                "TRUCK_1_9_TON",
                "TRUCK_2_4_TONN",
                "TRUCK_3_5_TON",
                "TRUCK_5_TON",
                "TRUCK_7_TON",
                "TRUCK_10_TON"
        );

        List<VehicleTypeEntity> allTypes = vehicleTypeEntityService.findAll();
        if (allTypes.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        List<VehicleTypeEntity> updated = new ArrayList<>();

        for (VehicleTypeEntity type : allTypes) {
            String name = type.getVehicleTypeName();
            if (name == null) {
                continue;
            }

            if (petrolTypes.contains(name)) {
                type.setFuelTypeEntity(petrol);
                updated.add(vehicleTypeEntityService.save(type));
            } else if (dieselTypes.contains(name)) {
                type.setFuelTypeEntity(diesel);
                updated.add(vehicleTypeEntityService.save(type));
            }
        }

        return updated.stream()
                .map(vehicleTypeMapper::toVehicleTypeResponse)
                .toList();
    }
}