package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ContractStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.common.utils.BinPacker;
import capstone_project.common.utils.UserContextUtils;
import capstone_project.dtos.request.order.ContractRequest;
import capstone_project.dtos.request.order.CreateContractForCusRequest;
import capstone_project.dtos.request.order.contract.ContractFileUploadRequest;
import capstone_project.dtos.response.order.contract.*;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.entity.order.order.CategoryPricingDetailEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.entity.pricing.SizeRuleEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.repository.entityServices.auth.impl.UserEntityServiceImpl;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.contract.ContractRuleEntityService;
import capstone_project.repository.entityServices.order.order.CategoryPricingDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
import capstone_project.repository.entityServices.pricing.DistanceRuleEntityService;
import capstone_project.repository.entityServices.pricing.SizeRuleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
import capstone_project.service.mapper.order.ContractMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.order.ContractService;
import capstone_project.service.services.order.order.OrderStatusWebSocketService;
import capstone_project.service.services.user.DistanceService;
import capstone_project.service.services.map.VietMapDistanceService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractEntityService contractEntityService;
    private final ContractRuleEntityService contractRuleEntityService;
    private final SizeRuleEntityService sizeRuleEntityService;
    private final CategoryPricingDetailEntityService categoryPricingDetailEntityService;
    private final OrderEntityService orderEntityService;
    private final DistanceRuleEntityService distanceRuleEntityService;
    private final BasingPriceEntityService basingPriceEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final VehicleEntityService vehicleEntityService;
    private final DistanceService distanceService;
    private final VietMapDistanceService vietMapDistanceService;
    private final CloudinaryService cloudinaryService;
    private final UserContextUtils userContextUtils;
    private final OrderStatusWebSocketService orderStatusWebSocketService;

    private final ContractMapper contractMapper;

    private static final double EARTH_RADIUS_KM = 6371.0;
    private final UserEntityServiceImpl userEntityServiceImpl;

    @Override
    public List<ContractResponse> getAllContracts() {
        
        List<ContractEntity> contractEntities = contractEntityService.findAll();
        if (contractEntities.isEmpty()) {
            log.warn("No contracts found");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        return contractEntities.stream()
                .map(contractMapper::toContractResponse)
                .toList();
    }

    @Override
    public ContractResponse getContractById(UUID id) {
        
        ContractEntity contractEntity = contractEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return contractMapper.toContractResponse(contractEntity);
    }

    @Override
    @Transactional
    public ContractResponse createContract(ContractRequest contractRequest) {

        if (contractRequest == null) {
            log.error("[createContract] Request is null");
            throw new BadRequestException("Contract request must not be null",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        if (contractRequest.orderId() == null) {
            log.error("[createContract] Order ID is null in contract request");
            throw new BadRequestException("Order ID must not be null",
                    ErrorEnum.NULL.getErrorCode());
        }

        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(contractRequest.orderId());
        } catch (IllegalArgumentException e) {
            log.error("[createContract] Invalid orderId format: {}", contractRequest.orderId());
            throw e;
        }

        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
            log.error("[createContract] Contract already exists for order ID: {}", orderUuid);
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        OrderEntity order = orderEntityService.findEntityById(orderUuid)
                .orElseThrow(() -> {
                    log.error("[createContract] Order not found: {}", orderUuid);
                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        ContractEntity contractEntity = contractMapper.mapRequestToEntity(contractRequest);
        contractEntity.setStatus(ContractStatusEnum.CONTRACT_DRAFT.name());
        contractEntity.setOrderEntity(order);

        ContractEntity savedContract = contractEntityService.save(contractEntity);

        return contractMapper.toContractResponse(savedContract);
    }

    @Override
    @Transactional
    public ContractResponse createBothContractAndContractRule(ContractRequest contractRequest) {

        if (contractRequest == null) {
            log.error("[createContract] Request is null");
            throw new BadRequestException("Contract request must not be null",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        if (contractRequest.orderId() == null) {
            log.error("[createContract] Order ID is null in contract request");
            throw new BadRequestException("Order ID must not be null",
                    ErrorEnum.NULL.getErrorCode());
        }

        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(contractRequest.orderId());
        } catch (IllegalArgumentException e) {
            log.error("[createBoth] Invalid orderId format: {}", contractRequest.orderId());
            throw e;
        }

        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
            log.error("[createBoth] Contract already exists for orderId={}", orderUuid);
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
//            deleteContractByOrderId(orderUuid);
        }

        OrderEntity order = orderEntityService.findEntityById(orderUuid)
                .orElseThrow(() -> {
                    log.error("[createBoth] Order not found: {}", orderUuid);
                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        BigDecimal distanceKm = distanceService.getDistanceInKilometers(order.getId());

        ContractEntity contractEntity = contractMapper.mapRequestToEntity(contractRequest);
        contractEntity.setStatus(ContractStatusEnum.CONTRACT_DRAFT.name());
        contractEntity.setOrderEntity(order);
        
        // Set contract deadlines
        setContractDeadlines(contractEntity, order);

        ContractEntity savedContract = contractEntityService.save(contractEntity);

        List<ContractRuleAssignResponse> assignments = assignVehiclesWithAvailability(orderUuid);

        Map<UUID, Integer> vehicleCountMap = assignments.stream()
                .collect(Collectors.groupingBy(ContractRuleAssignResponse::getSizeRuleId, Collectors.summingInt(a -> 1)));

        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
            UUID sizeRuleId = entry.getKey();
            Integer count = entry.getValue();

            SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                    .orElseThrow(() -> {
                        log.error("[createBoth] Vehicle rule not found: {}", sizeRuleId);
                        return new NotFoundException("Vehicle rule not found", ErrorEnum.NOT_FOUND.getErrorCode());
                    });

            ContractRuleEntity contractRule = ContractRuleEntity.builder()
                    .contractEntity(savedContract)
                    .sizeRuleEntity(sizeRule)
                    .numOfVehicles(count)
                    .status(CommonStatusEnum.ACTIVE.name())
                    .build();

            // 🔑 Lấy các orderDetails từ assignments
            List<OrderDetailForPackingResponse> detailResponses = assignments.stream()
                    .filter(a -> a.getSizeRuleId().equals(sizeRuleId))
                    .flatMap(a -> a.getAssignedDetails().stream())
                    .toList();

//            List<OrderDetailForPackingResponse> detailIds = assignments.stream()

            if (!detailResponses.isEmpty()) {
                List<UUID> detailIds = detailResponses.stream()
                        .map(r -> UUID.fromString(r.id()))
                        .toList();

                List<OrderDetailEntity> orderDetailEntities = orderDetailEntityService.findAllByIds(detailIds);
                contractRule.getOrderDetails().addAll(orderDetailEntities);
            }

            contractRuleEntityService.save(contractRule);
        }
        order.setStatus(OrderStatusEnum.PROCESSING.name());
        orderEntityService.save(order);

        PriceCalculationResponse totalPriceResponse = calculateTotalPrice(savedContract, distanceKm, vehicleCountMap);

        BigDecimal totalPrice = totalPriceResponse.getTotalPrice();

        UserEntity currentStaff = userContextUtils.getCurrentUser();

        savedContract.setTotalValue(totalPrice);
        savedContract.setStaff(currentStaff);
        ContractEntity updatedContract = contractEntityService.save(savedContract);

        return contractMapper.toContractResponse(updatedContract);
    }

    @Override
    @Transactional
    public ContractResponse createBothContractAndContractRuleForCus(CreateContractForCusRequest contractRequest) {

        if (contractRequest == null) {
            log.error("[createContract] Request is null");
            throw new BadRequestException("Contract request must not be null",
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }

        if (contractRequest.orderId() == null) {
            log.error("[createContract] Order ID is null in contract request");
            throw new BadRequestException("Order ID must not be null",
                    ErrorEnum.NULL.getErrorCode());
        }

        UUID orderUuid;
        try {
            orderUuid = UUID.fromString(contractRequest.orderId());
        } catch (IllegalArgumentException e) {
            log.error("[createBoth] Invalid orderId format: {}", contractRequest.orderId());
            throw e;
        }

        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
            log.error("[createBoth] Contract already exists for orderId={}", orderUuid);
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
//            deleteContractByOrderId(orderUuid);
        }

        OrderEntity order = orderEntityService.findEntityById(orderUuid)
                .orElseThrow(() -> {
                    log.error("[createBoth] Order not found: {}", orderUuid);
                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        BigDecimal distanceKm = distanceService.getDistanceInKilometers(order.getId());

        ContractEntity contractEntity = contractMapper.mapRequestForCusToEntity(contractRequest);
        contractEntity.setStatus(ContractStatusEnum.CONTRACT_DRAFT.name());
        contractEntity.setOrderEntity(order);
        
        // Set contract deadlines
        setContractDeadlines(contractEntity, order);

        ContractEntity savedContract = contractEntityService.save(contractEntity);

        List<ContractRuleAssignResponse> assignments = assignVehiclesWithAvailability(orderUuid);

        Map<UUID, Integer> vehicleCountMap = assignments.stream()
                .collect(Collectors.groupingBy(ContractRuleAssignResponse::getSizeRuleId, Collectors.summingInt(a -> 1)));

        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
            UUID sizeRuleId = entry.getKey();
            Integer count = entry.getValue();

            SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                    .orElseThrow(() -> {
                        log.error("[createBoth] Vehicle rule not found: {}", sizeRuleId);
                        return new NotFoundException("Vehicle rule not found", ErrorEnum.NOT_FOUND.getErrorCode());
                    });

            ContractRuleEntity contractRule = ContractRuleEntity.builder()
                    .contractEntity(savedContract)
                    .sizeRuleEntity(sizeRule)
                    .numOfVehicles(count)
                    .status(CommonStatusEnum.ACTIVE.name())
                    .build();

            // 🔑 Lấy các orderDetails từ assignments
            List<OrderDetailForPackingResponse> detailResponses = assignments.stream()
                    .filter(a -> a.getSizeRuleId().equals(sizeRuleId))
                    .flatMap(a -> a.getAssignedDetails().stream())
                    .toList();

//            List<OrderDetailForPackingResponse> detailIds = assignments.stream()

            if (!detailResponses.isEmpty()) {
                List<UUID> detailIds = detailResponses.stream()
                        .map(r -> UUID.fromString(r.id()))
                        .toList();

                List<OrderDetailEntity> orderDetailEntities = orderDetailEntityService.findAllByIds(detailIds);
                contractRule.getOrderDetails().addAll(orderDetailEntities);
            }

            contractRuleEntityService.save(contractRule);
        }
        
        OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
        order.setStatus(OrderStatusEnum.PROCESSING.name());
        orderEntityService.save(order);
        
        // Send WebSocket notification for status change
        try {
            orderStatusWebSocketService.sendOrderStatusChange(
                order.getId(),
                order.getOrderCode(),
                previousStatus,
                OrderStatusEnum.PROCESSING
            );
            
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for order status change: {}", e.getMessage());
            // Don't throw - WebSocket failure shouldn't break business logic
        }

        PriceCalculationResponse totalPriceResponse = calculateTotalPrice(savedContract, distanceKm, vehicleCountMap);

        BigDecimal totalPrice = totalPriceResponse.getTotalPrice();

        UserEntity currentStaff = userContextUtils.getCurrentUser();

        savedContract.setTotalValue(totalPrice);
        savedContract.setStaff(currentStaff);
        ContractEntity updatedContract = contractEntityService.save(savedContract);

        return contractMapper.toContractResponse(updatedContract);
    }

    @Override
    public BothOptimalAndRealisticAssignVehiclesResponse getBothOptimalAndRealisticAssignVehiclesResponse(UUID orderId) {
        List<ContractRuleAssignResponse> optimal = null;
        List<ContractRuleAssignResponse> realistic = null;

        optimal = assignVehiclesOptimal(orderId);

        realistic = assignVehiclesWithAvailability(orderId);
//        try {
//        } catch (Exception e) {
//            log.warn("[getBothOptimalAndRealisticAssignVehiclesResponse] Optimal assignment failed for orderId={}, reason={}", orderId, e.getMessage());
//        }
//
//        try {
//        } catch (Exception e) {
//            log.warn("[getBothOptimalAndRealisticAssignVehiclesResponse] Realistic assignment failed for orderId={}, reason={}", orderId, e.getMessage());
//        }

        if (optimal == null && realistic == null) {
            return null;
        }

        return new BothOptimalAndRealisticAssignVehiclesResponse(optimal, realistic);
    }

    @Override
    public ContractResponse updateContract(UUID id, ContractRequest contractRequest) {
        return null;
    }

    @Override
    @Transactional
    public void deleteContractByOrderId(UUID orderId) {

        if (orderId == null) {
            log.error("[deleteContractByOrderId] Order ID is null");
            throw new BadRequestException("Order ID must not be null",
                    ErrorEnum.NULL.getErrorCode());
        }

        ContractEntity contractEntity = contractEntityService.getContractByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Contract not found for order ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        List<ContractRuleEntity> ruleEntity = contractRuleEntityService.findContractRuleEntityByContractEntityId(contractEntity.getId());
        if (!ruleEntity.isEmpty()) {
            ruleEntity.forEach(rule -> rule.getOrderDetails().clear());
            contractRuleEntityService.saveAll(ruleEntity);
            contractRuleEntityService.deleteByContractEntityId(contractEntity.getId());
        }
        contractEntityService.deleteContractByOrderId(orderId);
    }

    @Override
    public List<ContractRuleAssignResponse> assignVehiclesWithAvailability(UUID orderId) {
        List<ContractRuleAssignResponse> optimal = assignVehiclesOptimal(orderId);

        OrderEntity orderEntity = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));

        List<SizeRuleEntity> sortedSizeRules = sizeRuleEntityService
                .findAllByCategoryId(orderEntity.getCategory().getId())
                .stream()
                .filter(rule -> CommonStatusEnum.ACTIVE.name().equals(rule.getStatus()))
                .sorted(Comparator.comparing(SizeRuleEntity::getMaxWeight)
                        .thenComparing(SizeRuleEntity::getMaxLength)
                        .thenComparing(SizeRuleEntity::getMaxWidth)
                        .thenComparing(SizeRuleEntity::getMaxHeight))
                .toList();

        // map ruleId -> số lượng xe khả dụng
        Map<UUID, Integer> availableVehicles = new HashMap<>();
        for (SizeRuleEntity rule : sortedSizeRules) {
            int count = vehicleEntityService
                    .getVehicleEntitiesByVehicleTypeEntityAndStatus(
                            rule.getVehicleTypeEntity(),
                            CommonStatusEnum.ACTIVE.name()
                    ).size();
            availableVehicles.put(rule.getId(), count);
        }

        // map ruleId -> số lượng xe đã sử dụng
        Map<UUID, Integer> usedVehicles = new HashMap<>();
        List<ContractRuleAssignResponse> realisticAssignments = new ArrayList<>();

        for (ContractRuleAssignResponse assignment : optimal) {
            UUID ruleId = assignment.getSizeRuleId();
            int used = usedVehicles.getOrDefault(ruleId, 0);
            int available = availableVehicles.getOrDefault(ruleId, 0);

            if (used < available) {
                // còn xe → gán
                realisticAssignments.add(assignment);
                usedVehicles.put(ruleId, used + 1);
            } else {
                // hết xe → upgrade
                SizeRuleEntity currentRule = sortedSizeRules.get(assignment.getVehicleIndex());
                SizeRuleEntity upgradedRule = tryUpgradeUntilAvailable(
                        assignment, currentRule, sortedSizeRules, availableVehicles, usedVehicles
                );

                if (upgradedRule != null) {
                    assignment.setSizeRuleId(upgradedRule.getId());
                    assignment.setSizeRuleName(upgradedRule.getSizeRuleName());
                    assignment.setVehicleIndex(sortedSizeRules.indexOf(upgradedRule));
                    realisticAssignments.add(assignment);

                    usedVehicles.put(upgradedRule.getId(),
                            usedVehicles.getOrDefault(upgradedRule.getId(), 0) + 1);
                } else {
                    log.error("Không có xe nào đủ khả dụng để chở cho order {}", orderId);
                    throw new BadRequestException(
                            ErrorEnum.NO_VEHICLE_AVAILABLE.getMessage(),
                            ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode()
                    );
                }
            }
        }

        return realisticAssignments;
    }

    private SizeRuleEntity tryUpgradeUntilAvailable(ContractRuleAssignResponse assignment,
                                                    SizeRuleEntity currentRule,
                                                    List<SizeRuleEntity> sortedRules,
                                                    Map<UUID, Integer> availableVehicles,
                                                    Map<UUID, Integer> usedVehicles) {

        int currentIdx = sortedRules.indexOf(currentRule);

        for (int nextIdx = currentIdx + 1; nextIdx < sortedRules.size(); nextIdx++) {
            SizeRuleEntity nextRule = sortedRules.get(nextIdx);
            int used = usedVehicles.getOrDefault(nextRule.getId(), 0);
            int available = availableVehicles.getOrDefault(nextRule.getId(), 0);

            if (used < available) {
                
                return nextRule;
            }
        }
        return null;
    }

//    public List<ContractRuleAssignResponse> assignVehiclesOptimal(UUID orderId) {
//        final long t0 = System.nanoTime();
//        
//
//        List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
//        if (details.isEmpty()) {
//            log.error("[assignVehicles] Order details not found for orderId={}", orderId);
//            throw new NotFoundException("No order details found for this order", ErrorEnum.NOT_FOUND.getErrorCode());
//        }
//
//        OrderEntity orderEntity = orderEntityService.findEntityById(orderId)
//                .orElseThrow(() -> {
//                    log.error("[assignVehicles] Order not found: {}", orderId);
//                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
//                });
//
//        if (orderEntity.getCategory() == null) {
//            log.error("[assignVehicles] Order category is null for orderId={}", orderId);
//            throw new BadRequestException("Order category is required", ErrorEnum.INVALID.getErrorCode());
//        }
//
//        // Lấy rule theo category, sort theo kluong + kích thước
//        List<sizeRuleEntity> sortedsizeRules = sizeRuleEntityService
//                .findAllByCategoryId(orderEntity.getCategory().getId())
//                .stream()
//                .filter(rule -> CommonStatusEnum.ACTIVE.name().equals(rule.getStatus()))
//                .sorted(Comparator.comparing(sizeRuleEntity::getMaxWeight)
//                        .thenComparing(sizeRuleEntity::getMaxLength)
//                        .thenComparing(sizeRuleEntity::getMaxWidth)
//                        .thenComparing(sizeRuleEntity::getMaxHeight))
//                .toList();
//
//        if (sortedsizeRules.isEmpty()) {
//            log.error("[assignVehicles] No vehicle rules found for categoryId={}", orderEntity.getCategory().getId());
//            throw new NotFoundException("No vehicle rules found for this category", ErrorEnum.NOT_FOUND.getErrorCode());
//        }
//
//        // Map ruleId -> rule, ruleId -> index
//        Map<UUID, sizeRuleEntity> ruleById = sortedsizeRules.stream()
//                .collect(Collectors.toMap(sizeRuleEntity::getId, Function.identity()));
//        Map<UUID, Integer> ruleIndexById = new HashMap<>();
//        for (int i = 0; i < sortedsizeRules.size(); i++) {
//            ruleIndexById.put(sortedsizeRules.get(i).getId(), i);
//        }
//
//        // Sort details (FFD: kiện   to trước)
//        details.sort((a, b) -> {
//            int cmp = b.getWeight().compareTo(a.getWeight());
//            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxLength().compareTo(a.getOrderSizeEntity().getMaxLength());
//            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxWidth().compareTo(a.getOrderSizeEntity().getMaxWidth());
//            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxHeight().compareTo(a.getOrderSizeEntity().getMaxHeight());
//            return cmp;
//        });
//
//        List<ContractRuleAssignResponse> assignments = new ArrayList<>();
//        int processed = 0;
//
//        // Gán kiện   vào xe
//        for (OrderDetailEntity detail : details) {
//            processed++;
//            if (detail.getOrderSizeEntity() == null) {
//                log.warn("[assignVehicles] Detail id={} missing orderSize", detail.getId());
//                throw new BadRequestException("Order detail missing size: " + detail.getId(), ErrorEnum.INVALID.getErrorCode());
//            }
//
//            
//
//            boolean assigned = false;
//
//            // thử gán vào xe đã mở
//            for (ContractRuleAssignResponse assignment : assignments) {
//                sizeRuleEntity currentRule = ruleById.get(assignment.getsizeRuleId());
//                if (currentRule == null) {
//                    log.error("[assignVehicles] Missing rule for id={}", assignment.getsizeRuleId());
//                    continue;
//                }
//
//                if (canFit(detail, currentRule, assignment)) {
//                    assignment.setCurrentLoad(assignment.getCurrentLoad().add(detail.getWeight()));
//                    assignment.getAssignedDetails().add(toPackingResponse(detail));
//                    
//                    assigned = true;
//                    break;
//                }
//
//                // thử upgrade
//                sizeRuleEntity upgradedRule = tryUpgrade(detail, assignment, sortedsizeRules);
//                if (upgradedRule != null) {
//                    assignment.setsizeRuleId(upgradedRule.getId());
//                    assignment.setsizeRuleName(upgradedRule.getsizeRuleName());
//                    assignment.setCurrentLoad(calculateTotalWeight(assignment, detail));
//                    assignment.getAssignedDetails().add(toPackingResponse(detail));
//                    
//                    assigned = true;
//                    break;
//                }
//            }
//
//            // nếu chưa gán được -> mở xe mới
//            if (!assigned) {
//                for (sizeRuleEntity rule : sortedsizeRules) {
//                    if (canFit(detail, rule)) {
//                        ContractRuleAssignResponse newAssignment = new ContractRuleAssignResponse(
//                                ruleIndexById.get(rule.getId()),
//                                rule.getId(),
//                                rule.getsizeRuleName(),
//                                detail.getWeight(),
//                                new ArrayList<>(List.of(toPackingResponse(detail)))
//                        );
//                        assignments.add(newAssignment);
//                        
//                        assigned = true;
//                        break;
//                    }
//                }
//            }
//
//            if (!assigned) {
//                log.error("[assignVehicles] No vehicle can carry detail {}", detail.getId());
//                throw new RuntimeException("Không có loại xe nào chở được kiện   " + detail.getId());
//            }
//        }
//
//        
//        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
//        
//        return assignments;
//    }

    @Override
    public List<ContractRuleAssignResponse> assignVehiclesOptimal(UUID orderId) {
        final long t0 = System.nanoTime();

        OrderEntity orderEntity = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));

        List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
        if (details.isEmpty()) {
            throw new NotFoundException("No order details found for this order", ErrorEnum.NOT_FOUND.getErrorCode());
        }

        if (orderEntity.getCategory() == null) {
            throw new BadRequestException("Order category is required", ErrorEnum.INVALID.getErrorCode());
        }

        List<SizeRuleEntity> sortedsizeRules = sizeRuleEntityService
                .findAllByCategoryId(orderEntity.getCategory().getId())
                .stream()
                .filter(rule -> CommonStatusEnum.ACTIVE.name().equals(rule.getStatus()))
                .sorted(Comparator.comparing(SizeRuleEntity::getMaxWeight)
                        .thenComparing(SizeRuleEntity::getMaxLength)
                        .thenComparing(SizeRuleEntity::getMaxWidth)
                        .thenComparing(SizeRuleEntity::getMaxHeight))
                .toList();

        if (sortedsizeRules.isEmpty()) {
            throw new NotFoundException("No vehicle rules found for this category", ErrorEnum.NOT_FOUND.getErrorCode());
        }

        List<BinPacker.ContainerState> containers = BinPacker.pack(details, sortedsizeRules);

        List<ContractRuleAssignResponse> responses = BinPacker.toContractResponses(containers, details);

        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;

        return responses;
    }

    /**
     * Tìm vehicle rule lớn hơn rule hiện tại trong sorted list
     */
    private SizeRuleEntity findNextBiggerRule(SizeRuleEntity current, List<SizeRuleEntity> sorted) {
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getId().equals(current.getId()) && i + 1 < sorted.size()) {
                return sorted.get(i + 1);
            }
        }
        return null;
    }

    private OrderDetailForPackingResponse toPackingResponse(OrderDetailEntity entity) {
        return new OrderDetailForPackingResponse(
                entity.getId().toString(),
                entity.getWeightTons(),
                entity.getWeightBaseUnit(),
                entity.getUnit(),
                entity.getTrackingCode()
        );
    }

    private BigDecimal calculateTotalWeight(ContractRuleAssignResponse assignment, OrderDetailEntity newDetail) {
        return assignment.getCurrentLoad().add(newDetail.getWeightTons());
    }

    private SizeRuleEntity tryUpgrade(OrderDetailEntity detail,
                                      ContractRuleAssignResponse assignment,
                                      List<SizeRuleEntity> sortedRules) {

        int currentIdx = assignment.getVehicleIndex();

        for (int nextIdx = currentIdx + 1; nextIdx < sortedRules.size(); nextIdx++) {
            SizeRuleEntity nextRule = sortedRules.get(nextIdx);

            if (canFit(detail, nextRule, assignment)) {
                // cập nhật lại index cho assignment
                assignment.setVehicleIndex(nextIdx);
                return nextRule;
            }
        }
        return null;
    }

    @Override
    public PriceCalculationResponse calculateTotalPrice(ContractEntity contract,
                                                        BigDecimal distanceKm,
                                                        Map<UUID, Integer> vehicleCountMap) {
        final long t0 = System.nanoTime();

        if (contract.getOrderEntity() == null || contract.getOrderEntity().getCategory() == null) {
            throw new BadRequestException("Contract missing order/category", ErrorEnum.INVALID.getErrorCode());
        }

        List<DistanceRuleEntity> distanceRules = distanceRuleEntityService.findAll()
                .stream()
                .sorted(Comparator.comparing(DistanceRuleEntity::getFromKm))
                .toList();

        if (distanceRules.isEmpty()) {
            throw new RuntimeException("No distance rules found");
        }

        BigDecimal total = BigDecimal.ZERO;
        List<PriceCalculationResponse.CalculationStep> steps = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
            UUID sizeRuleId = entry.getKey();
            int numOfVehicles = entry.getValue();

            SizeRuleEntity sizeRule = sizeRuleEntityService.findEntityById(sizeRuleId)
                    .orElseThrow(() -> new NotFoundException("Vehicle rule not found: " + sizeRuleId,
                            ErrorEnum.NOT_FOUND.getErrorCode()));

            BigDecimal ruleTotal = BigDecimal.ZERO;
            BigDecimal remaining = distanceKm;

            for (DistanceRuleEntity distanceRule : distanceRules) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal from = distanceRule.getFromKm();
                BigDecimal to = distanceRule.getToKm();

                BasingPriceEntity basePriceEntity = basingPriceEntityService
                        .findBasingPriceEntityBysizeRuleEntityIdAndDistanceRuleEntityId(
                                sizeRule.getId(), distanceRule.getId())
                        .orElseThrow(() -> new RuntimeException("No base price found for tier "
                                + from + "-" + to + " and sizeRule=" + sizeRule.getId()));

                if (from.compareTo(BigDecimal.ZERO) == 0 && to.compareTo(BigDecimal.valueOf(4)) == 0) {
                    // fixed tier
                    ruleTotal = ruleTotal.add(basePriceEntity.getBasePrice());
                    remaining = remaining.subtract(to);

                    steps.add(PriceCalculationResponse.CalculationStep.builder()
                            .sizeRuleName(sizeRule.getSizeRuleName())
                            .numOfVehicles(numOfVehicles)
                            .distanceRange("0-4 km")
                            .unitPrice(basePriceEntity.getBasePrice())
                            .appliedKm(BigDecimal.valueOf(4))
                            .subtotal(basePriceEntity.getBasePrice())
                            .build());

                } else {
                    BigDecimal tierDistance = (to == null) ? remaining : remaining.min(to.subtract(from));
                    BigDecimal add = basePriceEntity.getBasePrice().multiply(tierDistance);
                    ruleTotal = ruleTotal.add(add);
                    remaining = remaining.subtract(tierDistance);

                    steps.add(PriceCalculationResponse.CalculationStep.builder()
                            .sizeRuleName(sizeRule.getSizeRuleName())
                            .numOfVehicles(numOfVehicles)
                            .distanceRange(from + "-" + (to == null ? "∞" : to) + " km")
                            .unitPrice(basePriceEntity.getBasePrice())
                            .appliedKm(tierDistance)
                            .subtotal(add)
                            .build());
                }
            }

            if (numOfVehicles > 0) {
                ruleTotal = ruleTotal.multiply(BigDecimal.valueOf(numOfVehicles));
            }

            total = total.add(ruleTotal);
        }

        BigDecimal totalBeforeAdjustment = total;
        BigDecimal categoryExtraFee = BigDecimal.ZERO;
        BigDecimal categoryMultiplier = BigDecimal.ONE;
        BigDecimal promotionDiscount = BigDecimal.ZERO;

        CategoryPricingDetailEntity adjustment = categoryPricingDetailEntityService.findByCategoryId(contract.getOrderEntity().getCategory().getId());
        if (adjustment != null) {
            categoryMultiplier = adjustment.getPriceMultiplier() != null ? adjustment.getPriceMultiplier() : BigDecimal.ONE;
            categoryExtraFee = adjustment.getExtraFee() != null ? adjustment.getExtraFee() : BigDecimal.ZERO;

            BigDecimal adjustedTotal = total.multiply(categoryMultiplier).add(categoryExtraFee);

            steps.add(PriceCalculationResponse.CalculationStep.builder()
                    .sizeRuleName("Điều chỉnh loại hàng: " + contract.getOrderEntity().getCategory().getCategoryName())
                    .numOfVehicles(0)
                    .distanceRange("×" + categoryMultiplier + " + " + categoryExtraFee + " VND")
                    .unitPrice(categoryMultiplier)
                    .appliedKm(BigDecimal.ZERO)
                    .subtotal(adjustedTotal.subtract(total)) // phần chênh lệch
                    .build());

            total = adjustedTotal;
        }

        if (promotionDiscount.compareTo(BigDecimal.ZERO) > 0) {
            steps.add(PriceCalculationResponse.CalculationStep.builder()
                    .sizeRuleName("Khuyến mãi")
                    .numOfVehicles(0)
                    .distanceRange("N/A")
                    .unitPrice(promotionDiscount.negate())
                    .appliedKm(BigDecimal.ZERO)
                    .subtotal(promotionDiscount.negate())
                    .build());

            total = total.subtract(promotionDiscount);
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total price must not be negative");
        }

//        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;

        return PriceCalculationResponse.builder()
                .totalPrice(total)
                .totalBeforeAdjustment(totalBeforeAdjustment)
                .categoryExtraFee(categoryExtraFee)
                .categoryMultiplier(categoryMultiplier)
                .promotionDiscount(promotionDiscount)
                .finalTotal(total)
                .steps(steps)
//                .summary("Tổng giá trị hợp đồng: " + total + " (tính trong " + elapsedMs + " ms)")
                .build();
    }

    private boolean canFit(OrderDetailEntity detail, SizeRuleEntity rule) {
        OrderSizeEntity size = detail.getOrderSizeEntity();
        if (size == null) return false;

        return detail.getWeightTons().compareTo(rule.getMaxWeight()) <= 0
                && size.getMaxLength().compareTo(rule.getMaxLength()) <= 0
                && size.getMaxWidth().compareTo(rule.getMaxWidth()) <= 0
                && size.getMaxHeight().compareTo(rule.getMaxHeight()) <= 0;
    }

    private boolean canFit(OrderDetailEntity detail, SizeRuleEntity rule, ContractRuleAssignResponse assignment) {
        OrderSizeEntity size = detail.getOrderSizeEntity();
        if (size == null) return false;

        BigDecimal newLoad = assignment.getCurrentLoad().add(detail.getWeightTons());
        return newLoad.compareTo(rule.getMaxWeight()) <= 0
                && size.getMaxLength().compareTo(rule.getMaxLength()) <= 0
                && size.getMaxWidth().compareTo(rule.getMaxWidth()) <= 0
                && size.getMaxHeight().compareTo(rule.getMaxHeight()) <= 0;
    }

    private boolean canFitAll(List<UUID> detailIds, SizeRuleEntity newRule, OrderDetailEntity newDetail) {
        BigDecimal totalWeight = newDetail.getWeightTons();

        for (UUID id : detailIds) {
            OrderDetailEntity d = orderDetailEntityService.findEntityById(id)
                    .orElseThrow(() -> new NotFoundException("Order detail not found: " + id, ErrorEnum.NOT_FOUND.getErrorCode()));

            totalWeight = totalWeight.add(d.getWeightTons());

            OrderSizeEntity size = d.getOrderSizeEntity();
            if (size == null) {
                log.warn("[canFitAll] Detail id={} missing size", id);
                return false;
            }

            if (size.getMaxLength().compareTo(newRule.getMaxLength()) > 0
                    || size.getMaxWidth().compareTo(newRule.getMaxWidth()) > 0
                    || size.getMaxHeight().compareTo(newRule.getMaxHeight()) > 0) {
                
                return false;
            }
        }

        boolean ok = totalWeight.compareTo(newRule.getMaxWeight()) <= 0;
        if (!ok) {
            
        }
        return ok;
    }

    @Override
    public BigDecimal calculateDistanceKm(AddressEntity from, AddressEntity to) {
        
        return vietMapDistanceService.calculateDistance(from, to);
    }

    @Override
    public BigDecimal calculateDistanceKm(AddressEntity from, AddressEntity to, String vehicleType) {
        
        return vietMapDistanceService.calculateDistance(from, to, vehicleType);
    }

    // CONTRACT TO CLOUD

    @Override
    public ContractResponse uploadContractFile(ContractFileUploadRequest req) throws IOException {

        // Get original filename and extension
        String originalFilename = req.file().getOriginalFilename();
        String fileExtension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        
        String fileName = "contract_" + UUID.randomUUID() + fileExtension;

        // upload Cloudinary
        var uploadResult = cloudinaryService.uploadFile(
                req.file().getBytes(),
                fileName,
                "CONTRACTS"
        );

        // Get the correct URL based on resource type
        String fileUrl;
        String resourceType = uploadResult.get("resource_type").toString();
        
        if ("raw".equals(resourceType)) {
            // For PDFs and other raw files, use getRawFileUrl
            String publicId = uploadResult.get("public_id").toString();
            fileUrl = cloudinaryService.getRawFileUrl(publicId);
            
        } else {
            // For images, use the secure_url from upload result
            fileUrl = uploadResult.get("secure_url").toString();
            
        }

        // load relationships
        ContractEntity ce = contractEntityService.findEntityById(req.contractId())
                .orElseThrow(() -> new RuntimeException("Contract not found by id: " + req.contractId()));

        // save DB
        ce.setAttachFileUrl(fileUrl);
        ce.setDescription(req.description());
        ce.setEffectiveDate(req.effectiveDate());
        ce.setExpirationDate(req.expirationDate());
        ce.setAdjustedValue(req.adjustedValue());
        ce.setContractName(req.contractName());
        
        // Set staff user ID from current authenticated user
        UUID staffUserId = userContextUtils.getCurrentUserId();
        UserEntity staffUser = new UserEntity();
        staffUser.setId(staffUserId);
        ce.setStaff(staffUser);

        var updated = contractEntityService.save(ce);

        // Update order status to CONTRACT_DRAFT
        if (ce.getOrderEntity() != null) {
            OrderEntity order = ce.getOrderEntity();
            OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
            order.setStatus(OrderStatusEnum.CONTRACT_DRAFT.name());
            orderEntityService.save(order);

            // Send WebSocket notification for status change
            try {
                orderStatusWebSocketService.sendOrderStatusChange(
                    order.getId(),
                    order.getOrderCode(),
                    previousStatus,
                    OrderStatusEnum.CONTRACT_DRAFT
                );
                
            } catch (Exception e) {
                log.error("Failed to send WebSocket notification for order status change: {}", e.getMessage());
                // Don't throw - WebSocket failure shouldn't break business logic
            }
        }

        return contractMapper.toContractResponse(updated);

    }

    @Override
    public ContractResponse getContractByOrderId(UUID orderId) {
        
        ContractEntity contractEntity = contractEntityService.getContractByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Contract not found for order ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return contractMapper.toContractResponse(contractEntity);
    }

    /**
     * Set contract deadlines based on order details
     * Reasonable deadlines for Vietnamese logistics:
     * - Contract signing: 24 hours after contract draft creation
     * - Deposit payment: 48 hours after contract signing
     * - Full payment: 1 day before pickup time (earliest estimated start time)
     */
    private void setContractDeadlines(ContractEntity contract, OrderEntity order) {
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        
        // Signing deadline: 24 hours from contract creation
        contract.setSigningDeadline(now.plusHours(24));
        
        // Deposit payment deadline: 48 hours after signing (72 hours from creation)
        contract.setDepositPaymentDeadline(now.plusHours(72));
        
        // Full payment deadline: 1 day before pickup time
        // Get the earliest estimated start time from order details
        java.time.LocalDateTime earliestPickupTime = order.getOrderDetailEntities().stream()
                .map(OrderDetailEntity::getEstimatedStartTime)
                .filter(time -> time != null)
                .min(java.time.LocalDateTime::compareTo)
                .orElse(now.plusDays(7)); // Default to 7 days if no estimated time
        
        // Set deadline to 1 day before pickup time
        contract.setFullPaymentDeadline(earliestPickupTime.minusDays(1));

    }
}
