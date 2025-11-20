package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.UnitEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.common.utils.BinPacker;
import capstone_project.dtos.request.order.ContractRuleRequest;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.dtos.response.order.contract.*;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.entity.vehicle.VehicleEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.contract.ContractRuleEntityService;
import capstone_project.repository.entityServices.order.order.CategoryPricingDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
import capstone_project.repository.entityServices.pricing.DistanceRuleEntityService;
import capstone_project.repository.entityServices.pricing.SizeRuleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.mapper.order.ContractRuleMapper;
import capstone_project.service.services.order.order.ContractRuleService;
import capstone_project.service.services.order.order.ContractService;
import capstone_project.service.services.user.DistanceService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractRuleServiceImpl implements ContractRuleService {

    private final ContractRuleEntityService contractRuleEntityService;
    private final ContractRuleMapper contractRuleMapper;
    private final ContractEntityService contractEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final SizeRuleEntityService sizeRuleEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final ContractService contractService;
    private final OrderEntityService orderEntityService;
    private final DistanceService distanceService;
    private final DistanceRuleEntityService distanceRuleEntityService;
    private final BasingPriceEntityService basingPriceEntityService;
    private final CategoryPricingDetailEntityService categoryPricingDetailEntityService;

    @Override
    public List<ContractRuleResponse> getContracts() {
        
        List<ContractRuleEntity> contractRule = contractRuleEntityService.findAll();

        if (contractRule.isEmpty()) {
            log.warn("No contract rules found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return contractRule.stream()
                .map(contractRuleMapper::toContractRuleResponse)
                .toList();
    }

    @Override
    public ContractRuleResponse getContractById(UUID id) {

        ContractRuleEntity contractRule = contractRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        return contractRuleMapper.toContractRuleResponse(contractRule);
    }

    @Override
    public ListContractRuleAssignResult getListAssignOrUnAssignContractRule(UUID contractId) {

        ContractEntity contractEntity = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new NotFoundException("Contract not found with ID: " + contractId,
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        List<ContractRuleEntity> assignedRules =
                contractRuleEntityService.findContractRuleEntityByContractEntityId(contractId);

        List<OrderDetailEntity> orderDetails =
                orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(contractEntity.getOrderEntity().getId());

        Set<UUID> assignedDetailIds = assignedRules.stream()
                .flatMap(r -> r.getOrderDetails().stream())
                .map(OrderDetailEntity::getId)
                .collect(Collectors.toSet());

        List<UUID> unassignedDetails = orderDetails.stream()
                .map(OrderDetailEntity::getId)
                .filter(id -> !assignedDetailIds.contains(id))
                .toList();

        List<ContractRuleAssignResponse> responses = new ArrayList<>();
        int vehicleIndex = 0;

        for (ContractRuleEntity rule : assignedRules) {
            // Phát hiện đơn vị chủ đạo từ các orderDetail
            String dominantUnit = rule.getOrderDetails().stream()
                    .map(OrderDetailEntity::getUnit)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("Kí"); // Default là Kí

            // Tính currentLoad bằng weightBaseUnit (để tương thích với dữ liệu cũ)
            // Dữ liệu cũ: weightBaseUnit lưu theo đơn vị gốc (kg)
            // Dữ liệu mới: weightBaseUnit lưu theo tấn (đã convert)
            BigDecimal currentLoad = rule.getOrderDetails().stream()
                    .map(detail -> {
                        BigDecimal baseWeight = detail.getWeightBaseUnit();
                        if (baseWeight != null) {
                            return baseWeight;
                        }
                        // Fallback về weight nếu weightBaseUnit null
                        return detail.getWeightTons() != null ? detail.getWeightTons() : BigDecimal.ZERO;
                    })
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<OrderDetailForPackingResponse> detailResponses = rule.getOrderDetails().stream()
                    .map(this::toPackingResponse)
                    .toList();

            List<PackedDetailResponse> packedDetails = recreatePackingDetails(rule);

            responses.add(
                    ContractRuleAssignResponse.builder()
                            .vehicleIndex(vehicleIndex++)
                            .sizeRuleId(rule.getSizeRuleEntity().getId())
                            .sizeRuleName(rule.getSizeRuleEntity().getSizeRuleName())
                            .currentLoad(currentLoad)
                            .currentLoadUnit(dominantUnit) // Sử dụng đơn vị động
                            .assignedDetails(detailResponses)
                            .packedDetailDetails(packedDetails)
                            .build()
            );
        }

        return ListContractRuleAssignResult.builder()
                .vehicleAssignments(responses)
                .unassignedDetails(unassignedDetails)
                .build();

    }

    private List<PackedDetailResponse> recreatePackingDetails(ContractRuleEntity contractRule) {
        try {
            SizeRuleEntity sizeRule = contractRule.getSizeRuleEntity();
            List<OrderDetailEntity> assignedDetails = new ArrayList<>(contractRule.getOrderDetails());

            BinPacker.ManualResult result = BinPacker.packManualForDetails(
                    assignedDetails, sizeRule, 1);

            if (!result.containers.isEmpty()) {
                BinPacker.ContainerState container = result.containers.get(0);
                return convertPlacementsToPackedDetails(container.placements);
            }
        } catch (Exception e) {
            log.warn("Failed to recreate packing for contract rule {}: {}",
                    contractRule.getId(), e.getMessage());
        }
        return Collections.emptyList();
    }

    private List<PackedDetailResponse> convertPlacementsToPackedDetails(List<BinPacker.Placement> placements) {
        return placements.stream()
                .map(p -> PackedDetailResponse.builder()
                        .orderDetailId(p.box.id.toString())
                        .x(BigDecimal.valueOf(p.x).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                        .y(BigDecimal.valueOf(p.y).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                        .z(BigDecimal.valueOf(p.z).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                        .length(BigDecimal.valueOf(p.lx).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                        .width(BigDecimal.valueOf(p.ly).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                        .height(BigDecimal.valueOf(p.lz).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                        .orientation(p.lx + "x" + p.ly + "x" + p.lz)
                        .orientation_X(p.lx)
                        .orientation_Y(p.ly)
                        .orientation_Z(p.lz)
                        .build())
                .toList();
    }

//    private OrderDetailForPackingResponse toPackingResponse(OrderDetailEntity entity) {
//        return new OrderDetailForPackingResponse(
//                entity.getId().toString(),
//                entity.getWeight(),
//                entity.getWeightBaseUnit(),
//                entity.getUnit(),
//                entity.getTrackingCode()
//        );
//    }

    @Override
    @Transactional
    public ListContractRuleAssignResult createListContractRules(List<ContractRuleRequest> contractRuleRequests) {

        if (contractRuleRequests == null || contractRuleRequests.isEmpty()) {
            throw new BadRequestException("Contract rule requests must not be null or empty",
                    ErrorEnum.INVALID.getErrorCode());
        }

        UUID contractEntityId = UUID.fromString(contractRuleRequests.get(0).contractEntityId());
        ContractEntity contractEntity = contractEntityService.findEntityById(contractEntityId)
                .orElseThrow(() -> new NotFoundException("Contract not found with ID: " + contractEntityId,
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        List<OrderDetailEntity> orderDetails =
                orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(contractEntity.getOrderEntity().getId());
        if (orderDetails.isEmpty()) {
            throw new NotFoundException("No order details found for this contract", ErrorEnum.NOT_FOUND.getErrorCode());
        }

        List<ContractRuleEntity> existingRules =
                contractRuleEntityService.findContractRuleEntityByContractEntityId(contractEntityId);

        Set<UUID> alreadyAssigned = existingRules.stream()
                .flatMap(r -> r.getOrderDetails().stream())
                .map(OrderDetailEntity::getId)
                .collect(Collectors.toSet());

        List<OrderDetailEntity> unassignedDetails = orderDetails.stream()
                .filter(d -> !alreadyAssigned.contains(d.getId()))
                .toList();

        if (unassignedDetails.isEmpty()) {
            throw new BadRequestException("All order details have already been assigned for this contract",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        List<ContractRuleAssignResponse> responses = new ArrayList<>();
        int vehicleIndex = existingRules.size();
        Set<UUID> newlyAssigned = new HashSet<>();

        for (ContractRuleRequest request : contractRuleRequests) {
            UUID sizeRuleId = UUID.fromString(request.sizeRuleId());

            SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                    .orElseThrow(() -> new NotFoundException("Vehicle rule not found: " + request.sizeRuleId(),
                            ErrorEnum.NOT_FOUND.getErrorCode()));

            if (!CommonStatusEnum.ACTIVE.name().equals(sizeRule.getStatus())) {
                throw new BadRequestException("Vehicle rule " + sizeRule.getSizeRuleName() + " is not active",
                        ErrorEnum.INVALID.getErrorCode());
            }

            // Phát hiện đơn vị chủ đạo từ orderDetails (khai báo sớm để dùng cho tất cả containers)
            String dominantUnit = orderDetails.stream()
                    .map(OrderDetailEntity::getUnit)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse("Kí");

            List<VehicleEntity> activeVehicles = vehicleEntityService.findByVehicleTypeAndStatus(
                    sizeRule.getVehicleTypeEntity().getId(), CommonStatusEnum.ACTIVE.name());

            if (activeVehicles == null || activeVehicles.isEmpty()) {
                throw new BadRequestException(
                        "No ACTIVE vehicles available for type " + sizeRule.getVehicleTypeEntity().getVehicleTypeName(),
                        ErrorEnum.INVALID.getErrorCode());
            }

            int numOfVehicles = request.numOfVehicles();
            if (numOfVehicles <= 0) {
                throw new BadRequestException("numOfVehicles must be greater than 0", ErrorEnum.INVALID.getErrorCode());
            }

            List<BinPacker.ContainerState> containers = new ArrayList<>();
            for (int i = 0; i < numOfVehicles; i++) {
                containers.add(new BinPacker.ContainerState(
                        sizeRule,
                        BinPacker.convertToInt(sizeRule.getMaxLength()),
                        BinPacker.convertToInt(sizeRule.getMaxWidth()),
                        BinPacker.convertToInt(sizeRule.getMaxHeight())
                ));
            }

            BinPacker.ManualResult result = BinPacker.packManual(unassignedDetails, containers);

            BigDecimal maxWeight = sizeRule.getMaxWeight();
            boolean overloaded = result.containers.stream()
                    .anyMatch(c -> {
                        // Tính currentLoad chính xác từ weightBaseUnit (đã convert về tấn)
                        BigDecimal actualCurrentLoad = c.placements.stream()
                                .map(p -> {
                                    OrderDetailEntity detail = orderDetails.stream()
                                            .filter(d -> d.getId().equals(p.box.id))
                                            .findFirst()
                                            .orElse(null);
                                    if (detail == null) return BigDecimal.ZERO;

                                    // Ưu tiên dùng weightBaseUnit
                                    BigDecimal baseWeight = detail.getWeightBaseUnit();
                                    if (baseWeight != null) {
                                        return baseWeight;
                                    }
                                    // Fallback: convert từ weight và unit
                                    if (detail.getWeightTons() != null && detail.getUnit() != null) {
                                        try {
                                            UnitEnum unitEnum = UnitEnum.valueOf(detail.getUnit());
                                            return detail.getWeightTons().multiply(unitEnum.toTon());
                                        } catch (IllegalArgumentException e) {
                                            log.warn("Invalid unit for detail {}: {}", detail.getId(), detail.getUnit());
                                            return BigDecimal.ZERO;
                                        }
                                    }
                                    return BigDecimal.ZERO;
                                })
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        return actualCurrentLoad.compareTo(maxWeight) > 0;
                    });

            if (overloaded) {
                throw new BadRequestException(
                        "Total weight of order details exceeds the max capacity of vehicle type "
                                + sizeRule.getSizeRuleName(),
                        ErrorEnum.INVALID.getErrorCode());
            }

            boolean anyPacked = result.containers.stream().anyMatch(c -> !c.placements.isEmpty());
            if (!anyPacked) {
                throw new BadRequestException(
                        "No order details fit into the given vehicle type " + sizeRule.getSizeRuleName(),
                        ErrorEnum.INVALID.getErrorCode());
            }

            Optional<ContractRuleEntity> existingForVehicleOpt = existingRules.stream()
                    .filter(r -> r.getSizeRuleEntity().getId().equals(sizeRuleId))
                    .findFirst();

            for (BinPacker.ContainerState c : result.containers) {
                if (c.placements.isEmpty()) continue;

                List<OrderDetailForPackingResponse> assignedDetails = new ArrayList<>();

                ContractRuleEntity ruleEntity = existingForVehicleOpt.orElseGet(() -> {
                    ContractRuleEntity r = new ContractRuleEntity();
                    r.setContractEntity(contractEntity);
                    r.setSizeRuleEntity(sizeRule);
                    r.setStatus(CommonStatusEnum.ACTIVE.name());
                    r.setOrderDetails(new HashSet<>());
                    return r;
                });

                BigDecimal actualCurrentLoad = BigDecimal.ZERO;
                List<PackedDetailResponse> packedDetails = new ArrayList<>();

                for (BinPacker.Placement p : c.placements) {
                    OrderDetailEntity detail = orderDetails.stream()
                            .filter(d -> d.getId().equals(p.box.id))
                            .findFirst()
                            .orElse(null);
                    if (detail != null) {
                        ruleEntity.getOrderDetails().add(detail);
                        newlyAssigned.add(detail.getId());
                        assignedDetails.add(toPackingResponse(detail));

                        // Tính actualCurrentLoad bằng weightBaseUnit (đã convert về tấn)
                        BigDecimal detailWeight = detail.getWeightBaseUnit();
                        if (detailWeight == null && detail.getWeightTons() != null && detail.getUnit() != null) {
                            // Fallback: convert từ weight và unit nếu chưa có weightBaseUnit
                            try {
                                UnitEnum unitEnum = UnitEnum.valueOf(detail.getUnit());
                                detailWeight = detail.getWeightTons().multiply(unitEnum.toTon());
                            } catch (IllegalArgumentException e) {
                                log.warn("Invalid unit for detail {}: {}", detail.getId(), detail.getUnit());
                                detailWeight = BigDecimal.ZERO;
                            }
                        }
                        if (detailWeight == null) {
                            detailWeight = BigDecimal.ZERO;
                        }
                        actualCurrentLoad = actualCurrentLoad.add(detailWeight);

                        packedDetails.add(PackedDetailResponse.builder()
                                .orderDetailId(detail.getId().toString())
                                .x(BigDecimal.valueOf(p.x).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                                .y(BigDecimal.valueOf(p.y).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                                .z(BigDecimal.valueOf(p.z).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                                .length(BigDecimal.valueOf(p.lx).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                                .width(BigDecimal.valueOf(p.ly).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                                .height(BigDecimal.valueOf(p.lz).divide(BigDecimal.valueOf(BinPacker.UNIT_MULTIPLIER)))
                                .orientation(p.lx + "x" + p.ly + "x" + p.lz)
                                .orientation_X(p.lx)
                                .orientation_Y(p.ly)
                                .orientation_Z(p.lz)
                                .build());
                    }
                }

                contractRuleEntityService.save(ruleEntity);

                responses.add(
                        ContractRuleAssignResponse.builder()
                                .vehicleIndex(vehicleIndex++)
                                .sizeRuleId(sizeRule.getId())
                                .sizeRuleName(sizeRule.getSizeRuleName())
                                .currentLoad(actualCurrentLoad)
                                .currentLoadUnit(dominantUnit) // Sử dụng đơn vị động
                                .assignedDetails(assignedDetails)
                                .packedDetailDetails(packedDetails)
                                .build()
                );
            }

            unassignedDetails = unassignedDetails.stream()
                    .filter(d -> !newlyAssigned.contains(d.getId()))
                    .toList();
        }

        List<OrderDetailForPackingResponse> stillUnassigned = unassignedDetails.stream()
                .map(this::toPackingResponse)
                .toList();

        if (!stillUnassigned.isEmpty()) {
            log.warn("Still unassigned {} details: {}", stillUnassigned.size(),
                    stillUnassigned.stream().map(OrderDetailForPackingResponse::id).toList());
        }

        Map<UUID, Integer> vehicleCountMap = responses.stream()
                .collect(Collectors.groupingBy(ContractRuleAssignResponse::getSizeRuleId, Collectors.summingInt(a -> 1)));

        OrderEntity order = contractEntity.getOrderEntity();
        PriceCalculationResponse newTotal = contractService.calculateTotalPrice(
                contractEntity,
                distanceService.getDistanceInKilometers(order.getId()),
                vehicleCountMap
        );

        contractEntity.setTotalValue(newTotal.getTotalPrice());
        contractEntityService.save(contractEntity);

        return ListContractRuleAssignResult.builder()
                .vehicleAssignments(responses)
                .unassignedDetails(stillUnassigned.stream().map(r -> UUID.fromString(r.id())).toList())
                .build();
    }

    private OrderDetailForPackingResponse toPackingResponse(OrderDetailEntity detail) {
        return new OrderDetailForPackingResponse(
                detail.getId().toString(),
                detail.getWeightTons(),
                detail.getWeightBaseUnit(),
                detail.getUnit(),
                detail.getTrackingCode()
        );
    }

    @Override
    @Transactional
    public ContractRuleResponse updateContractRule(UUID id, ContractRuleRequest contractRuleRequest) {

        ContractRuleEntity existingContractRule = contractRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        ContractEntity contractEntity = existingContractRule.getContractEntity();
        if (contractEntity == null || contractEntity.getOrderEntity() == null) {
            log.error("Contract rule {} has invalid contract or order reference", id);
            throw new BadRequestException("Contract rule has invalid references",
                    ErrorEnum.INVALID.getErrorCode());
        }

        UUID sizeRuleId = UUID.fromString(contractRuleRequest.sizeRuleId());
        SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                .orElseThrow(() -> new NotFoundException("Vehicle rule not found: " + sizeRuleId,
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        Optional<ContractRuleEntity> existing = contractRuleEntityService
                .findContractRuleEntitiesByContractEntityIdAndSizeRuleEntityId(contractEntity.getId(), sizeRule.getId());

        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            log.error("Contract rule with vehicle rule {} already exists in contract {}", sizeRule.getSizeRuleName(), contractEntity.getId());
            throw new BadRequestException(
                    String.format("Contract rule for vehicle %s already exists in this contract", sizeRule.getSizeRuleName()),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
            );
        }

        List<OrderDetailEntity> allOrderDetails = orderDetailEntityService
                .findOrderDetailEntitiesByOrderEntityId(contractEntity.getOrderEntity().getId());

        if (allOrderDetails.isEmpty()) {
            throw new NotFoundException("No order details found for this contract",
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        Set<UUID> alreadyAssignedToOtherRules = contractRuleEntityService
                .findContractRuleEntityByContractEntityId(contractEntity.getId())
                .stream()
                .filter(r -> !r.getId().equals(id)) // Loại trừ contract rule hiện tại
                .flatMap(r -> r.getOrderDetails().stream())
                .map(OrderDetailEntity::getId)
                .collect(Collectors.toSet());

        List<OrderDetailEntity> availableDetails = allOrderDetails.stream()
                .filter(d -> !alreadyAssignedToOtherRules.contains(d.getId()))
                .toList();

        if (availableDetails.isEmpty()) {
            throw new BadRequestException("All order details are already assigned to other vehicle rules",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        if (!CommonStatusEnum.ACTIVE.name().equals(sizeRule.getStatus())) {
            throw new BadRequestException("Vehicle rule " + sizeRule.getSizeRuleName() + " is not active",
                    ErrorEnum.INVALID.getErrorCode());
        }

        List<BinPacker.ContainerState> containers = new ArrayList<>();
        containers.add(new BinPacker.ContainerState(
                sizeRule,
                BinPacker.convertToInt(sizeRule.getMaxLength()),
                BinPacker.convertToInt(sizeRule.getMaxWidth()),
                BinPacker.convertToInt(sizeRule.getMaxHeight())
        ));

        BinPacker.ManualResult result = BinPacker.packManual(availableDetails, containers);

        BigDecimal maxWeight = sizeRule.getMaxWeight();
        boolean overloaded = result.containers.stream()
                .anyMatch(c -> {
                    BigDecimal actualCurrentLoad = c.placements.stream()
                            .map(p -> {
                                OrderDetailEntity detail = allOrderDetails.stream()
                                        .filter(d -> d.getId().equals(p.box.id))
                                        .findFirst()
                                        .orElse(null);
                                return detail != null ? detail.getWeightTons() : BigDecimal.ZERO;
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return actualCurrentLoad.compareTo(maxWeight) > 0;
                });

        if (overloaded) {
            throw new BadRequestException(
                    "Total weight of assigned order details exceeds the max capacity of vehicle type "
                            + sizeRule.getSizeRuleName(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        boolean anyPacked = result.containers.stream().anyMatch(c -> !c.placements.isEmpty());
        if (!anyPacked) {
            throw new BadRequestException(
                    "No order details fit into the given vehicle type " + sizeRule.getSizeRuleName(),
                    ErrorEnum.INVALID.getErrorCode());
        }

        Set<OrderDetailEntity> assignedDetails = new HashSet<>();
        BigDecimal actualCurrentLoad = BigDecimal.ZERO;

        BinPacker.ContainerState container = result.containers.get(0);

        for (BinPacker.Placement p : container.placements) {
            OrderDetailEntity detail = allOrderDetails.stream()
                    .filter(d -> d.getId().equals(p.box.id))
                    .findFirst()
                    .orElse(null);
            if (detail != null) {
                assignedDetails.add(detail);
                actualCurrentLoad = actualCurrentLoad.add(detail.getWeightTons());
            }
        }

        contractRuleMapper.toContractRuleEntity(contractRuleRequest, existingContractRule);
        existingContractRule.setSizeRuleEntity(sizeRule);
        existingContractRule.getOrderDetails().clear();
        existingContractRule.getOrderDetails().addAll(assignedDetails);

        ContractRuleEntity saved = contractRuleEntityService.save(existingContractRule);

        Map<UUID, Integer> vehicleCountMap = contractRuleEntityService
                .findContractRuleEntityByContractEntityId(contractEntity.getId())
                .stream()
                .collect(Collectors.groupingBy(r -> r.getSizeRuleEntity().getId(), Collectors.summingInt(r -> 1)));

        OrderEntity order = contractEntity.getOrderEntity();
        PriceCalculationResponse newTotalResponse = contractService.calculateTotalPrice(
                contractEntity,
                distanceService.getDistanceInKilometers(order.getId()),
                vehicleCountMap
        );

        BigDecimal newTotal = newTotalResponse.getTotalPrice();
        contractEntity.setTotalValue(newTotal);
        contractEntityService.save(contractEntity);

        return contractRuleMapper.toContractRuleResponse(saved);
    }

    @Override
    public PriceCalculationResponse calculatePriceAPI(UUID contractId) {

        ContractEntity contract = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

        OrderEntity order = contract.getOrderEntity();
        if (order == null) {
            throw new BadRequestException("Contract has no associated order", ErrorEnum.INVALID.getErrorCode());
        }

        List<ContractRuleAssignResponse> assignResult = contractService.assignVehiclesWithAvailability(order.getId());

        Map<UUID, Integer> vehicleCountMap = assignResult.stream()
                .collect(Collectors.groupingBy(
                        ContractRuleAssignResponse::getSizeRuleId,
                        Collectors.summingInt(a -> 1)
                ));

        BigDecimal distanceKm = distanceService.getDistanceInKilometers(order.getId());

        PriceCalculationResponse priceResponse =
                contractService.calculateTotalPrice(contract, distanceKm, vehicleCountMap);

        return priceResponse;
    }

    @Override
    @Transactional
    public void deleteContractRule(UUID id) {

        ContractRuleEntity contractRule = contractRuleEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.error("Contract rule not found with ID {}", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        contractRule.getOrderDetails().clear();
        contractRuleEntityService.save(contractRule);

        contractRuleEntityService.deleteById(id);

    }

    @Override
    @Transactional
    public void deleteAllContractRulesByContract(UUID contractId) {

        List<ContractRuleEntity> contractRules = contractRuleEntityService.findContractRuleEntityByContractEntityId(contractId);

        if (contractRules.isEmpty()) {
            log.warn("No contract rules found for contract ID {}", contractId);
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        for (ContractRuleEntity contractRule : contractRules) {
            contractRule.getOrderDetails().clear();
        }

        contractRuleEntityService.saveAll(contractRules);
        contractRuleEntityService.deleteByContractEntityId(contractId);

    }

}
