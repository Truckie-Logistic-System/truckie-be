package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.MaintenanceTypeRequest;
import capstone_project.dtos.request.vehicle.UpdateMaintenanceTypeRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.vehicle.MaintenanceTypeResponse;
import capstone_project.entity.vehicle.MaintenanceTypeEntity;

import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.vehicle.MaintenanceTypeEntityService;
import capstone_project.repository.repositories.vehicle.MaintenanceTypeRepository;
import capstone_project.service.mapper.vehicle.MaintenanceTypeMapper;
import capstone_project.service.services.vehicle.MaintenanceTypeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MaintenanceTypeServiceImpl implements MaintenanceTypeService {

    private final MaintenanceTypeEntityService entityService;
    private final MaintenanceTypeRepository repository;
    private final MaintenanceTypeMapper mapper;

    @Override
    public List<MaintenanceTypeResponse> getAll() {
        log.info("Fetching all maintenance types");
        return Optional.of(entityService.findAll())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new NotFoundException(
                        "There are no maintenance type available.",
                        ErrorEnum.NOT_FOUND.getErrorCode()))
                .stream()
                .map(mapper::toResponse)
                .toList();

    }

    @Override
    public MaintenanceTypeResponse getById(UUID id) {
        log.info("Fetching maintenance type by ID: {}", id);
        if (id == null) {
            throw new NotFoundException("ID should not be null.", ErrorEnum.REQUIRED.getErrorCode());
        }
        MaintenanceTypeEntity entity = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException("Maintenance type is not found with ID: " + id , ErrorEnum.NOT_FOUND.getErrorCode()));
        return mapper.toResponse(entity);
    }

    @Override
    public MaintenanceTypeResponse create(MaintenanceTypeRequest req) {
        log.info("Creating new maintenance type");
        MaintenanceTypeEntity entity = mapper.toEntity(req);
        entity = entityService.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public MaintenanceTypeResponse update(UUID id, UpdateMaintenanceTypeRequest req) {
        log.info("Updating maintenance type with ID: {}", id);
        if (id == null) {
            throw new NotFoundException("ID should not be null.", ErrorEnum.REQUIRED.getErrorCode());
        }
        MaintenanceTypeEntity entity = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode()));
        mapper.toEntity(req, entity);
        entity = entityService.save(entity);
        return mapper.toResponse(entity);
    }

    @Override
    public void delete(UUID id) {
        repository.deleteById(id);
    }
}
