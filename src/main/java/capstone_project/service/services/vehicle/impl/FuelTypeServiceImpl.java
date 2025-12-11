package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.CreateFuelTypeRequest;
import capstone_project.dtos.request.vehicle.UpdateFuelTypeRequest;
import capstone_project.dtos.response.vehicle.FuelTypeResponse;
import capstone_project.entity.order.order.FuelTypeEntity;
import capstone_project.repository.repositories.vehicle.FuelTypeRepository;
import capstone_project.service.services.vehicle.FuelTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FuelTypeServiceImpl implements FuelTypeService {

    private final FuelTypeRepository fuelTypeRepository;

    @Override
    public List<FuelTypeResponse> getAllFuelTypes() {
        List<FuelTypeEntity> entities = fuelTypeRepository.findAll();
        
        // Sort by createdAt DESC (newest first)
        entities.sort(Comparator.comparing(
                FuelTypeEntity::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        
        return entities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public FuelTypeResponse getFuelTypeById(UUID id) {
        FuelTypeEntity entity = fuelTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy loại nhiên liệu với ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()));
        return mapToResponse(entity);
    }

    @Override
    public List<FuelTypeResponse> searchFuelTypesByName(String name) {
        List<FuelTypeEntity> entities = fuelTypeRepository.findByNameContainingIgnoreCase(name);
        
        // Sort by createdAt DESC
        entities.sort(Comparator.comparing(
                FuelTypeEntity::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        
        return entities.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public FuelTypeResponse createFuelType(CreateFuelTypeRequest request) {
        // Check for duplicate name
        if (fuelTypeRepository.existsByName(request.getName())) {
            throw new BadRequestException(
                    "Loại nhiên liệu với tên '" + request.getName() + "' đã tồn tại",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }
        
        FuelTypeEntity entity = FuelTypeEntity.builder()
                .name(request.getName())
                .description(request.getDescription())
                .build();
        
        FuelTypeEntity savedEntity = fuelTypeRepository.save(entity);
        log.info("Created new fuel type: {} (ID: {})", savedEntity.getName(), savedEntity.getId());
        
        return mapToResponse(savedEntity);
    }

    @Override
    @Transactional
    public FuelTypeResponse updateFuelType(UpdateFuelTypeRequest request) {
        FuelTypeEntity entity = fuelTypeRepository.findById(request.getId())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy loại nhiên liệu với ID: " + request.getId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));
        
        // Check for duplicate name (excluding current entity)
        if (fuelTypeRepository.existsByNameAndIdNot(request.getName(), request.getId())) {
            throw new BadRequestException(
                    "Loại nhiên liệu với tên '" + request.getName() + "' đã tồn tại",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }
        
        entity.setName(request.getName());
        entity.setDescription(request.getDescription());
        
        FuelTypeEntity savedEntity = fuelTypeRepository.save(entity);
        log.info("Updated fuel type: {} (ID: {})", savedEntity.getName(), savedEntity.getId());
        
        return mapToResponse(savedEntity);
    }

    @Override
    @Transactional
    public void deleteFuelType(UUID id) {
        FuelTypeEntity entity = fuelTypeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy loại nhiên liệu với ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()));
        
        fuelTypeRepository.delete(entity);
        log.info("Deleted fuel type: {} (ID: {})", entity.getName(), id);
    }

    private FuelTypeResponse mapToResponse(FuelTypeEntity entity) {
        return FuelTypeResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .modifiedAt(entity.getModifiedAt())
                .build();
    }
}
