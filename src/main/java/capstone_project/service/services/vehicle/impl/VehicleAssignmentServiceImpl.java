package capstone_project.service.services.vehicle.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.enums.VehicleTypeEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.vehicle.UpdateVehicleAssignmentRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.user.DriverResponse;
import capstone_project.dtos.response.vehicle.SampleVehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.dtos.response.vehicle.VehicleResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.entity.user.driver.DriverEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.pricing.VehicleRuleEntityService;
import capstone_project.repository.entityServices.user.DriverEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.user.DriverMapper;
import capstone_project.service.mapper.vehicle.VehicleAssignmentMapper;
import capstone_project.service.mapper.vehicle.VehicleMapper;
import capstone_project.service.services.order.order.ContractRuleService;
import capstone_project.service.services.order.order.ContractService;
import capstone_project.service.services.user.DriverService;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import capstone_project.dtos.response.vehicle.SimplifiedVehicleAssignmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class VehicleAssignmentServiceImpl implements VehicleAssignmentService {

    private final VehicleAssignmentEntityService entityService;
    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final DriverEntityService driverEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final OrderEntityService orderEntityService;
    private final ContractEntityService contractEntityService;
    private final ContractRuleService contractRuleService;
    private final VehicleRuleEntityService vehicleRuleEntityService;
    private final DriverService driverService;
    private final VehicleAssignmentMapper mapper;
    private final VehicleMapper vehicleMapper;
    private final DriverMapper driverMapper;

    @Override
    public List<VehicleAssignmentResponse> getAllAssignments() {
        log.info("Fetching all vehicles");
        return Optional.of(entityService.findAll())
                .filter(list -> !list.isEmpty())
                .orElseThrow(() -> new NotFoundException(
                        "There are no vehicle assignments available.",
                        ErrorEnum.NOT_FOUND.getErrorCode()))
                .stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public VehicleAssignmentResponse getAssignmentById(UUID id) {
        log.info("Fetching vehicle assignment by ID: {}", id);
        VehicleAssignmentEntity entity = entityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        "Assignment is not found with ASSIGNMENT ID: " + id,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return mapper.toResponse(entity);
    }

    @Override
    public VehicleAssignmentResponse createAssignment(VehicleAssignmentRequest req) {
        log.info("Creating new vehicle assignment");

        vehicleEntityService.findByVehicleId(UUID.fromString(req.vehicleId())).orElseThrow(() -> new NotFoundException(
                ErrorEnum.VEHICLE_NOT_FOUND.getMessage(),
                ErrorEnum.VEHICLE_NOT_FOUND.getErrorCode()
        ));

        driverEntityService.findEntityById(UUID.fromString(req.driverId_1())).orElseThrow(() -> new NotFoundException(
                "Driver 1 not found with DRIVER ID: " + req.driverId_1(),
                ErrorEnum.VEHICLE_NOT_FOUND.getErrorCode()
        ));

        driverEntityService.findEntityById(UUID.fromString(req.driverId_2())).orElseThrow(() -> new NotFoundException(
                "Driver 2 not found with DRIVER ID: " + req.driverId_2(),
                ErrorEnum.VEHICLE_NOT_FOUND.getErrorCode()
        ));

        var saved = entityService.save(mapper.toEntity(req));


        return mapper.toResponse(saved);
    }

    @Override
    public VehicleAssignmentResponse updateAssignment(UUID id, UpdateVehicleAssignmentRequest req) {
        log.info("Updating vehicle assignment with ID: {}", id);
        var existing = entityService.findEntityById(id)
                .orElseThrow(() ->
                        new NotFoundException(
                                "Assignment is not found with ASSIGNMENT ID: " + id,
                                ErrorEnum.NOT_FOUND.getErrorCode()));

        mapper.toEntity(req, existing);
        var updated = entityService.save(existing);
        return mapper.toResponse(updated);
    }

    @Override
    public List<VehicleAssignmentResponse> getAllAssignmentsWithOrder(UUID vehicleType) {
        log.info("Fetching vehicle assignment by vehicle type ID: {}", vehicleType);

        vehicleTypeEntityService.findEntityById(vehicleType)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.VEHICLE_TYPE_NOT_FOUND.getMessage(),
                        ErrorEnum.VEHICLE_TYPE_NOT_FOUND.getErrorCode()
                ));


        List<VehicleAssignmentEntity> entity = entityService.findVehicleWithOrder(vehicleType);
        if (entity.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NO_VEHICLE_AVAILABLE.getMessage(),
                    ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode());
        }
        return entity.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<VehicleAssignmentResponse> getListVehicleAssignmentByOrderID(UUID orderID) {
        log.info("Fetching vehicle assignment by vehicle type ID: {}", orderID);

        orderEntityService.findEntityById(orderID)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));


        List<VehicleAssignmentEntity> entity = entityService.findVehicleAssignmentsWithOrderID(orderID);
        if (entity.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NO_VEHICLE_AVAILABLE.getMessage(),
                    ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode());
        }
        return entity.stream()
                .map(mapper::toResponse)
                .toList();
    }

    @Override
    public List<SampleVehicleAssignmentResponse> getVehicleAndDriversForDetails(UUID orderID) {
        List<SampleVehicleAssignmentResponse> sampleVehicleAssignmentResponses = new ArrayList<>();

        OrderEntity order = orderEntityService.findEntityById(orderID)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Order",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        ContractEntity contractEntity = contractEntityService.getContractByOrderId(orderID)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + "Contract",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // lay goi y assign Vehicle type tu contract rules
        ListContractRuleAssignResult assignResult =
                contractRuleService.getListAssignOrUnAssignContractRule(contractEntity.getId());

        if (!order.getStatus().equals(OrderStatusEnum.ON_PLANNING.name())) {
            throw new NotFoundException("Đơn hàng chưa phải là ON_PLANNING",
                    ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode());
        }

        for (ContractRuleAssignResponse response : assignResult.vehicleAssignments()) {
            UUID vehicleRuleId = response.getVehicleRuleId();
            VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findEntityById(vehicleRuleId)
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle rule not found: " + vehicleRuleId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
            VehicleTypeEnum vehicleTypeEnum = VehicleTypeEnum.valueOf(vehicleRule.getVehicleTypeEntity().getVehicleTypeName());
            List<VehicleEntity> getVehiclesByVehicleType = vehicleEntityService.getVehicleEntitiesByVehicleTypeEntityAndStatus(vehicleRule.getVehicleTypeEntity(), CommonStatusEnum.ACTIVE.name());
            log.info("Tìm thấy {} xe ACTIVE cho loại {}", getVehiclesByVehicleType.size(), vehicleTypeEnum);
            List<UUID> vehicleIds = getVehiclesByVehicleType.stream().map(VehicleEntity::getId).toList();
            for (UUID vehicleId : sortVehiclesByUsageThisMonth(vehicleIds)) {
                Map<VehicleResponse, List<DriverResponse>> sampleVehicleAssignment = new HashMap<>();
                VehicleEntity vehicle = vehicleEntityService.findEntityById(vehicleId).get();
                Optional<VehicleAssignmentEntity> activeAssignment = entityService.findVehicleAssignmentByVehicleEntityAndStatus(vehicle, CommonStatusEnum.ACTIVE.name());
                if (activeAssignment.isPresent()) {
                    continue;
                }
                List<DriverEntity> availableDrivers = driverEntityService.findByStatus(CommonStatusEnum.ACTIVE.name())
                        .stream()
                        .filter(d -> driverService.isCheckClassDriverLicenseForVehicleType(d, vehicleTypeEnum))
                        .filter(d -> !entityService.existsActiveAssignmentForDriver(d.getId()))
                        .toList();
                log.info("Tìm thấy {} tài xế ACTIVE hợp lệ cho xe {}", availableDrivers.size(), vehicle.getId());
                List<DriverEntity> selectedDrivers = new ArrayList<>();
                List<VehicleAssignmentEntity> pastAssignments = entityService.findAssignmentsByVehicleOrderByCreatedAtDesc(vehicle);
                if (!pastAssignments.isEmpty()) {
                    VehicleAssignmentEntity lastAssignment = pastAssignments.get(0);
                    DriverEntity driver1 = lastAssignment.getDriver1();
                    DriverEntity driver2 = lastAssignment.getDriver2();
                    if (driver1 != null && CommonStatusEnum.ACTIVE.name().equals(driver1.getStatus()) &&
                            driverService.isCheckClassDriverLicenseForVehicleType(driver1, vehicleTypeEnum) &&
                            !entityService.existsActiveAssignmentForDriver(driver1.getId())) {
                        selectedDrivers.add(driver1);
                    }
                    if (driver2 != null && CommonStatusEnum.ACTIVE.name().equals(driver2.getStatus()) &&
                            driverService.isCheckClassDriverLicenseForVehicleType(driver2, vehicleTypeEnum) &&
                            !entityService.existsActiveAssignmentForDriver(driver2.getId()) &&
                            !selectedDrivers.contains(driver2)) {
                        selectedDrivers.add(driver2);
                    }
                }
                // Luôn bổ sung từ availableDrivers cho đủ 2 tài xế
                for (DriverEntity d : availableDrivers) {
                    if (selectedDrivers.size() >= 2) break;
                    if (!selectedDrivers.contains(d)) {
                        selectedDrivers.add(d);
                    }
                }
                log.info("Chọn được {} tài xế cho xe {}", selectedDrivers.size(), vehicle.getId());
                if (selectedDrivers.size() == 2) {
                    List<DriverResponse> driverResponses = selectedDrivers.stream()
                            .map(driverMapper::mapDriverResponse)
                            .toList();
                    sampleVehicleAssignment.put(vehicleMapper.toResponse(vehicle), driverResponses);
                }
                if (!sampleVehicleAssignment.isEmpty()) {
                    sampleVehicleAssignmentResponses.add(
                            new SampleVehicleAssignmentResponse(response.getAssignedDetails(), sampleVehicleAssignment)
                    );
                }
            }
        }
        if (sampleVehicleAssignmentResponses.isEmpty()) {
            log.warn("Không tìm được cặp xe và tài xế phù hợp cho order {}", orderID);
        }
        return sampleVehicleAssignmentResponses;
    }

    /**
     * Chuyển đổi response phức tạp sang response đơn giản hóa
     */
    public SimplifiedVehicleAssignmentResponse convertToSimplifiedResponse(List<SampleVehicleAssignmentResponse> responses) {
        List<SimplifiedVehicleAssignmentResponse.VehicleSuggestionDTO> vehicleSuggestions = new ArrayList<>();

        for (SampleVehicleAssignmentResponse response : responses) {
            for (Map.Entry<VehicleResponse, List<DriverResponse>> entry : response.sampleVehicleAssignment().entrySet()) {
                VehicleResponse vehicleResponse = entry.getKey();
                List<DriverResponse> driverResponses = entry.getValue();

                List<SimplifiedVehicleAssignmentResponse.DriverSuggestionDTO> driverSuggestionDTOs = driverResponses.stream()
                        .map(driver -> new SimplifiedVehicleAssignmentResponse.DriverSuggestionDTO(
                                UUID.fromString(driver.getId()),
                                driver.getUserResponse().getFullName(),
                                driver.getDriverLicenseNumber(),
                                driver.getLicenseClass()
                        ))
                        .toList();

                vehicleSuggestions.add(
                        new SimplifiedVehicleAssignmentResponse.VehicleSuggestionDTO(
                                UUID.fromString(vehicleResponse.id()),
                                vehicleResponse.licensePlateNumber(),
                                vehicleResponse.model(),
                                vehicleResponse.manufacturer(),
                                driverSuggestionDTOs
                        )
                );
            }
        }

        return new SimplifiedVehicleAssignmentResponse(vehicleSuggestions);
    }

    @Override
    public SimplifiedVehicleAssignmentResponse getSimplifiedSuggestionsForOrder(UUID orderID) {
        List<SampleVehicleAssignmentResponse> responses = getVehicleAndDriversForDetails(orderID);
        return convertToSimplifiedResponse(responses);
    }

    private List<UUID> sortVehiclesByUsageThisMonth(List<UUID> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return List.of();
        }

        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = startOfMonth.plusMonths(1);

        // Gọi repo 1 lần lấy count cho tất cả xe
        List<Object[]> results = entityService.countAssignmentsThisMonthForVehicles(vehicleIds, startOfMonth, endOfMonth);

        // Chuyển sang map: <vehicleId, count>
        Map<UUID, Long> usageMap = results.stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));

        // Những xe không có trong kết quả query thì count = 0
        for (UUID vehicleId : vehicleIds) {
            usageMap.putIfAbsent(vehicleId, 0L);
        }

        // Sắp xếp tăng dần theo số lần dùng
        return usageMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .toList();
    }

}
