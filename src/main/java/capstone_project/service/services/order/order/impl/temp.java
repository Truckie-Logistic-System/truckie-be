//package capstone_project.service.services.order.order.impl;
//
//import capstone_project.common.enums.CommonStatusEnum;
//import capstone_project.common.enums.ContractStatusEnum;
//import capstone_project.common.enums.ErrorEnum;
//import capstone_project.common.enums.OrderStatusEnum;
//import capstone_project.common.exceptions.dto.BadRequestException;
//import capstone_project.common.exceptions.dto.NotFoundException;
//import capstone_project.common.utils.BinPacker;
//import capstone_project.common.utils.UserContextUtils;
//import capstone_project.dtos.request.order.ContractRequest;
//import capstone_project.dtos.request.order.CreateContractForCusRequest;
//import capstone_project.dtos.request.order.contract.ContractFileUploadRequest;
//import capstone_project.dtos.response.order.contract.*;
//import capstone_project.entity.auth.UserEntity;
//import capstone_project.entity.order.contract.ContractEntity;
//import capstone_project.entity.order.contract.ContractRuleEntity;
//import capstone_project.entity.order.order.CategoryPricingDetailEntity;
//import capstone_project.entity.order.order.OrderDetailEntity;
//import capstone_project.entity.order.order.OrderEntity;
//import capstone_project.entity.order.order.OrderSizeEntity;
//import capstone_project.entity.pricing.BasingPriceEntity;
//import capstone_project.entity.pricing.DistanceRuleEntity;
//import capstone_project.entity.pricing.VehicleRuleEntity;
//import capstone_project.entity.user.address.AddressEntity;
//import capstone_project.repository.entityServices.auth.impl.UserEntityServiceImpl;
//import capstone_project.repository.entityServices.order.contract.ContractEntityService;
//import capstone_project.repository.entityServices.order.contract.ContractRuleEntityService;
//import capstone_project.repository.entityServices.order.order.CategoryPricingDetailEntityService;
//import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
//import capstone_project.repository.entityServices.order.order.OrderEntityService;
//import capstone_project.repository.entityServices.pricing.BasingPriceEntityService;
//import capstone_project.repository.entityServices.pricing.DistanceRuleEntityService;
//import capstone_project.repository.entityServices.pricing.VehicleRuleEntityService;
//import capstone_project.repository.entityServices.vehicle.VehicleEntityService;
//import capstone_project.service.mapper.order.ContractMapper;
//import capstone_project.service.services.cloudinary.CloudinaryService;
//import capstone_project.service.services.order.order.ContractService;
//import capstone_project.service.services.user.DistanceService;
//import jakarta.transaction.Transactional;
//import lombok.AllArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//
//import java.io.IOException;
//import java.math.BigDecimal;
//import java.util.*;
//import java.util.stream.Collectors;
//
//@Slf4j
//@Service
//@AllArgsConstructor
//public class ContractServiceImpl implements ContractService {
//
//    private final ContractEntityService contractEntityService;
//    private final ContractRuleEntityService contractRuleEntityService;
//    private final VehicleRuleEntityService vehicleRuleEntityService;
//    private final CategoryPricingDetailEntityService categoryPricingDetailEntityService;
//    private final OrderEntityService orderEntityService;
//    private final DistanceRuleEntityService distanceRuleEntityService;
//    private final BasingPriceEntityService basingPriceEntityService;
//    private final OrderDetailEntityService orderDetailEntityService;
//    private final VehicleEntityService vehicleEntityService;
//    private final DistanceService distanceService;
//    private final CloudinaryService cloudinaryService;
//    private final UserContextUtils userContextUtils;
//
//    private final ContractMapper contractMapper;
//
//    private static final double EARTH_RADIUS_KM = 6371.0;
//    private final UserEntityServiceImpl userEntityServiceImpl;
//
//    @Override
//    public List<ContractResponse> getAllContracts() {
//        log.info("Getting all contracts");
//        List<ContractEntity> contractEntities = contractEntityService.findAll();
//        if (contractEntities.isEmpty()) {
//            log.warn("No contracts found");
//            throw new NotFoundException(
//                    ErrorEnum.NOT_FOUND.getMessage(),
//                    ErrorEnum.NOT_FOUND.getErrorCode()
//            );
//        }
//        return contractEntities.stream()
//                .map(contractMapper::toContractResponse)
//                .toList();
//    }
//
//    @Override
//    public ContractResponse getContractById(UUID id) {
//        log.info("Getting contract by ID: {}", id);
//        ContractEntity contractEntity = contractEntityService.findEntityById(id)
//                .orElseThrow(() -> new NotFoundException(
//                        ErrorEnum.NOT_FOUND.getMessage(),
//                        ErrorEnum.NOT_FOUND.getErrorCode()
//                ));
//        return contractMapper.toContractResponse(contractEntity);
//    }
//
//    @Override
//    @Transactional
//    public ContractResponse createContract(ContractRequest contractRequest) {
//        log.info("Creating new contract");
//
//        if (contractRequest == null) {
//            log.error("[createContract] Request is null");
//            throw new BadRequestException("Contract request must not be null",
//                    ErrorEnum.INVALID_REQUEST.getErrorCode());
//        }
//
//        if (contractRequest.orderId() == null) {
//            log.error("[createContract] Order ID is null in contract request");
//            throw new BadRequestException("Order ID must not be null",
//                    ErrorEnum.NULL.getErrorCode());
//        }
//
//        UUID orderUuid;
//        try {
//            orderUuid = UUID.fromString(contractRequest.orderId());
//        } catch (IllegalArgumentException e) {
//            log.error("[createContract] Invalid orderId format: {}", contractRequest.orderId());
//            throw e;
//        }
//
//        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
//            log.error("[createContract] Contract already exists for order ID: {}", orderUuid);
//            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
//                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
//        }
//
//        OrderEntity order = orderEntityService.findEntityById(orderUuid)
//                .orElseThrow(() -> {
//                    log.error("[createContract] Order not found: {}", orderUuid);
//                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
//                });
//
//        ContractEntity contractEntity = contractMapper.mapRequestToEntity(contractRequest);
//        contractEntity.setStatus(ContractStatusEnum.CONTRACT_DRAFT.name());
//        contractEntity.setOrderEntity(order);
//
//        ContractEntity savedContract = contractEntityService.save(contractEntity);
//
//        return contractMapper.toContractResponse(savedContract);
//    }
//
//
//    @Override
//    @Transactional
//    public ContractResponse createBothContractAndContractRule(ContractRequest contractRequest) {
//        log.info("Creating new contract");
//
//        if (contractRequest == null) {
//            log.error("[createContract] Request is null");
//            throw new BadRequestException("Contract request must not be null",
//                    ErrorEnum.INVALID_REQUEST.getErrorCode());
//        }
//
//        if (contractRequest.orderId() == null) {
//            log.error("[createContract] Order ID is null in contract request");
//            throw new BadRequestException("Order ID must not be null",
//                    ErrorEnum.NULL.getErrorCode());
//        }
//
//        UUID orderUuid;
//        try {
//            orderUuid = UUID.fromString(contractRequest.orderId());
//        } catch (IllegalArgumentException e) {
//            log.error("[createBoth] Invalid orderId format: {}", contractRequest.orderId());
//            throw e;
//        }
//
//        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
//            log.error("[createBoth] Contract already exists for orderId={}", orderUuid);
//            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
//                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
////            deleteContractByOrderId(orderUuid);
//        }
//
//        OrderEntity order = orderEntityService.findEntityById(orderUuid)
//                .orElseThrow(() -> {
//                    log.error("[createBoth] Order not found: {}", orderUuid);
//                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
//                });
//
//        BigDecimal distanceKm = distanceService.getDistanceInKilometers(order.getId());
//
//        ContractEntity contractEntity = contractMapper.mapRequestToEntity(contractRequest);
//        contractEntity.setStatus(ContractStatusEnum.CONTRACT_DRAFT.name());
//        contractEntity.setOrderEntity(order);
//
//        ContractEntity savedContract = contractEntityService.save(contractEntity);
//
//        List<ContractRuleAssignResponse> assignments = assignVehiclesWithAvailability(orderUuid);
//
//        Map<UUID, Integer> vehicleCountMap = assignments.stream()
//                .collect(Collectors.groupingBy(ContractRuleAssignResponse::getVehicleRuleId, Collectors.summingInt(a -> 1)));
//
//
//        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
//            UUID vehicleRuleId = entry.getKey();
//            Integer count = entry.getValue();
//
//            VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findEntityById(vehicleRuleId)
//                    .orElseThrow(() -> {
//                        log.error("[createBoth] Vehicle rule not found: {}", vehicleRuleId);
//                        return new NotFoundException("Vehicle rule not found", ErrorEnum.NOT_FOUND.getErrorCode());
//                    });
//
//            ContractRuleEntity contractRule = ContractRuleEntity.builder()
//                    .contractEntity(savedContract)
//                    .vehicleRuleEntity(vehicleRule)
//                    .numOfVehicles(count)
//                    .status(CommonStatusEnum.ACTIVE.name())
//                    .build();
//
//            // 🔑 Lấy các orderDetails từ assignments
//            List<OrderDetailForPackingResponse> detailResponses = assignments.stream()
//                    .filter(a -> a.getVehicleRuleId().equals(vehicleRuleId))
//                    .flatMap(a -> a.getAssignedDetails().stream())
//                    .toList();
//
////            List<OrderDetailForPackingResponse> detailIds = assignments.stream()
//
//
//            if (!detailResponses.isEmpty()) {
//                List<UUID> detailIds = detailResponses.stream()
//                        .map(r -> UUID.fromString(r.id()))
//                        .toList();
//
//                List<OrderDetailEntity> orderDetailEntities = orderDetailEntityService.findAllByIds(detailIds);
//                contractRule.getOrderDetails().addAll(orderDetailEntities);
//            }
//
//            contractRuleEntityService.save(contractRule);
//        }
//        order.setStatus(OrderStatusEnum.CONTRACT_DRAFT.name());
//        orderEntityService.save(order);
//
//        PriceCalculationResponse totalPriceResponse = calculateTotalPrice(savedContract, distanceKm, vehicleCountMap);
//
//        BigDecimal totalPrice = totalPriceResponse.getTotalPrice();
//
//        UserEntity currentStaff = userContextUtils.getCurrentUser();
//
//        savedContract.setTotalValue(totalPrice);
//        savedContract.setStaff(currentStaff);
//        ContractEntity updatedContract = contractEntityService.save(savedContract);
//
//        return contractMapper.toContractResponse(updatedContract);
//    }
//
//    @Override
//    @Transactional
//    public ContractResponse createBothContractAndContractRuleForCus(CreateContractForCusRequest contractRequest) {
//        log.info("Creating new contract");
//
//        if (contractRequest == null) {
//            log.error("[createContract] Request is null");
//            throw new BadRequestException("Contract request must not be null",
//                    ErrorEnum.INVALID_REQUEST.getErrorCode());
//        }
//
//        if (contractRequest.orderId() == null) {
//            log.error("[createContract] Order ID is null in contract request");
//            throw new BadRequestException("Order ID must not be null",
//                    ErrorEnum.NULL.getErrorCode());
//        }
//
//        UUID orderUuid;
//        try {
//            orderUuid = UUID.fromString(contractRequest.orderId());
//        } catch (IllegalArgumentException e) {
//            log.error("[createBoth] Invalid orderId format: {}", contractRequest.orderId());
//            throw e;
//        }
//
//        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
//            log.error("[createBoth] Contract already exists for orderId={}", orderUuid);
//            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
//                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
////            deleteContractByOrderId(orderUuid);
//        }
//
//        OrderEntity order = orderEntityService.findEntityById(orderUuid)
//                .orElseThrow(() -> {
//                    log.error("[createBoth] Order not found: {}", orderUuid);
//                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
//                });
//
//        BigDecimal distanceKm = distanceService.getDistanceInKilometers(order.getId());
//
//        ContractEntity contractEntity = contractMapper.mapRequestForCusToEntity(contractRequest);
//        contractEntity.setStatus(ContractStatusEnum.CONTRACT_DRAFT.name());
//        contractEntity.setOrderEntity(order);
//
//        ContractEntity savedContract = contractEntityService.save(contractEntity);
//
//        List<ContractRuleAssignResponse> assignments = assignVehiclesWithAvailability(orderUuid);
//
//        Map<UUID, Integer> vehicleCountMap = assignments.stream()
//                .collect(Collectors.groupingBy(ContractRuleAssignResponse::getVehicleRuleId, Collectors.summingInt(a -> 1)));
//
//
//        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
//            UUID vehicleRuleId = entry.getKey();
//            Integer count = entry.getValue();
//
//            VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findEntityById(vehicleRuleId)
//                    .orElseThrow(() -> {
//                        log.error("[createBoth] Vehicle rule not found: {}", vehicleRuleId);
//                        return new NotFoundException("Vehicle rule not found", ErrorEnum.NOT_FOUND.getErrorCode());
//                    });
//
//            ContractRuleEntity contractRule = ContractRuleEntity.builder()
//                    .contractEntity(savedContract)
//                    .vehicleRuleEntity(vehicleRule)
//                    .numOfVehicles(count)
//                    .status(CommonStatusEnum.ACTIVE.name())
//                    .build();
//
//            // 🔑 Lấy các orderDetails từ assignments
//            List<OrderDetailForPackingResponse> detailResponses = assignments.stream()
//                    .filter(a -> a.getVehicleRuleId().equals(vehicleRuleId))
//                    .flatMap(a -> a.getAssignedDetails().stream())
//                    .toList();
//
////            List<OrderDetailForPackingResponse> detailIds = assignments.stream()
//
//
//            if (!detailResponses.isEmpty()) {
//                List<UUID> detailIds = detailResponses.stream()
//                        .map(r -> UUID.fromString(r.id()))
//                        .toList();
//
//                List<OrderDetailEntity> orderDetailEntities = orderDetailEntityService.findAllByIds(detailIds);
//                contractRule.getOrderDetails().addAll(orderDetailEntities);
//            }
//
//            contractRuleEntityService.save(contractRule);
//        }
//        order.setStatus(OrderStatusEnum.CONTRACT_DRAFT.name());
//        orderEntityService.save(order);
//
//        PriceCalculationResponse totalPriceResponse = calculateTotalPrice(savedContract, distanceKm, vehicleCountMap);
//
//        BigDecimal totalPrice = totalPriceResponse.getTotalPrice();
//
//        UserEntity currentStaff = userContextUtils.getCurrentUser();
//
//        savedContract.setTotalValue(totalPrice);
//        savedContract.setStaff(currentStaff);
//        ContractEntity updatedContract = contractEntityService.save(savedContract);
//
//        return contractMapper.toContractResponse(updatedContract);
//    }
//
//    @Override
//    public BothOptimalAndRealisticAssignVehiclesResponse getBothOptimalAndRealisticAssignVehiclesResponse(UUID orderId) {
//        List<ContractRuleAssignResponse> optimal = null;
//        List<ContractRuleAssignResponse> realistic = null;
//
//        try {
//            optimal = assignVehiclesOptimal(orderId);
//        } catch (Exception e) {
//            log.warn("[getBothOptimalAndRealisticAssignVehiclesResponse] Optimal assignment failed for orderId={}, reason={}", orderId, e.getMessage());
//        }
//
//        try {
//            realistic = assignVehiclesWithAvailability(orderId);
//        } catch (Exception e) {
//            log.warn("[getBothOptimalAndRealisticAssignVehiclesResponse] Realistic assignment failed for orderId={}, reason={}", orderId, e.getMessage());
//        }
//
//        if (optimal == null && realistic == null) {
//            return null;
//        }
//
//        return new BothOptimalAndRealisticAssignVehiclesResponse(optimal, realistic);
//    }
//
//
//    @Override
//    public ContractResponse updateContract(UUID id, ContractRequest contractRequest) {
//        return null;
//    }
//
//    @Override
//    @Transactional
//    public void deleteContractByOrderId(UUID orderId) {
//        log.info("Deleting contract by order ID: {}", orderId);
//
//        if (orderId == null) {
//            log.error("[deleteContractByOrderId] Order ID is null");
//            throw new BadRequestException("Order ID must not be null",
//                    ErrorEnum.NULL.getErrorCode());
//        }
//
//        ContractEntity contractEntity = contractEntityService.getContractByOrderId(orderId)
//                .orElseThrow(() -> new NotFoundException(
//                        "Contract not found for order ID: " + orderId,
//                        ErrorEnum.NOT_FOUND.getErrorCode()
//                ));
//
//        List<ContractRuleEntity> ruleEntity = contractRuleEntityService.findContractRuleEntityByContractEntityId(contractEntity.getId());
//        if (!ruleEntity.isEmpty()) {
//            ruleEntity.forEach(rule -> rule.getOrderDetails().clear());
//            contractRuleEntityService.saveAll(ruleEntity);
//            contractRuleEntityService.deleteByContractEntityId(contractEntity.getId());
//        }
//        contractEntityService.deleteContractByOrderId(orderId);
//    }
//
//
//
//    @Override
//    public List<ContractRuleAssignResponse> assignVehiclesWithAvailability(UUID orderId) {
//        List<ContractRuleAssignResponse> optimal = assignVehiclesOptimal(orderId);
//
//        OrderEntity orderEntity = orderEntityService.findEntityById(orderId)
//                .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));
//
//        // Lấy danh sách vehicle rules và sắp xếp từ NHỎ -> LỚN
//        List<VehicleRuleEntity> sortedVehicleRules = vehicleRuleEntityService
//                .findAllByCategoryId(orderEntity.getCategory().getId())
//                .stream()
//                .filter(rule -> CommonStatusEnum.ACTIVE.name().equals(rule.getStatus()))
//                .sorted(Comparator.comparing(VehicleRuleEntity::getMaxWeight)
//                        .thenComparing(VehicleRuleEntity::getMaxLength)
//                        .thenComparing(VehicleRuleEntity::getMaxWidth)
//                        .thenComparing(VehicleRuleEntity::getMaxHeight))
//                .toList();
//
//        // Map ruleId -> số lượng xe khả dụng
//        Map<UUID, Integer> availableVehicles = new HashMap<>();
//        for (VehicleRuleEntity rule : sortedVehicleRules) {
//            int count = vehicleEntityService
//                    .getVehicleEntitiesByVehicleTypeEntityAndStatus(
//                            rule.getVehicleTypeEntity(),
//                            CommonStatusEnum.ACTIVE.name()
//                    ).size();
//            availableVehicles.put(rule.getId(), count);
//        }
//
//        // Map ruleId -> số lượng xe đã sử dụng
//        Map<UUID, Integer> usedVehicles = new HashMap<>();
//        List<ContractRuleAssignResponse> realisticAssignments = new ArrayList<>();
//
//        for (ContractRuleAssignResponse optimalAssignment : optimal) {
//            UUID optimalRuleId = optimalAssignment.getVehicleRuleId();
//            int used = usedVehicles.getOrDefault(optimalRuleId, 0);
//            int available = availableVehicles.getOrDefault(optimalRuleId, 0);
//
//            if (used < available) {
//                // Còn xe khả dụng → gán đúng loại xe optimal
//                realisticAssignments.add(optimalAssignment);
//                usedVehicles.put(optimalRuleId, used + 1);
//            } else {
//                // Hết xe → UPGRADE lên xe LỚN HƠN
//                VehicleRuleEntity currentRule = findVehicleRuleById(optimalRuleId, sortedVehicleRules);
//                VehicleRuleEntity upgradedRule = findNextBiggerRule(currentRule, sortedVehicleRules);
//
//                boolean upgraded = false;
//                while (upgradedRule != null && !upgraded) {
//                    int upgradedUsed = usedVehicles.getOrDefault(upgradedRule.getId(), 0);
//                    int upgradedAvailable = availableVehicles.getOrDefault(upgradedRule.getId(), 0);
//
//                    if (upgradedUsed < upgradedAvailable) {
//                        // Tạo assignment mới với xe upgraded
//                        ContractRuleAssignResponse upgradedAssignment = createUpgradedAssignment(
//                                optimalAssignment, upgradedRule, sortedVehicleRules);
//                        realisticAssignments.add(upgradedAssignment);
//                        usedVehicles.put(upgradedRule.getId(), upgradedUsed + 1);
//                        upgraded = true;
//                        log.info("[assignVehiclesWithAvailability] Upgraded vehicle from {} to {} for order {}",
//                                currentRule.getVehicleRuleName(), upgradedRule.getVehicleRuleName(), orderId);
//                    } else {
//                        // Thử xe lớn hơn nữa
//                        upgradedRule = findNextBiggerRule(upgradedRule, sortedVehicleRules);
//                    }
//                }
//
//                if (!upgraded) {
//                    log.error("[assignVehiclesWithAvailability] No available vehicles for order {}, optimal rule: {}",
//                            orderId, currentRule.getVehicleRuleName());
//                    throw new BadRequestException(
//                            ErrorEnum.NO_VEHICLE_AVAILABLE.getMessage(),
//                            ErrorEnum.NO_VEHICLE_AVAILABLE.getErrorCode()
//                    );
//                }
//            }
//        }
//
//        // Sửa vehicleIndex
//        for (int i = 0; i < realisticAssignments.size(); i++) {
//            realisticAssignments.get(i).setVehicleIndex(i);
//        }
//
//        return realisticAssignments;
//    }
//
//    /**
//     * Tìm vehicle rule bằng ID
//     */
//    private VehicleRuleEntity findVehicleRuleById(UUID ruleId, List<VehicleRuleEntity> sortedRules) {
//        return sortedRules.stream()
//                .filter(rule -> rule.getId().equals(ruleId))
//                .findFirst()
//                .orElseThrow(() -> new RuntimeException("Vehicle rule not found: " + ruleId));
//    }
//
//    /**
//     * Tạo assignment mới với xe đã upgrade
//     */
//    private ContractRuleAssignResponse createUpgradedAssignment(ContractRuleAssignResponse original,
//                                                                VehicleRuleEntity upgradedRule,
//                                                                List<VehicleRuleEntity> sortedRules) {
//        ContractRuleAssignResponse upgraded = new ContractRuleAssignResponse();
//
//        // Copy tất cả thông tin từ original
//        upgraded.setVehicleRuleId(upgradedRule.getId());
//        upgraded.setVehicleRuleName(upgradedRule.getVehicleRuleName());
//        upgraded.setCurrentLoad(original.getCurrentLoad());
//        upgraded.setAssignedDetails(new ArrayList<>(original.getAssignedDetails()));
//        upgraded.setPackedDetailDetails(new ArrayList<>(original.getPackedDetailDetails()));
//
//        // Set vehicleIndex tạm thời, sẽ được sửa sau
//        upgraded.setVehicleIndex(original.getVehicleIndex());
//
//        return upgraded;
//    }
//
//
//    private VehicleRuleEntity tryUpgradeUntilAvailable(ContractRuleAssignResponse assignment,
//                                                       VehicleRuleEntity currentRule,
//                                                       List<VehicleRuleEntity> sortedRules,
//                                                       Map<UUID, Integer> availableVehicles,
//                                                       Map<UUID, Integer> usedVehicles) {
//
//        int currentIdx = sortedRules.indexOf(currentRule);
//
//        for (int nextIdx = currentIdx + 1; nextIdx < sortedRules.size(); nextIdx++) {
//            VehicleRuleEntity nextRule = sortedRules.get(nextIdx);
//            int used = usedVehicles.getOrDefault(nextRule.getId(), 0);
//            int available = availableVehicles.getOrDefault(nextRule.getId(), 0);
//
//            if (used < available) {
//                log.info("[assignVehicles] Upgrade thành công assignment rule={} -> rule={} (used={}/available={})",
//                        currentRule.getId(), nextRule.getId(), used + 1, available);
//                return nextRule;
//            }
//        }
//        return null;
//    }
//
////    public List<ContractRuleAssignResponse> assignVehiclesOptimal(UUID orderId) {
////        final long t0 = System.nanoTime();
////        log.info("Assigning vehicles for order ID: {}", orderId);
////
////        List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
////        if (details.isEmpty()) {
////            log.error("[assignVehicles] Order details not found for orderId={}", orderId);
////            throw new NotFoundException("No order details found for this order", ErrorEnum.NOT_FOUND.getErrorCode());
////        }
////
////        OrderEntity orderEntity = orderEntityService.findEntityById(orderId)
////                .orElseThrow(() -> {
////                    log.error("[assignVehicles] Order not found: {}", orderId);
////                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
////                });
////
////        if (orderEntity.getCategory() == null) {
////            log.error("[assignVehicles] Order category is null for orderId={}", orderId);
////            throw new BadRequestException("Order category is required", ErrorEnum.INVALID.getErrorCode());
////        }
////
////        // Lấy rule theo category, sort theo kluong + kích thước
////        List<VehicleRuleEntity> sortedVehicleRules = vehicleRuleEntityService
////                .findAllByCategoryId(orderEntity.getCategory().getId())
////                .stream()
////                .filter(rule -> CommonStatusEnum.ACTIVE.name().equals(rule.getStatus()))
////                .sorted(Comparator.comparing(VehicleRuleEntity::getMaxWeight)
////                        .thenComparing(VehicleRuleEntity::getMaxLength)
////                        .thenComparing(VehicleRuleEntity::getMaxWidth)
////                        .thenComparing(VehicleRuleEntity::getMaxHeight))
////                .toList();
////
////        if (sortedVehicleRules.isEmpty()) {
////            log.error("[assignVehicles] No vehicle rules found for categoryId={}", orderEntity.getCategory().getId());
////            throw new NotFoundException("No vehicle rules found for this category", ErrorEnum.NOT_FOUND.getErrorCode());
////        }
////
////        // Map ruleId -> rule, ruleId -> index
////        Map<UUID, VehicleRuleEntity> ruleById = sortedVehicleRules.stream()
////                .collect(Collectors.toMap(VehicleRuleEntity::getId, Function.identity()));
////        Map<UUID, Integer> ruleIndexById = new HashMap<>();
////        for (int i = 0; i < sortedVehicleRules.size(); i++) {
////            ruleIndexById.put(sortedVehicleRules.get(i).getId(), i);
////        }
////
////        // Sort details (FFD: kiện to trước)
////        details.sort((a, b) -> {
////            int cmp = b.getWeight().compareTo(a.getWeight());
////            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxLength().compareTo(a.getOrderSizeEntity().getMaxLength());
////            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxWidth().compareTo(a.getOrderSizeEntity().getMaxWidth());
////            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxHeight().compareTo(a.getOrderSizeEntity().getMaxHeight());
////            return cmp;
////        });
////
////        List<ContractRuleAssignResponse> assignments = new ArrayList<>();
////        int processed = 0;
////
////        // Gán kiện vào xe
////        for (OrderDetailEntity detail : details) {
////            processed++;
////            if (detail.getOrderSizeEntity() == null) {
////                log.warn("[assignVehicles] Detail id={} missing orderSize", detail.getId());
////                throw new BadRequestException("Order detail missing size: " + detail.getId(), ErrorEnum.INVALID.getErrorCode());
////            }
////
////            log.info("[assignVehicles] Processing detail {}/{}: id={}, weight={}, size={}x{}x{}",
////                    processed, details.size(), detail.getId(), detail.getWeight(),
////                    detail.getOrderSizeEntity().getMaxLength(),
////                    detail.getOrderSizeEntity().getMaxWidth(),
////                    detail.getOrderSizeEntity().getMaxHeight());
////
////            boolean assigned = false;
////
////            // thử gán vào xe đã mở
////            for (ContractRuleAssignResponse assignment : assignments) {
////                VehicleRuleEntity currentRule = ruleById.get(assignment.getVehicleRuleId());
////                if (currentRule == null) {
////                    log.error("[assignVehicles] Missing rule for id={}", assignment.getVehicleRuleId());
////                    continue;
////                }
////
////                if (canFit(detail, currentRule, assignment)) {
////                    assignment.setCurrentLoad(assignment.getCurrentLoad().add(detail.getWeight()));
////                    assignment.getAssignedDetails().add(toPackingResponse(detail));
////                    log.info("[assignVehicles] Assigned detail {} -> existing vehicle ruleId={}, newLoad={}",
////                            detail.getId(), currentRule.getId(), assignment.getCurrentLoad());
////                    assigned = true;
////                    break;
////                }
////
////                // thử upgrade
////                VehicleRuleEntity upgradedRule = tryUpgrade(detail, assignment, sortedVehicleRules);
////                if (upgradedRule != null) {
////                    assignment.setVehicleRuleId(upgradedRule.getId());
////                    assignment.setVehicleRuleName(upgradedRule.getVehicleRuleName());
////                    assignment.setCurrentLoad(calculateTotalWeight(assignment, detail));
////                    assignment.getAssignedDetails().add(toPackingResponse(detail));
////                    log.info("[assignVehicles] Upgraded vehicle for detail {} -> ruleId={}, maxWeight={}, newLoad={}",
////                            detail.getId(), upgradedRule.getId(), upgradedRule.getMaxWeight(), assignment.getCurrentLoad());
////                    assigned = true;
////                    break;
////                }
////            }
////
////            // nếu chưa gán được -> mở xe mới
////            if (!assigned) {
////                for (VehicleRuleEntity rule : sortedVehicleRules) {
////                    if (canFit(detail, rule)) {
////                        ContractRuleAssignResponse newAssignment = new ContractRuleAssignResponse(
////                                ruleIndexById.get(rule.getId()),
////                                rule.getId(),
////                                rule.getVehicleRuleName(),
////                                detail.getWeight(),
////                                new ArrayList<>(List.of(toPackingResponse(detail)))
////                        );
////                        assignments.add(newAssignment);
////                        log.info("[assignVehicles] Opened new vehicle for detail {} -> ruleId={}, firstLoad={}",
////                                detail.getId(), rule.getId(), detail.getWeight());
////                        assigned = true;
////                        break;
////                    }
////                }
////            }
////
////            if (!assigned) {
////                log.error("[assignVehicles] No vehicle can carry detail {}", detail.getId());
////                throw new RuntimeException("Không có loại xe nào chở được kiện " + detail.getId());
////            }
////        }
////
////        log.info("[assignVehicles] Completed. vehiclesUsed={}, detailsProcessed={}", assignments.size(), processed);
////        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
////        log.info("[assignVehicles] Finished in {} ms", elapsedMs);
////        return assignments;
////    }
//
//
//    @Override
//    public List<ContractRuleAssignResponse> assignVehiclesOptimal(UUID orderId) {
//        final long t0 = System.nanoTime();
//        log.info("[assignVehiclesOptimal] Starting for orderId: {}", orderId);
//
//        // 1. Lấy thông tin đơn hàng và chi tiết
//        OrderEntity orderEntity = orderEntityService.findEntityById(orderId)
//                .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));
//
//        List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
//        if (details.isEmpty()) {
//            throw new NotFoundException("No order details found for this order", ErrorEnum.NOT_FOUND.getErrorCode());
//        }
//
//        if (orderEntity.getCategory() == null) {
//            throw new BadRequestException("Order category is required", ErrorEnum.INVALID.getErrorCode());
//        }
//
//        // 2. Lấy danh sách vehicle rules và sắp xếp từ NHỎ -> LỚN
//        List<VehicleRuleEntity> sortedVehicleRules = vehicleRuleEntityService
//                .findAllByCategoryId(orderEntity.getCategory().getId())
//                .stream()
//                .filter(rule -> CommonStatusEnum.ACTIVE.name().equals(rule.getStatus()))
//                .sorted(Comparator.comparing(VehicleRuleEntity::getMaxWeight)
//                        .thenComparing(VehicleRuleEntity::getMaxLength)
//                        .thenComparing(VehicleRuleEntity::getMaxWidth)
//                        .thenComparing(VehicleRuleEntity::getMaxHeight))
//                .toList();
//
//        if (sortedVehicleRules.isEmpty()) {
//            throw new NotFoundException("No vehicle rules found for this category", ErrorEnum.NOT_FOUND.getErrorCode());
//        }
//
//        // 3. Convert OrderDetailEntity -> BoxItem và sắp xếp GIẢM DẦN theo trọng lượng
//        List<BinPacker.BoxItem> boxes = details.stream()
//                .map(d -> {
//                    OrderSizeEntity s = d.getOrderSizeEntity();
//                    if (s == null) {
//                        throw new BadRequestException("Order detail missing size: " + d.getId(), ErrorEnum.INVALID.getErrorCode());
//                    }
//                    int lx = BinPacker.convertToInt(s.getMaxLength());
//                    int ly = BinPacker.convertToInt(s.getMaxWidth());
//                    int lz = BinPacker.convertToInt(s.getMaxHeight());
//                    long w = BinPacker.convertWeightToLong(d.getWeight());
//                    return new BinPacker.BoxItem(d.getId(), lx, ly, lz, w);
//                })
//                .sorted((a, b) -> Long.compare(b.weight, a.weight)) // Sắp xếp GIẢM DẦN theo trọng lượng
//                .toList();
//
//        List<BinPacker.ContainerState> containers = new ArrayList<>();
//
//        // 4. Thuật toán First-Fit Decreasing với upgrade tham lam
//        for (BinPacker.BoxItem box : boxes) {
//            boolean assigned = false;
//
//            // Ưu tiên thêm vào xe hiện có trước
//            for (int ci = 0; ci < containers.size(); ci++) {
//                BinPacker.ContainerState currentContainer = containers.get(ci);
//
//                // Thử thêm vào xe hiện tại
//                BinPacker.Placement placement = BinPacker.tryPlaceBoxInContainer(box, currentContainer);
//                if (placement != null) {
//                    currentContainer.addPlacement(placement);
//                    assigned = true;
//                    break;
//                }
//
//                // Nếu không vừa, thử upgrade xe hiện tại lên loại lớn hơn
//                VehicleRuleEntity currentRule = currentContainer.rule;
//                VehicleRuleEntity upgradedRule = findNextBiggerRule(currentRule, sortedVehicleRules);
//
//                while (upgradedRule != null && !assigned) {
//                    BinPacker.ContainerState upgradedContainer = BinPacker.upgradeContainer(currentContainer, upgradedRule);
//                    if (upgradedContainer != null) {
//                        BinPacker.Placement upgradedPlacement = BinPacker.tryPlaceBoxInContainer(box, upgradedContainer);
//                        if (upgradedPlacement != null) {
//                            upgradedContainer.addPlacement(upgradedPlacement);
//                            containers.set(ci, upgradedContainer); // Thay thế xe cũ bằng xe đã upgrade
//                            assigned = true;
//                            break;
//                        }
//                    }
//                    // Thử loại xe lớn hơn nữa
//                    upgradedRule = findNextBiggerRule(upgradedRule, sortedVehicleRules);
//                }
//
//                if (assigned) break;
//            }
//
//            // 5. Nếu chưa gán được, mở xe mới với loại xe NHỎ NHẤT có thể chở
//            if (!assigned) {
//                BinPacker.ContainerState newContainer = openNewContainerForBox(box, sortedVehicleRules);
//                if (newContainer != null) {
//                    containers.add(newContainer);
//                } else {
//                    throw new RuntimeException("Không có loại xe nào chở được kiện: " + box.id);
//                }
//            }
//        }
//
//        // 6. Convert kết quả
//        List<ContractRuleAssignResponse> responses = BinPacker.toContractResponses(containers, details);
//
//        log.info("[assignVehiclesOptimal] Completed. vehiclesUsed={}, detailsProcessed={}",
//                responses.size(), details.size());
//        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
//        log.info("[assignVehiclesOptimal] Finished in {} ms", elapsedMs);
//
//        return responses;
//    }
//
//    /**
//     * Mở container mới với loại xe NHỎ NHẤT có thể chở box
//     */
//    private BinPacker.ContainerState openNewContainerForBox(BinPacker.BoxItem box, List<VehicleRuleEntity> sortedRules) {
//        // Tìm xe NHỎ NHẤT có thể chở box này
//        for (VehicleRuleEntity rule : sortedRules) {
//            int maxX = BinPacker.convertToInt(rule.getMaxLength());
//            int maxY = BinPacker.convertToInt(rule.getMaxWidth());
//            int maxZ = BinPacker.convertToInt(rule.getMaxHeight());
//            long maxWeight = BinPacker.convertWeightToLong(rule.getMaxWeight());
//
//            // Kiểm tra box có vừa với xe không
//            boolean fitsDimensions = box.lx <= maxX && box.ly <= maxY && box.lz <= maxZ;
//            boolean fitsWeight = box.weight <= maxWeight;
//
//            if (fitsDimensions && fitsWeight) {
//                BinPacker.ContainerState newContainer = new BinPacker.ContainerState(rule, maxX, maxY, maxZ);
//                BinPacker.Placement placement = BinPacker.tryPlaceBoxInContainer(box, newContainer);
//                if (placement != null) {
//                    newContainer.addPlacement(placement);
//                    log.info("[openNewContainerForBox] Selected smallest vehicle: {} for box {}",
//                            rule.getVehicleRuleName(), box.id);
//                    return newContainer;
//                }
//            }
//        }
//        return null;
//    }
//
//    /**
//     * Tìm vehicle rule lớn hơn tiếp theo
//     */
//    private VehicleRuleEntity findNextBiggerRule(VehicleRuleEntity current, List<VehicleRuleEntity> sorted) {
//        int currentIndex = -1;
//        for (int i = 0; i < sorted.size(); i++) {
//            if (sorted.get(i).getId().equals(current.getId())) {
//                currentIndex = i;
//                break;
//            }
//        }
//
//        if (currentIndex != -1 && currentIndex + 1 < sorted.size()) {
//            return sorted.get(currentIndex + 1);
//        }
//        return null;
//    }
//
//    // Trong assignVehiclesOptimal, thêm logging:
//    private BinPacker.ContainerState findFittingContainer(BinPacker.BoxItem box,
//                                                          List<BinPacker.ContainerState> containers) {
//        for (BinPacker.ContainerState container : containers) {
//            BinPacker.Placement placement = BinPacker.tryPlaceBoxInContainer(box, container);
//            if (placement != null) {
//                log.debug("[findFittingContainer] Box {} fits in existing vehicle {}",
//                        box.id, container.rule.getVehicleRuleName());
//                return container;
//            }
//        }
//        return null;
//    }
//
//    /**
//     * Tìm vehicle rule lớn hơn rule hiện tại trong sorted list
//     */
////    private VehicleRuleEntity findNextBiggerRule(VehicleRuleEntity current, List<VehicleRuleEntity> sorted) {
////        for (int i = 0; i < sorted.size(); i++) {
////            if (sorted.get(i).getId().equals(current.getId()) && i + 1 < sorted.size()) {
////                return sorted.get(i + 1);
////            }
////        }
////        return null;
////    }
//
//
//    private OrderDetailForPackingResponse toPackingResponse(OrderDetailEntity entity) {
//        return new OrderDetailForPackingResponse(
//                entity.getId().toString(),
//                entity.getWeight(),
//                entity.getWeightBaseUnit(),
//                entity.getUnit(),
//                entity.getTrackingCode()
//        );
//    }
//
//    private BigDecimal calculateTotalWeight(ContractRuleAssignResponse assignment, OrderDetailEntity newDetail) {
//        return assignment.getCurrentLoad().add(newDetail.getWeight());
//    }
//
//    private VehicleRuleEntity tryUpgrade(OrderDetailEntity detail,
//                                         ContractRuleAssignResponse assignment,
//                                         List<VehicleRuleEntity> sortedRules) {
//
//        int currentIdx = assignment.getVehicleIndex();
//
//        for (int nextIdx = currentIdx + 1; nextIdx < sortedRules.size(); nextIdx++) {
//            VehicleRuleEntity nextRule = sortedRules.get(nextIdx);
//
//            if (canFit(detail, nextRule, assignment)) {
//                // cập nhật lại index cho assignment
//                assignment.setVehicleIndex(nextIdx);
//                return nextRule;
//            }
//        }
//        return null;
//    }
//
//
//    /**
//     * Upgrade vehicle if possible
//     */
////    private int tryUpgrade(OrderDetailEntity detail,
////                           ContractRuleAssignResponse assignment,
////                           List<VehicleRuleEntity> sortedVehicleRules,
////                           Map<Integer, VehicleRuleEntity> vehicleRuleCache) {
////        int currentIdx = assignment.getVehicleIndex();
////        log.info("[tryUpgrade] Try upgrade for detailId={} from index={}", detail.getId(), currentIdx);
////        for (int i = currentIdx + 1; i < sortedVehicleRules.size(); i++) {
////            VehicleRuleEntity biggerRule = vehicleRuleCache.get(i);
////            if (biggerRule == null) {
////                log.warn("[tryUpgrade] Missing vehicle rule at index={}", i);
////                continue;
////            }
////            if (canFitAll(assignment.getAssignedDetails(), biggerRule, detail)) {
////                log.info("[tryUpgrade] Upgrade possible to index={}, ruleId={}", i, biggerRule.getId());
////                return i;
////            }
////        }
////        log.info("[tryUpgrade] No upgrade possible for detailId={}", detail.getId());
////        return -1;
////    }
//
////    private VehicleRuleEntity tryUpgrade(OrderDetailEntity detail,
////                                         ContractRuleAssignResponse assignment,
////                                         List<VehicleRuleEntity> sortedVehicleRules,
////                                         Map<UUID, VehicleRuleEntity> vehicleRuleById,
////                                         Map<UUID, Integer> ruleIndexById) {
////        UUID currentRuleId = assignment.getVehicleRuleId();
////        Integer startIdx = ruleIndexById.get(currentRuleId);
////        if (startIdx == null) {
////            log.warn("[tryUpgrade] cannot find index for ruleId={}", currentRuleId);
////            return null;
////        }
////        log.info("[tryUpgrade] Try upgrade for detailId={} from ruleIndex={}", detail.getId(), startIdx);
////        for (int i = startIdx + 1; i < sortedVehicleRules.size(); i++) {
////            VehicleRuleEntity biggerRule = sortedVehicleRules.get(i);
////            if (canFitAll(assignment.getAssignedDetails(), biggerRule, detail)) {
////                log.info("[tryUpgrade] Upgrade possible to index={}, ruleId={}", i, biggerRule.getId());
////                return biggerRule;
////            }
////        }
////        log.info("[tryUpgrade] No upgrade possible for detailId={}", detail.getId());
////        return null;
////    }
//
////    private VehicleRuleEntity tryUpgrade(OrderDetailEntity detail,
////                                         ContractRuleAssignResponse assignment,
////                                         List<VehicleRuleEntity> sortedRules) {
////
////        int currentIdx = assignment.getVehicleIndex();
////
////        for (int nextIdx = currentIdx + 1; nextIdx < sortedRules.size(); nextIdx++) {
////            VehicleRuleEntity nextRule = sortedRules.get(nextIdx);
////
////            if (canFit(detail, nextRule, assignment)) {
////                // cập nhật lại index cho assignment
////                assignment.setVehicleIndex(nextIdx);
////                return nextRule;
////            }
////        }
////        return null;
////    }
//    @Override
//    public PriceCalculationResponse calculateTotalPrice(ContractEntity contract,
//                                                        BigDecimal distanceKm,
//                                                        Map<UUID, Integer> vehicleCountMap) {
//        final long t0 = System.nanoTime();
//
//        if (contract.getOrderEntity() == null || contract.getOrderEntity().getCategory() == null) {
//            throw new BadRequestException("Contract missing order/category", ErrorEnum.INVALID.getErrorCode());
//        }
//
//        List<DistanceRuleEntity> distanceRules = distanceRuleEntityService.findAll()
//                .stream()
//                .sorted(Comparator.comparing(DistanceRuleEntity::getFromKm))
//                .toList();
//
//        if (distanceRules.isEmpty()) {
//            throw new RuntimeException("No distance rules found");
//        }
//
//        BigDecimal total = BigDecimal.ZERO;
//        List<PriceCalculationResponse.CalculationStep> steps = new ArrayList<>();
//
//        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
//            UUID vehicleRuleId = entry.getKey();
//            int numOfVehicles = entry.getValue();
//
//            VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findEntityById(vehicleRuleId)
//                    .orElseThrow(() -> new NotFoundException("Vehicle rule not found: " + vehicleRuleId,
//                            ErrorEnum.NOT_FOUND.getErrorCode()));
//
//            BigDecimal ruleTotal = BigDecimal.ZERO;
//            BigDecimal remaining = distanceKm;
//
//            for (DistanceRuleEntity distanceRule : distanceRules) {
//                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;
//
//                BigDecimal from = distanceRule.getFromKm();
//                BigDecimal to = distanceRule.getToKm();
//
//                BasingPriceEntity basePriceEntity = basingPriceEntityService
//                        .findBasingPriceEntityByVehicleRuleEntityIdAndDistanceRuleEntityId(
//                                vehicleRule.getId(), distanceRule.getId())
//                        .orElseThrow(() -> new RuntimeException("No base price found for tier "
//                                + from + "-" + to + " and vehicleRule=" + vehicleRule.getId()));
//
//                if (from.compareTo(BigDecimal.ZERO) == 0 && to.compareTo(BigDecimal.valueOf(4)) == 0) {
//                    // fixed tier
//                    ruleTotal = ruleTotal.add(basePriceEntity.getBasePrice());
//                    remaining = remaining.subtract(to);
//
//                    steps.add(PriceCalculationResponse.CalculationStep.builder()
//                            .vehicleRuleName(vehicleRule.getVehicleRuleName())
//                            .numOfVehicles(numOfVehicles)
//                            .distanceRange("0-4 km")
//                            .unitPrice(basePriceEntity.getBasePrice())
//                            .appliedKm(BigDecimal.valueOf(4))
//                            .subtotal(basePriceEntity.getBasePrice())
//                            .build());
//
//                } else {
//                    BigDecimal tierDistance = (to == null) ? remaining : remaining.min(to.subtract(from));
//                    BigDecimal add = basePriceEntity.getBasePrice().multiply(tierDistance);
//                    ruleTotal = ruleTotal.add(add);
//                    remaining = remaining.subtract(tierDistance);
//
//                    steps.add(PriceCalculationResponse.CalculationStep.builder()
//                            .vehicleRuleName(vehicleRule.getVehicleRuleName())
//                            .numOfVehicles(numOfVehicles)
//                            .distanceRange(from + "-" + (to == null ? "∞" : to) + " km")
//                            .unitPrice(basePriceEntity.getBasePrice())
//                            .appliedKm(tierDistance)
//                            .subtotal(add)
//                            .build());
//                }
//            }
//
//            if (numOfVehicles > 0) {
//                ruleTotal = ruleTotal.multiply(BigDecimal.valueOf(numOfVehicles));
//            }
//
//            total = total.add(ruleTotal);
//        }
//
//        BigDecimal totalBeforeAdjustment = total;
//        BigDecimal categoryExtraFee = BigDecimal.ZERO;
//        BigDecimal categoryMultiplier = BigDecimal.ONE;
//        BigDecimal promotionDiscount = BigDecimal.ZERO;
//
//        CategoryPricingDetailEntity adjustment = categoryPricingDetailEntityService.findByCategoryId(contract.getOrderEntity().getCategory().getId());
//        if (adjustment != null) {
//            categoryMultiplier = adjustment.getPriceMultiplier() != null ? adjustment.getPriceMultiplier() : BigDecimal.ONE;
//            categoryExtraFee = adjustment.getExtraFee() != null ? adjustment.getExtraFee() : BigDecimal.ZERO;
//
//            BigDecimal adjustedTotal = total.multiply(categoryMultiplier).add(categoryExtraFee);
//
//            steps.add(PriceCalculationResponse.CalculationStep.builder()
//                    .vehicleRuleName("Điều chỉnh loại hàng: " + contract.getOrderEntity().getCategory().getCategoryName())
//                    .numOfVehicles(0)
//                    .distanceRange("×" + categoryMultiplier + " + " + categoryExtraFee + " VND")
//                    .unitPrice(categoryMultiplier)
//                    .appliedKm(BigDecimal.ZERO)
//                    .subtotal(adjustedTotal.subtract(total)) // phần chênh lệch
//                    .build());
//
//            total = adjustedTotal;
//        }
//
//        if (promotionDiscount.compareTo(BigDecimal.ZERO) > 0) {
//            steps.add(PriceCalculationResponse.CalculationStep.builder()
//                    .vehicleRuleName("Khuyến mãi")
//                    .numOfVehicles(0)
//                    .distanceRange("N/A")
//                    .unitPrice(promotionDiscount.negate())
//                    .appliedKm(BigDecimal.ZERO)
//                    .subtotal(promotionDiscount.negate())
//                    .build());
//
//            total = total.subtract(promotionDiscount);
//        }
//
//
//        if (total.compareTo(BigDecimal.ZERO) < 0) {
//            throw new IllegalArgumentException("Total price must not be negative");
//        }
//
////        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
//
//        return PriceCalculationResponse.builder()
//                .totalPrice(total)
//                .totalBeforeAdjustment(totalBeforeAdjustment)
//                .categoryExtraFee(categoryExtraFee)
//                .categoryMultiplier(categoryMultiplier)
//                .promotionDiscount(promotionDiscount)
//                .finalTotal(total)
//                .steps(steps)
////                .summary("Tổng giá trị hợp đồng: " + total + " (tính trong " + elapsedMs + " ms)")
//                .build();
//    }
//
//
//    private boolean canFit(OrderDetailEntity detail, VehicleRuleEntity rule) {
//        OrderSizeEntity size = detail.getOrderSizeEntity();
//        if (size == null) return false;
//
//        return detail.getWeight().compareTo(rule.getMaxWeight()) <= 0
//                && size.getMaxLength().compareTo(rule.getMaxLength()) <= 0
//                && size.getMaxWidth().compareTo(rule.getMaxWidth()) <= 0
//                && size.getMaxHeight().compareTo(rule.getMaxHeight()) <= 0;
//    }
//
//    private boolean canFit(OrderDetailEntity detail, VehicleRuleEntity rule, ContractRuleAssignResponse assignment) {
//        OrderSizeEntity size = detail.getOrderSizeEntity();
//        if (size == null) return false;
//
//        BigDecimal newLoad = assignment.getCurrentLoad().add(detail.getWeight());
//        return newLoad.compareTo(rule.getMaxWeight()) <= 0
//                && size.getMaxLength().compareTo(rule.getMaxLength()) <= 0
//                && size.getMaxWidth().compareTo(rule.getMaxWidth()) <= 0
//                && size.getMaxHeight().compareTo(rule.getMaxHeight()) <= 0;
//    }
//
//    private boolean canFitAll(List<UUID> detailIds, VehicleRuleEntity newRule, OrderDetailEntity newDetail) {
//        BigDecimal totalWeight = newDetail.getWeight();
//
//        for (UUID id : detailIds) {
//            OrderDetailEntity d = orderDetailEntityService.findEntityById(id)
//                    .orElseThrow(() -> new NotFoundException("Order detail not found: " + id, ErrorEnum.NOT_FOUND.getErrorCode()));
//
//            totalWeight = totalWeight.add(d.getWeight());
//
//            OrderSizeEntity size = d.getOrderSizeEntity();
//            if (size == null) {
//                log.warn("[canFitAll] Detail id={} missing size", id);
//                return false;
//            }
//
//            if (size.getMaxLength().compareTo(newRule.getMaxLength()) > 0
//                    || size.getMaxWidth().compareTo(newRule.getMaxWidth()) > 0
//                    || size.getMaxHeight().compareTo(newRule.getMaxHeight()) > 0) {
//                log.info("[canFitAll] Dimension exceed for detailId={} vs ruleId={}", id, newRule.getId());
//                return false;
//            }
//        }
//
//        boolean ok = totalWeight.compareTo(newRule.getMaxWeight()) <= 0;
//        if (!ok) {
//            log.info("[canFitAll] Overweight: totalWeight={} > ruleMax={}", totalWeight, newRule.getMaxWeight());
//        }
//        return ok;
//    }
//
//    @Override
//    public BigDecimal calculateDistanceKm(AddressEntity from, AddressEntity to) {
//        double lat1 = from.getLatitude().doubleValue();
//        double lon1 = from.getLongitude().doubleValue();
//        double lat2 = to.getLatitude().doubleValue();
//        double lon2 = to.getLongitude().doubleValue();
//
//        log.info("Pickup coords: lat={}, lon={}", lat1, lon1);
//        log.info("Delivery coords: lat={}, lon={}", lat2, lon2);
//
//        if (lat1 == lat2 && lon1 == lon2) {
//            return BigDecimal.ZERO;
//        }
//
//        double dLat = Math.toRadians(lat2 - lat1);
//        double dLon = Math.toRadians(lon2 - lon1);
//        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
//                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
//                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
//
//        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
//        double distanceKm = EARTH_RADIUS_KM * c;
//
//        log.info("Calculated raw distance: {} km", distanceKm);
//        return BigDecimal.valueOf(distanceKm);
//    }
//
//
//    // CONTRACT TO CLOUD
//
//
//    @Override
//    public ContractResponse uploadContractFile(ContractFileUploadRequest req) throws IOException {
//        log.info("Uploading contract file for contractId={}", req.contractId());
//        String fileName = "contract_" + UUID.randomUUID();
//
//        // upload Cloudinary
//        var uploadResult = cloudinaryService.uploadFile(
//                req.file().getBytes(),
//                fileName,
//                "CONTRACTS"
//        );
//
//
//        String imageUrl = uploadResult.get("secure_url").toString();
//
//        // load relationships
//        ContractEntity ce = contractEntityService.findEntityById(req.contractId())
//                .orElseThrow(() -> new RuntimeException("Contract not found by id: " + req.contractId()));
//
//        // save DB
//        ce.setAttachFileUrl(imageUrl);
//        ce.setDescription(req.description());
//        ce.setEffectiveDate(req.effectiveDate());
//        ce.setExpirationDate(req.expirationDate());
//        ce.setSupportedValue(req.supportedValue());
//        ce.setContractName(req.contractName());
//
//
//        var updated = contractEntityService.save(ce);
//        return contractMapper.toContractResponse(updated);
//
//    }
//
//    @Override
//    public ContractResponse getContractByOrderId(UUID orderId) {
//        log.info("Getting contract by Order ID: {}", orderId);
//        ContractEntity contractEntity = contractEntityService.getContractByOrderId(orderId)
//                .orElseThrow(() -> new NotFoundException(
//                        "Contract not found for order ID: " + orderId,
//                        ErrorEnum.NOT_FOUND.getErrorCode()
//                ));
//        return contractMapper.toContractResponse(contractEntity);
//    }
//}
