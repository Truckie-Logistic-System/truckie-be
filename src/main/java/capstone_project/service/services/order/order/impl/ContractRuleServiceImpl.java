package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.ContractRuleRequest;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.order.contract.ContractRuleResponse;
import capstone_project.dtos.response.order.contract.PriceCalculationResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.contract.ContractRuleEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.pricing.VehicleRuleEntityService;
import capstone_project.service.mapper.order.ContractRuleMapper;
import capstone_project.service.services.order.order.ContractRuleService;
import capstone_project.service.services.order.order.ContractService;
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
    private final VehicleRuleEntityService vehicleRuleEntityService;
    private final ContractService contractService;
    private final OrderEntityService orderEntityService;

    @Override
    public List<ContractRuleResponse> getContracts() {
        log.info("Fetching all contract rules");
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
        log.info("Fetching contract rule with ID: {}", id);

        ContractRuleEntity contractRule = contractRuleEntityService.findEntityById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        return contractRuleMapper.toContractRuleResponse(contractRule);
    }

    @Override
    public ListContractRuleAssignResult getListAssignOrUnAssignContractRule(UUID contractId) {
        log.info("Fetching contract rule assignments for contract ID: {}", contractId);

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
            BigDecimal currentLoad = rule.getOrderDetails().stream()
                    .map(OrderDetailEntity::getOrderSizeEntity)
                    .filter(Objects::nonNull)
                    .map(OrderSizeEntity::getMaxWeight)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            List<UUID> detailIds = rule.getOrderDetails().stream()
                    .map(OrderDetailEntity::getId)
                    .toList();

            responses.add(
                    ContractRuleAssignResponse.builder()
                            .vehicleIndex(vehicleIndex++)
                            .vehicleRuleId(rule.getVehicleRuleEntity().getId())
                            .vehicleRuleName(rule.getVehicleRuleEntity().getVehicleRuleName())
                            .currentLoad(currentLoad)
                            .assignedDetails(detailIds)
                            .build()
            );
        }

        return ListContractRuleAssignResult.builder()
                .vehicleAssignments(responses)
                .unassignedDetails(unassignedDetails)
                .build();

    }

    @Override
    @Transactional
    public ListContractRuleAssignResult createListContractRules(List<ContractRuleRequest> contractRuleRequests) {
        log.info("Start createListContractRules with {} requests",
                contractRuleRequests == null ? 0 : contractRuleRequests.size());

        if (contractRuleRequests == null || contractRuleRequests.isEmpty()) {
            log.error("Contract rule requests is null or empty");
            throw new BadRequestException("Contract rule requests must not be null or empty",
                    ErrorEnum.INVALID.getErrorCode());
        }

        UUID contractEntityId = UUID.fromString(contractRuleRequests.get(0).contractEntityId());
        ContractEntity contractEntity = contractEntityService.findEntityById(contractEntityId)
                .orElseThrow(() -> {
                    log.error("Contract not found with ID {}", contractEntityId);
                    return new NotFoundException("Contract not found with ID: " + contractEntityId,
                            ErrorEnum.NOT_FOUND.getErrorCode());
                });

        List<OrderDetailEntity> orderDetails =
                orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(contractEntity.getOrderEntity().getId());

        if (orderDetails.isEmpty()) {
            log.error("No order details found for contract {}", contractEntityId);
            throw new NotFoundException("No order details found for this contract",
                    ErrorEnum.NOT_FOUND.getErrorCode());
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
            log.error("All orderDetails already assigned for contract {}", contractEntityId);
            throw new BadRequestException("All order details have already been assigned for this contract",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode());
        }

        List<ContractRuleAssignResponse> responses = new ArrayList<>();
        int vehicleIndex = existingRules.size(); // tiếp tục đánh index
        Set<UUID> newlyAssigned = new HashSet<>();

        for (ContractRuleRequest request : contractRuleRequests) {
            UUID vehicleRuleId = UUID.fromString(request.vehicleRuleId());

            VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findEntityById(vehicleRuleId)
                    .orElseThrow(() -> {
                        log.error("Vehicle rule not found with ID {}", request.vehicleRuleId());
                        return new NotFoundException("Vehicle rule not found: " + request.vehicleRuleId(),
                                ErrorEnum.NOT_FOUND.getErrorCode());
                    });

            if (vehicleRule.getStatus() == null || !vehicleRule.getStatus().equals(CommonStatusEnum.ACTIVE.name())) {
                log.error("Vehicle rule {} is not active", vehicleRule.getVehicleRuleName());
                throw new BadRequestException(
                        String.format("Vehicle rule %s is not active", vehicleRule.getVehicleRuleName()),
                        ErrorEnum.INVALID.getErrorCode()
                );
            }

            boolean exists = existingRules.stream()
                    .anyMatch(r -> r.getVehicleRuleEntity().getId().equals(vehicleRuleId));
            if (exists) {
                log.error("Contract rule for vehicle {} already exists in contract {}",
                        vehicleRule.getVehicleRuleName(), contractEntityId);
                throw new BadRequestException(
                        String.format("Contract rule for vehicle %s already exists in this contract",
                                vehicleRule.getVehicleRuleName()),
                        ErrorEnum.ALREADY_EXISTED.getErrorCode()
                );
            }

            Integer numOfVehicles = request.numOfVehicles();

            for (int v = 0; v < numOfVehicles; v++) {
                BigDecimal currentLoad = BigDecimal.ZERO;
                List<UUID> assignedDetails = new ArrayList<>();

                ContractRuleEntity contractRule = new ContractRuleEntity();
                contractRule.setContractEntity(contractEntity);
                contractRule.setVehicleRuleEntity(vehicleRule);
                contractRule.setStatus(CommonStatusEnum.ACTIVE.name());

                boolean filled = false;
                for (OrderDetailEntity detail : unassignedDetails) {
                    if (newlyAssigned.contains(detail.getId())) {
                        continue;
                    }

                    OrderSizeEntity size = detail.getOrderSizeEntity();
                    if (size == null) {
                        throw new BadRequestException("All order details must have size information",
                                ErrorEnum.INVALID.getErrorCode());
                    }

                    boolean canFit = size.getMaxWeight().compareTo(vehicleRule.getMaxWeight()) <= 0
                            && size.getMaxLength().compareTo(vehicleRule.getMaxLength()) <= 0
                            && size.getMaxWidth().compareTo(vehicleRule.getMaxWidth()) <= 0
                            && size.getMaxHeight().compareTo(vehicleRule.getMaxHeight()) <= 0
                            && currentLoad.add(size.getMaxWeight()).compareTo(vehicleRule.getMaxWeight()) <= 0;

                    if (canFit) {
                        currentLoad = currentLoad.add(size.getMaxWeight());
                        assignedDetails.add(detail.getId());
                        newlyAssigned.add(detail.getId());
                        contractRule.getOrderDetails().add(detail);
                    }
                }

                contractRuleEntityService.save(contractRule);

                responses.add(
                        ContractRuleAssignResponse.builder()
                                .vehicleIndex(vehicleIndex++)
                                .vehicleRuleId(vehicleRule.getId())
                                .vehicleRuleName(vehicleRule.getVehicleRuleName())
                                .currentLoad(currentLoad)
                                .assignedDetails(assignedDetails)
                                .build()
                );
            }
        }


        Set<UUID> allAssigned = new HashSet<>(alreadyAssigned);
        allAssigned.addAll(newlyAssigned);

        List<UUID> stillUnassigned = orderDetails.stream()
                .map(OrderDetailEntity::getId)
                .filter(id -> !allAssigned.contains(id))
                .toList();

        if (!stillUnassigned.isEmpty()) {
            log.warn("Assignment incomplete: {} orderDetails still unassigned -> {}",
                    stillUnassigned.size(), stillUnassigned);
        } else {
            log.info("All orderDetails assigned successfully!");
        }

        Map<UUID, Integer> vehicleCountMap = responses.stream()
                .collect(Collectors.groupingBy(ContractRuleAssignResponse::getVehicleRuleId, Collectors.summingInt(a -> 1)));

        OrderEntity order = contractEntity.getOrderEntity();

        PriceCalculationResponse newTotal = contractService.calculateTotalPrice(contractEntity,
                contractService.calculateDistanceKm(order.getPickupAddress(), order.getDeliveryAddress()),
                vehicleCountMap);

        BigDecimal newTotalValue = newTotal.getTotalPrice();

        contractEntity.setTotalValue(newTotalValue);
        contractEntityService.save(contractEntity);

        return ListContractRuleAssignResult.builder()
                .vehicleAssignments(responses)
                .unassignedDetails(stillUnassigned)
                .build();
    }

    @Override
    @Transactional
    public ContractRuleResponse updateContractRule(UUID id, ContractRuleRequest contractRuleRequest) {
        log.info("Updating contract rule with ID: {}", id);

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

        // lấy vehicleRule mới từ request
        UUID vehicleRuleId = UUID.fromString(contractRuleRequest.vehicleRuleId());
        VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findEntityById(vehicleRuleId)
                .orElseThrow(() -> new NotFoundException("Vehicle rule not found: " + vehicleRuleId,
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        // check duplicate contractRule
        Optional<ContractRuleEntity> existing = contractRuleEntityService
                .findContractRuleEntitiesByContractEntityIdAndVehicleRuleEntityId(contractEntity.getId(), vehicleRule.getId());

        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            log.error("Contract rule with vehicle rule {} already exists in contract {}", vehicleRule.getVehicleRuleName(), contractEntity.getId());
            throw new BadRequestException(
                    String.format("Contract rule for vehicle %s already exists in this contract", vehicleRule.getVehicleRuleName()),
                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
            );
        }

        // lấy order details của contract
        List<OrderDetailEntity> orderDetails = orderDetailEntityService
                .findOrderDetailEntitiesByOrderEntityId(contractEntity.getOrderEntity().getId());

        if (orderDetails.isEmpty()) {
            throw new NotFoundException("No order details found for this contract",
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        // bắt đầu tính lại assignment cho contractRule
        BigDecimal currentLoad = BigDecimal.ZERO;
        Set<OrderDetailEntity> assignedDetails = new HashSet<>();

        for (OrderDetailEntity detail : orderDetails) {
            OrderSizeEntity size = detail.getOrderSizeEntity();
            if (size == null) {
                log.warn("OrderDetail {} has no size info, skipping", detail.getId());
                continue; // hoặc throw error tùy yêu cầu
            }

            boolean canFit = size.getMaxWeight().compareTo(vehicleRule.getMaxWeight()) <= 0
                    && size.getMaxLength().compareTo(vehicleRule.getMaxLength()) <= 0
                    && size.getMaxWidth().compareTo(vehicleRule.getMaxWidth()) <= 0
                    && size.getMaxHeight().compareTo(vehicleRule.getMaxHeight()) <= 0
                    && currentLoad.add(size.getMaxWeight()).compareTo(vehicleRule.getMaxWeight()) <= 0;

            if (canFit) {
                currentLoad = currentLoad.add(size.getMaxWeight());
                assignedDetails.add(detail);
            }
        }

        if (assignedDetails.isEmpty()) {
            log.warn("No order details fit into vehicle {}", vehicleRule.getVehicleRuleName());
            throw new BadRequestException(
                    "No order details can be assigned to the selected vehicle",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // update contractRule từ request (map primitive fields)
        contractRuleMapper.toContractRuleEntity(contractRuleRequest, existingContractRule);

        // set lại quan hệ
        existingContractRule.setVehicleRuleEntity(vehicleRule);
        existingContractRule.getOrderDetails().clear();
        existingContractRule.getOrderDetails().addAll(assignedDetails);

        ContractRuleEntity saved = contractRuleEntityService.save(existingContractRule);

        // update lại tổng giá trị contract
        Map<UUID, Integer> vehicleCountMap = contractRuleEntityService
                .findContractRuleEntityByContractEntityId(contractEntity.getId())
                .stream()
                .collect(Collectors.groupingBy(r -> r.getVehicleRuleEntity().getId(), Collectors.summingInt(r -> 1)));

        OrderEntity order = contractEntity.getOrderEntity();
        PriceCalculationResponse newTotalResponse = contractService.calculateTotalPrice(
                contractEntity,
                contractService.calculateDistanceKm(order.getPickupAddress(), order.getDeliveryAddress()),
                vehicleCountMap
        );

        BigDecimal newTotal = newTotalResponse.getTotalPrice();

        contractEntity.setTotalValue(newTotal);
        contractEntityService.save(contractEntity);

        log.info("Updated contractRule {} with {} assigned details", saved.getId(), saved.getOrderDetails().size());

        return contractRuleMapper.toContractRuleResponse(saved);
    }

    @Override
    @Transactional
    public void deleteContractRule(UUID id) {
        log.info("Deleting contract rule with ID: {}", id);

        ContractRuleEntity contractRule = contractRuleEntityService.findEntityById(id)
                .orElseThrow(() -> {
                    log.error("Contract rule not found with ID {}", id);
                    return new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    );
                });

        // Xoá quan hệ trong bảng phụ (order_details liên kết với contract_rule này)
        contractRule.getOrderDetails().clear();
        contractRuleEntityService.save(contractRule);

        // Xoá contract_rule
        contractRuleEntityService.deleteById(id);

        log.info("Deleted contract rule with ID {}", id);
    }


    @Override
    @Transactional
    public void deleteAllContractRulesByContract(UUID contractId) {
        log.info("Deleting all contract rules with contract ID: {}", contractId);

        List<ContractRuleEntity> contractRules = contractRuleEntityService.findContractRuleEntityByContractEntityId(contractId);

        if (contractRules.isEmpty()) {
            log.warn("No contract rules found for contract ID {}", contractId);
            return;
        }

        for (ContractRuleEntity contractRule : contractRules) {
            contractRule.getOrderDetails().clear();
            contractRuleEntityService.save(contractRule);
        }

        contractRuleEntityService.deleteByContractEntityId(contractId);

        log.info("Deleted all contract rules for contract ID {}", contractId);
    }

}
