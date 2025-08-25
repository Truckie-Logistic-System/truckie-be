package capstone_project.service.services.order.order.impl;

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

        if (contractRequest.orderId() == null) {
            throw new IllegalArgumentException("Order ID must not be null");
        }

        UUID orderUuid = UUID.fromString(contractRequest.orderId());

        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        OrderEntity order = orderEntityService.findById(orderUuid)
                .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));

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

        if (contractRequest.orderId() == null) {
            log.error("Order ID is null in contract request");
            throw new IllegalArgumentException("Order ID must not be null");
        }

        UUID orderUuid = UUID.fromString(contractRequest.orderId());

        if (contractEntityService.getContractByOrderId(orderUuid).isPresent()) {
            log.error("Contract already exists for order ID: {}", orderUuid);
            throw new BadRequestException(ErrorEnum.ALREADY_EXISTED.getMessage(),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        // lấy order
        OrderEntity order = orderEntityService.findById(orderUuid)
                .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));

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
                    .orElseThrow(() -> new NotFoundException("Vehicle rule not found", ErrorEnum.NOT_FOUND.getErrorCode()));

            ContractRuleEntity contractRule = ContractRuleEntity.builder()
                    .contractEntity(savedContract)
                    .vehicleRuleEntity(vehicleRule)
                    .numOfVehicles(count.intValue())
                    .status("ACTIVE")
                    .build();

            contractRuleEntityService.save(contractRule);
        }

        // 3. Tính totalPrice dựa trên contractId + distanceKm (lúc này contract đã có contractRules)
        BigDecimal totalPrice = calculateTotalPrice(savedContract, distanceKm, vehicleCountMap);

        // 4. cập nhật lại contract với totalValue
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
        log.info("Assigning vehicles for order ID: {}", orderId);

        List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
        if (details.isEmpty()) {
            throw new NotFoundException("No order details found for this order", ErrorEnum.NOT_FOUND.getErrorCode());
        }

        OrderEntity orderEntity = orderEntityService.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found", ErrorEnum.NOT_FOUND.getErrorCode()));

        List<VehicleRuleEntity> sortedVehicleRules = vehicleRuleEntityService
                .findAllByCategoryId(orderEntity.getCategory().getId())
                .stream()
                .sorted(Comparator.comparing(VehicleRuleEntity::getMaxWeight)
                        .thenComparing(VehicleRuleEntity::getMaxLength)
                        .thenComparing(VehicleRuleEntity::getMaxWidth)
                        .thenComparing(VehicleRuleEntity::getMaxHeight))
                .toList();

        if (sortedVehicleRules.isEmpty()) {
            throw new NotFoundException("No vehicle rules found for this category", ErrorEnum.NOT_FOUND.getErrorCode());
        }

        details.sort((a, b) -> {
            int cmp = b.getWeight().compareTo(a.getWeight());
            if (cmp == 0) cmp = b.getLength().compareTo(a.getLength());
            if (cmp == 0) cmp = b.getWidth().compareTo(a.getWidth());
            if (cmp == 0) cmp = b.getHeight().compareTo(a.getHeight());
            return cmp;
        });

        Map<Integer, VehicleRuleEntity> vehicleRuleCache = new HashMap<>();
        for (int i = 0; i < sortedVehicleRules.size(); i++) {
            vehicleRuleCache.put(i, sortedVehicleRules.get(i));
        }

        List<ContractRuleAssignResponse> assignments = new ArrayList<>();

        // Gán kiện vào xe
        for (OrderDetailEntity detail : details) {
            boolean assigned = false;

            for (ContractRuleAssignResponse assignment : assignments) {
                VehicleRuleEntity currentRule = vehicleRuleCache.get(assignment.getVehicleIndex());

                // thử nhét vào xe hiện tại
                if (canFit(detail, currentRule, assignment)) {
                    assignment.setCurrentLoad(assignment.getCurrentLoad().add(detail.getWeight()));
                    assignment.getAssignedDetails().add(detail.getId());
                    log.debug("Assigned {} -> Vehicle[{}] ({}), newLoad={}",
                            detail.getId(), currentRule.getId(), currentRule.getVehicleRuleName(), assignment.getCurrentLoad());
                    assigned = true;
                    break;
                }

                // upgrade lên xe lớn hơn nếu cần
                int upgradedIdx = tryUpgrade(detail, assignment, sortedVehicleRules, vehicleRuleCache);
                if (upgradedIdx >= 0) {
                    VehicleRuleEntity upgradedRule = vehicleRuleCache.get(upgradedIdx);
                    assignment.setVehicleIndex(upgradedIdx);
                    assignment.setCurrentLoad(assignment.getCurrentLoad().add(detail.getWeight()));
                    assignment.getAssignedDetails().add(detail.getId());
                    log.debug("🔄 Upgraded vehicle to [{}] ({}) for package {}, newLoad={}",
                            upgradedRule.getId(), upgradedRule.getVehicleRuleName(), detail.getId(), assignment.getCurrentLoad());
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
                        log.debug("Opened new Vehicle[{}] ({}) for package {}, firstLoad={}",
                                rule.getId(), rule.getVehicleRuleName(), detail.getId(), detail.getWeight());
                        assigned = true;
                        break;
                    }
                }
            }

            if (!assigned) {
                log.error("No vehicle can carry package {}", detail.getId());
                throw new RuntimeException("Không có loại xe nào chở được kiện " + detail.getId());
            }
        }

        log.info("Vehicle assignment completed: {} vehicles used", assignments.size());
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
        for (int i = currentIdx + 1; i < sortedVehicleRules.size(); i++) {
            VehicleRuleEntity biggerRule = vehicleRuleCache.get(i);
            if (canFitAll(assignment.getAssignedDetails(), biggerRule, detail)) {
                return i;
            }
        }
        return -1;
    }

    public BigDecimal calculateTotalPrice(ContractEntity contract,
                                          BigDecimal distanceKm,
                                          Map<UUID, Integer> vehicleCountMap) {
        log.info("🔹 Calculating total price for contract ID: {}", contract.getId());

        List<DistanceRuleEntity> distanceRules = distanceRuleEntityService.findAll()
                .stream()
                .sorted(Comparator.comparing(DistanceRuleEntity::getFromKm))
                .toList();

        if (distanceRules.isEmpty()) {
            throw new RuntimeException("No distance rules found");
        }

        BigDecimal total = BigDecimal.ZERO;

        for (Map.Entry<UUID, Integer> entry : vehicleCountMap.entrySet()) {
            UUID vehicleRuleId = entry.getKey();
            int numOfVehicles = entry.getValue();

            VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findById(vehicleRuleId)
                    .orElseThrow(() -> new NotFoundException("Vehicle rule not found: " + vehicleRuleId,
                            ErrorEnum.NOT_FOUND.getErrorCode()));

            log.info("VehicleRule[{}] allocating {} vehicles", vehicleRule.getId(), numOfVehicles);

            BigDecimal ruleTotal = BigDecimal.ZERO;
            BigDecimal remaining = distanceKm;

            for (DistanceRuleEntity distanceRule : distanceRules) {
                if (remaining.compareTo(BigDecimal.ZERO) <= 0) break;

                BigDecimal from = distanceRule.getFromKm();
                BigDecimal to = distanceRule.getToKm();

                BasingPriceEntity basePriceEntity = basingPriceEntityService
                        .findBasingPriceEntityByVehicleRuleEntityIdAndDistanceRuleEntityId(
                                vehicleRule.getId(), distanceRule.getId())
                        .orElseThrow(() -> new RuntimeException("No base price found for tier "
                                + from + "-" + to + " and vehicleRule=" + vehicleRule.getId()));

                log.debug("DistanceRule {}-{}, BasePrice={}",
                        from, to, basePriceEntity.getBasePrice());

                if (from.compareTo(BigDecimal.ZERO) == 0 && to.compareTo(BigDecimal.valueOf(4)) == 0) {
                    // 0–4km: cố định
                    ruleTotal = ruleTotal.add(basePriceEntity.getBasePrice());
                    remaining = remaining.subtract(to);
                    log.debug("Applied base price {} for tier 0-4km, remaining={}",
                            basePriceEntity.getBasePrice(), remaining);
                } else {
                    BigDecimal tierDistance = (to == null)
                            ? remaining
                            : remaining.min(to.subtract(from));

                    ruleTotal = ruleTotal.add(basePriceEntity.getBasePrice().multiply(tierDistance));
                    remaining = remaining.subtract(tierDistance);

                    log.debug("Applied base price {} * {} km = {}, remaining={}",
                            basePriceEntity.getBasePrice(), tierDistance,
                            basePriceEntity.getBasePrice().multiply(tierDistance), remaining);
                }
            }

            if (numOfVehicles > 0) {
                ruleTotal = ruleTotal.multiply(BigDecimal.valueOf(numOfVehicles));
                log.info("VehicleRule[{}]: multiplied by {} vehicles => {}",
                        vehicleRule.getId(), numOfVehicles, ruleTotal);
            }

            total = total.add(ruleTotal);
        }

        CategoryPricingDetailEntity adjustment =
                categoryPricingDetailEntityService.findByCategoryId(contract.getOrderEntity().getCategory().getId());
        if (adjustment != null) {
            BigDecimal multiplier = adjustment.getPriceMultiplier() != null ? adjustment.getPriceMultiplier() : BigDecimal.ONE;
            BigDecimal extraFee = adjustment.getExtraFee() != null ? adjustment.getExtraFee() : BigDecimal.ZERO;
            total = total.multiply(multiplier).add(extraFee);
            log.info("Category adjustment applied: multiplier={}, extraFee={}, new total={}",
                    multiplier, extraFee, total);
        }

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Total price must not be negative");
        }

        log.info("Final total price for Contract[{}] = {}", contract.getId(), total);
        return total;
    }


    private boolean canFit(OrderDetailEntity detail, VehicleRuleEntity rule) {
        return detail.getWeight().compareTo(rule.getMaxWeight()) <= 0
                && detail.getLength().compareTo(rule.getMaxLength()) <= 0
                && detail.getWidth().compareTo(rule.getMaxWidth()) <= 0
                && detail.getHeight().compareTo(rule.getMaxHeight()) <= 0;
    }

    private boolean canFit(OrderDetailEntity detail, VehicleRuleEntity rule, ContractRuleAssignResponse assignment) {
        BigDecimal newLoad = assignment.getCurrentLoad().add(detail.getWeight());
        return newLoad.compareTo(rule.getMaxWeight()) <= 0
                && detail.getLength().compareTo(rule.getMaxLength()) <= 0
                && detail.getWidth().compareTo(rule.getMaxWidth()) <= 0
                && detail.getHeight().compareTo(rule.getMaxHeight()) <= 0;
    }

    private boolean canFitAll(List<UUID> detailIds, VehicleRuleEntity newRule, OrderDetailEntity newDetail) {
        BigDecimal totalWeight = newDetail.getWeight();

        for (UUID id : detailIds) {
            List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(id);

            for (OrderDetailEntity d : details) {
                totalWeight = totalWeight.add(d.getWeight());

                if (d.getLength().compareTo(newRule.getMaxLength()) > 0
                        || d.getWidth().compareTo(newRule.getMaxWidth()) > 0
                        || d.getHeight().compareTo(newRule.getMaxHeight()) > 0) {
                    return false;
                }
            }
        }
        return totalWeight.compareTo(newRule.getMaxWeight()) <= 0;
    }

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
