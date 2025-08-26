package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ContractStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.ContractRequest;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.entity.order.order.CategoryPricingDetailEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.pricing.BasingPriceEntity;
import capstone_project.entity.pricing.DistanceRuleEntity;
import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.service.entityServices.order.contract.ContractEntityService;
import capstone_project.service.entityServices.order.contract.ContractRuleEntityService;
import capstone_project.service.entityServices.order.order.CategoryPricingDetailEntityService;
import capstone_project.service.entityServices.order.order.OrderDetailEntityService;
import capstone_project.service.entityServices.order.order.OrderEntityService;
import capstone_project.service.entityServices.pricing.BasingPriceEntityService;
import capstone_project.service.entityServices.pricing.DistanceRuleEntityService;
import capstone_project.service.entityServices.pricing.VehicleRuleEntityService;
import capstone_project.service.mapper.order.ContractMapper;
import capstone_project.service.services.order.order.ContractService;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@AllArgsConstructor
public class ContractServiceImpl implements ContractService {

    private final ContractEntityService contractEntityService;
    private final ContractRuleEntityService contractRuleEntityService;
    private final VehicleRuleEntityService vehicleRuleEntityService;
    private final CategoryPricingDetailEntityService categoryPricingDetailEntityService;
    private final OrderEntityService orderEntityService;
    private final DistanceRuleEntityService distanceRuleEntityService;
    private final BasingPriceEntityService basingPriceEntityService;
    private final OrderDetailEntityService orderDetailEntityService;

    private final ContractMapper contractMapper;

    private static final double EARTH_RADIUS_KM = 6371.0;

    @Override
    public List<ContractResponse> getAllContracts() {
        log.info("Getting all contracts");
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
        log.info("Getting contract by ID: {}", id);
        ContractEntity contractEntity = contractEntityService.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        return contractMapper.toContractResponse(contractEntity);
    }

    @Override
    @Transactional
    public ContractResponse createContract(ContractRequest contractRequest) {
        log.info("Creating new contract");

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

        OrderEntity order = orderEntityService.findById(orderUuid)
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
        log.info("Creating new contract");

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
        }

        // lấy order
        OrderEntity order = orderEntityService.findById(orderUuid)
                .orElseThrow(() -> {
                    log.error("[createBoth] Order not found: {}", orderUuid);
                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        BigDecimal distanceKm = calculateDistanceKm(order.getPickupAddress(), order.getDeliveryAddress());

        // 1. map request -> ContractEntity và save trước (chưa set totalValue)
        ContractEntity contractEntity = contractMapper.mapRequestToEntity(contractRequest);
        contractEntity.setStatus(ContractStatusEnum.CONTRACT_DRAFT.name());
        contractEntity.setOrderEntity(order); // gắn với order

        ContractEntity savedContract = contractEntityService.save(contractEntity);

        List<ContractRuleAssignResponse> assignments = assignVehicles(orderUuid);

        Map<UUID, Integer> vehicleCountMap = assignments.stream()
                .collect(Collectors.groupingBy(ContractRuleAssignResponse::getVehicleRuleId, Collectors.summingInt(a -> 1)));


        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
            UUID vehicleRuleId = entry.getKey();
            Integer count = entry.getValue();

            VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findById(vehicleRuleId)
                    .orElseThrow(() -> {
                        log.error("[createBoth] Vehicle rule not found: {}", vehicleRuleId);
                        return new NotFoundException("Vehicle rule not found", ErrorEnum.NOT_FOUND.getErrorCode());
                    });

            ContractRuleEntity contractRule = ContractRuleEntity.builder()
                    .contractEntity(savedContract)
                    .vehicleRuleEntity(vehicleRule)
                    .numOfVehicles(count.intValue())
                    .status(CommonStatusEnum.ACTIVE.name())
                    .build();

            contractRuleEntityService.save(contractRule);
        }

        BigDecimal totalPrice = calculateTotalPrice(savedContract, distanceKm, vehicleCountMap);

        savedContract.setTotalValue(totalPrice);
        ContractEntity updatedContract = contractEntityService.save(savedContract);

        return contractMapper.toContractResponse(updatedContract);
    }


    @Override
    public ContractResponse updateContract(UUID id, ContractRequest contractRequest) {
        return null;
    }

    @Override
    public void deleteContract(UUID id) {

    }

    public List<ContractRuleAssignResponse> assignVehicles(UUID orderId) {
        final long t0 = System.nanoTime();
        log.info("Assigning vehicles for order ID: {}", orderId);

        List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
        if (details.isEmpty()) {
            log.error("[assignVehicles] Order not found: {}", orderId);
            throw new NotFoundException("No order details found for this order", ErrorEnum.NOT_FOUND.getErrorCode());
        }

        OrderEntity orderEntity = orderEntityService.findById(orderId)
                .orElseThrow(() -> {
                    log.error("[assignVehicles] Order not found: {}", orderId);
                    return new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode());
                });

        // tu tu chua sure
        if (orderEntity.getCategory() == null) {
            log.error("[assignVehicles] Order category is null for orderId={}", orderId);
            throw new BadRequestException("Order category is required", ErrorEnum.INVALID.getErrorCode());
        }

        List<VehicleRuleEntity> sortedVehicleRules = vehicleRuleEntityService
                .findAllByCategoryId(orderEntity.getCategory().getId())
                .stream()
                .sorted(Comparator.comparing(VehicleRuleEntity::getMaxWeight)
                        .thenComparing(VehicleRuleEntity::getMaxLength)
                        .thenComparing(VehicleRuleEntity::getMaxWidth)
                        .thenComparing(VehicleRuleEntity::getMaxHeight))
                .toList();

        if (sortedVehicleRules.isEmpty()) {
            log.error("[assignVehicles] No vehicle rules found for categoryId={}", orderEntity.getCategory().getId());
            throw new NotFoundException("No vehicle rules found for this category", ErrorEnum.NOT_FOUND.getErrorCode());
        }

        details.sort((a, b) -> {
            int cmp = b.getWeight().compareTo(a.getWeight());
            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxLength().compareTo(a.getOrderSizeEntity().getMaxLength());
            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxWidth().compareTo(a.getOrderSizeEntity().getMaxWidth());
            if (cmp == 0) cmp = b.getOrderSizeEntity().getMaxHeight().compareTo(a.getOrderSizeEntity().getMaxHeight());
            return cmp;
        });

        Map<Integer, VehicleRuleEntity> vehicleRuleCache = new HashMap<>();
        for (int i = 0; i < sortedVehicleRules.size(); i++) {
            vehicleRuleCache.put(i, sortedVehicleRules.get(i));
        }

        List<ContractRuleAssignResponse> assignments = new ArrayList<>();
        int processed = 0;

        // Gán kiện vào xe
        for (OrderDetailEntity detail : details) {
            processed++;
            if (detail.getOrderSizeEntity() == null) {
                log.warn("[assignVehicles] Detail id={} missing orderSize, cannot assign", detail.getId());
                throw new BadRequestException(
                        "Order detail is missing size information: " + detail.getId(),
                        ErrorEnum.INVALID.getErrorCode()
                );
            }

            log.info("[assignVehicles] Processing detail {}/{}: id={}, weight={}; size.max={}kg,{}x{}x{}",
                    processed, details.size(), detail.getId(), detail.getWeight(),
//                    detail.getLength(), detail.getWidth(), detail.getHeight(),
                    detail.getOrderSizeEntity().getMaxWeight(),
                    detail.getOrderSizeEntity().getMaxLength(),
                    detail.getOrderSizeEntity().getMaxWidth(),
                    detail.getOrderSizeEntity().getMaxHeight());

            boolean assigned = false;

            // thử gán vào xe đã mở
            for (ContractRuleAssignResponse assignment : assignments) {
                VehicleRuleEntity currentRule = vehicleRuleCache.get(assignment.getVehicleIndex());
                if (currentRule == null) {
                    log.error("[assignVehicles] Missing vehicle rule in cache for index={}", assignment.getVehicleIndex());
                    continue;
                }

                if (canFit(detail, currentRule, assignment)) {
                    assignment.setCurrentLoad(assignment.getCurrentLoad().add(detail.getWeight()));
                    assignment.getAssignedDetails().add(detail.getId());
                    log.info("[assignVehicles] Assigned detailId={} to existing vehicle index={}, ruleId={}, newLoad={}",
                            detail.getId(), assignment.getVehicleIndex(), currentRule.getId(), assignment.getCurrentLoad());
                    assigned = true;
                    break;
                }

                int upgradedIdx = tryUpgrade(detail, assignment, sortedVehicleRules, vehicleRuleCache);
                if (upgradedIdx >= 0) {
                    VehicleRuleEntity upgradedRule = vehicleRuleCache.get(upgradedIdx);
                    assignment.setVehicleIndex(upgradedIdx);
                    assignment.setCurrentLoad(assignment.getCurrentLoad().add(detail.getWeight()));
                    assignment.getAssignedDetails().add(detail.getId());
                    log.info("[assignVehicles] Upgraded vehicle for detailId={} to index={}, ruleId={}, newLoad={}",
                            detail.getId(), upgradedIdx, upgradedRule.getId(), assignment.getCurrentLoad());
                    assigned = true;
                    break;
                }
            }

            // nếu chưa gán thì mở xe mới
            if (!assigned) {
                for (int i = 0; i < sortedVehicleRules.size(); i++) {
                    VehicleRuleEntity rule = sortedVehicleRules.get(i);
                    if (canFit(detail, rule)) {
                        ContractRuleAssignResponse newAssignment = new ContractRuleAssignResponse(
                                i,
                                rule.getId(),
                                rule.getVehicleRuleName(),
                                detail.getWeight(),
                                new ArrayList<>(List.of(detail.getId()))
                        );
                        assignments.add(newAssignment);
                        log.info("[assignVehicles] Opened new vehicle for detailId={}, index={}, ruleId={}, firstLoad={}",
                                detail.getId(), i, rule.getId(), detail.getWeight());
                        assigned = true;
                        break;
                    }
                }
            }

            if (!assigned) {
                log.error("[assignVehicles] No vehicle can carry detailId={}", detail.getId());
                throw new RuntimeException("Không có loại xe nào chở được kiện " + detail.getId());
            }
        }

        log.info("[assignVehicles] Completed. vehiclesUsed={}, detailsProcessed={}", assignments.size(), processed);
        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
        log.info("[assignVehicles] Finished in {} ms", elapsedMs);
        return assignments;
    }

    /**
     * Upgrade vehicle if possible
     */
    private int tryUpgrade(OrderDetailEntity detail,
                           ContractRuleAssignResponse assignment,
                           List<VehicleRuleEntity> sortedVehicleRules,
                           Map<Integer, VehicleRuleEntity> vehicleRuleCache) {
        int currentIdx = assignment.getVehicleIndex();
        log.info("[tryUpgrade] Try upgrade for detailId={} from index={}", detail.getId(), currentIdx);
        for (int i = currentIdx + 1; i < sortedVehicleRules.size(); i++) {
            VehicleRuleEntity biggerRule = vehicleRuleCache.get(i);
            if (biggerRule == null) {
                log.warn("[tryUpgrade] Missing vehicle rule at index={}", i);
                continue;
            }
            if (canFitAll(assignment.getAssignedDetails(), biggerRule, detail)) {
                log.info("[tryUpgrade] Upgrade possible to index={}, ruleId={}", i, biggerRule.getId());
                return i;
            }
        }
        log.info("[tryUpgrade] No upgrade possible for detailId={}", detail.getId());
        return -1;
    }

    @Override
    public BigDecimal calculateTotalPrice(ContractEntity contract,
                                          BigDecimal distanceKm,
                                          Map<UUID, Integer> vehicleCountMap) {
        final long t0 = System.nanoTime();
        log.info("[calcTotal] Start for contractId={}, distanceKm={}, vehicleKinds={}",
                contract.getId(), distanceKm, vehicleCountMap.size());

        if (contract.getOrderEntity() == null || contract.getOrderEntity().getCategory() == null) {
            log.error("[calcTotal] Contract missing order/category. contractId={}", contract.getId());
            throw new BadRequestException("Contract missing order/category", ErrorEnum.INVALID.getErrorCode());
        }

        List<DistanceRuleEntity> distanceRules = distanceRuleEntityService.findAll()
                .stream()
                .sorted(Comparator.comparing(DistanceRuleEntity::getFromKm))
                .toList();

        log.info("[calcTotal] Distance rules loaded: count={}", distanceRules.size());
        if (distanceRules.isEmpty()) {
            log.error("[calcTotal] No distance rules found");
            throw new RuntimeException("No distance rules found");
        }

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
            UUID vehicleRuleId = entry.getKey();
            int numOfVehicles = entry.getValue();

            VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findById(vehicleRuleId)
                    .orElseThrow(() -> {
                        log.error("[calcTotal] Vehicle rule not found: {}", vehicleRuleId);
                        return new NotFoundException("Vehicle rule not found: " + vehicleRuleId,
                                ErrorEnum.NOT_FOUND.getErrorCode());
                    });

            log.info("[calcTotal] VehicleRule id={} name={} vehicles={}",
                    vehicleRule.getId(), vehicleRule.getVehicleRuleName(), numOfVehicles);

            BigDecimal ruleTotal = BigDecimal.ZERO;
            BigDecimal remaining = distanceKm;

            for (DistanceRuleEntity distanceRule : distanceRules) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal from = distanceRule.getFromKm();
                BigDecimal to = distanceRule.getToKm();

                BasingPriceEntity basePriceEntity = basingPriceEntityService
                        .findBasingPriceEntityByVehicleRuleEntityIdAndDistanceRuleEntityId(
                                vehicleRule.getId(), distanceRule.getId())
                        .orElseThrow(() -> {
                            log.error("[calcTotal] No base price for tier {}-{} and vehicleRule={}", from, to, vehicleRule.getId());
                            return new RuntimeException("No base price found for tier "
                                    + from + "-" + to + " and vehicleRule=" + vehicleRule.getId());
                        });

                if (from.compareTo(BigDecimal.ZERO) == 0 && to.compareTo(BigDecimal.valueOf(4)) == 0) {
                    ruleTotal = ruleTotal.add(basePriceEntity.getBasePrice());
                    remaining = remaining.subtract(to);
                    log.info("[calcTotal] Applied fixed tier 0-4km base={}, remaining={}",
                            basePriceEntity.getBasePrice(), remaining);
                } else {
                    BigDecimal tierDistance = (to == null) ? remaining : remaining.min(to.subtract(from));
                    BigDecimal add = basePriceEntity.getBasePrice().multiply(tierDistance);
                    ruleTotal = ruleTotal.add(add);
                    remaining = remaining.subtract(tierDistance);
                    log.info("[calcTotal] Applied tier {}-{}km price={} * {}km = {}, remaining={}",
                            from, to, basePriceEntity.getBasePrice(), tierDistance, add, remaining);
                }
            }

            if (numOfVehicles > 0) {
                ruleTotal = ruleTotal.multiply(BigDecimal.valueOf(numOfVehicles));
                log.info("[calcTotal] Multiply by vehicles={}, subtotal={}", numOfVehicles, ruleTotal);
            }

            total = total.add(ruleTotal);
        }

        CategoryPricingDetailEntity adjustment =
                categoryPricingDetailEntityService.findByCategoryId(contract.getOrderEntity().getCategory().getId());
        if (adjustment != null) {
            BigDecimal multiplier = adjustment.getPriceMultiplier() != null ? adjustment.getPriceMultiplier() : BigDecimal.ONE;
            BigDecimal extraFee = adjustment.getExtraFee() != null ? adjustment.getExtraFee() : BigDecimal.ZERO;
            total = total.multiply(multiplier).add(extraFee);
            log.info("[calcTotal] Category adjustment applied: multiplier={}, extraFee={}, total={}",
                    multiplier, extraFee, total);
        } else {
            log.info("[calcTotal] No category adjustment found");
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            log.error("[calcTotal] Total price negative: {}", total);
            throw new IllegalArgumentException("Total price must not be negative");
        }

        long elapsedMs = (System.nanoTime() - t0) / 1_000_000;
        log.info("[calcTotal] Final total={} for contractId={}, computed in {} ms", total, contract.getId(), elapsedMs);
        return total;
    }

    private boolean canFit(OrderDetailEntity detail, VehicleRuleEntity rule) {
        OrderSizeEntity size = detail.getOrderSizeEntity();
        if (size == null) return false;

        return size.getMaxWeight().compareTo(rule.getMaxWeight()) <= 0
                && size.getMaxLength().compareTo(rule.getMaxLength()) <= 0
                && size.getMaxWidth().compareTo(rule.getMaxWidth()) <= 0
                && size.getMaxHeight().compareTo(rule.getMaxHeight()) <= 0;
    }

    private boolean canFit(OrderDetailEntity detail, VehicleRuleEntity rule, ContractRuleAssignResponse assignment) {
        OrderSizeEntity size = detail.getOrderSizeEntity();
        if (size == null) return false;

        BigDecimal newLoad = assignment.getCurrentLoad().add(detail.getWeight());
        return newLoad.compareTo(rule.getMaxWeight()) <= 0
                && size.getMaxLength().compareTo(rule.getMaxLength()) <= 0
                && size.getMaxWidth().compareTo(rule.getMaxWidth()) <= 0
                && size.getMaxHeight().compareTo(rule.getMaxHeight()) <= 0;
    }

    private boolean canFitAll(List<UUID> detailIds, VehicleRuleEntity newRule, OrderDetailEntity newDetail) {
        // Sửa bug: detailIds là ID của OrderDetail, cần findById từng detail
        BigDecimal totalWeight = newDetail.getWeight();

        for (UUID id : detailIds) {
            OrderDetailEntity d = orderDetailEntityService.findById(id)
                    .orElseThrow(() -> new NotFoundException("Order detail not found: " + id, ErrorEnum.NOT_FOUND.getErrorCode()));

            totalWeight = totalWeight.add(d.getWeight());

            OrderSizeEntity size = d.getOrderSizeEntity();
            if (size == null) {
                log.warn("[canFitAll] Detail id={} missing size", id);
                return false;
            }

            if (size.getMaxLength().compareTo(newRule.getMaxLength()) > 0
                    || size.getMaxWidth().compareTo(newRule.getMaxWidth()) > 0
                    || size.getMaxHeight().compareTo(newRule.getMaxHeight()) > 0) {
                log.info("[canFitAll] Dimension exceed for detailId={} vs ruleId={}", id, newRule.getId());
                return false;
            }
        }

        boolean ok = totalWeight.compareTo(newRule.getMaxWeight()) <= 0;
        if (!ok) {
            log.info("[canFitAll] Overweight: totalWeight={} > ruleMax={}", totalWeight, newRule.getMaxWeight());
        }
        return ok;
    }

    @Override
    public BigDecimal calculateDistanceKm(AddressEntity from, AddressEntity to) {
        double lat1 = from.getLatitude().doubleValue();
        double lon1 = from.getLongitude().doubleValue();
        double lat2 = to.getLatitude().doubleValue();
        double lon2 = to.getLongitude().doubleValue();

        log.info("Pickup coords: lat={}, lon={}", lat1, lon1);
        log.info("Delivery coords: lat={}, lon={}", lat2, lon2);

        if (lat1 == lat2 && lon1 == lon2) {
            return BigDecimal.ZERO;
        }

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distanceKm = EARTH_RADIUS_KM * c;

        log.info("Calculated raw distance: {} km", distanceKm);
        return BigDecimal.valueOf(distanceKm);
    }
}
