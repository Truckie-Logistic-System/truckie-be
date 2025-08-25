package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.ContractRuleRequest;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.order.contract.ContractRuleResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.contract.ContractRuleEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.pricing.VehicleRuleEntity;
import capstone_project.service.entityServices.order.contract.ContractEntityService;
import capstone_project.service.entityServices.order.contract.ContractRuleEntityService;
import capstone_project.service.entityServices.order.order.OrderDetailEntityService;
import capstone_project.service.entityServices.pricing.VehicleRuleEntityService;
import capstone_project.service.mapper.order.ContractRuleMapper;
import capstone_project.service.services.order.order.ContractRuleService;
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

        ContractRuleEntity contractRule = contractRuleEntityService.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        return contractRuleMapper.toContractRuleResponse(contractRule);
    }

    @Override
    public ContractRuleResponse createContract(ContractRuleRequest contractRuleRequest) {
        log.info("Creating new contract rule with request: {}", contractRuleRequest);

        if (contractRuleRequest.vehicleRuleId() == null || contractRuleRequest.contractEntityId().isEmpty()) {
            log.error("Vehicle rule ID & Contract ID are required");
            throw new NotFoundException(
                    "Vehicle rule ID and Contract ID are required",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }


        if (contractRuleRequest.numOfVehicles() == null || contractRuleRequest.numOfVehicles() <= 0) {
            log.error("Number of vehicles must be greater than zero");
            throw new NotFoundException(
                    "Number of vehicles must be greater than zero",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        UUID contractEntityUUId = UUID.fromString(contractRuleRequest.contractEntityId());
        UUID vehicleRuleUUId = UUID.fromString(contractRuleRequest.vehicleRuleId());

        if (contractRuleEntityService.findContractRuleEntitiesByContractEntityIdAndVehicleRuleEntityId(contractEntityUUId, vehicleRuleUUId).isPresent()) {
            log.error("Contract rule with vehicle rule ID {} and contract ID {} already exists", vehicleRuleUUId, contractEntityUUId);
            throw new NotFoundException(
                    "Contract rule with this vehicle rule ID and contract ID already exists",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
            );
        }

        ContractRuleEntity contractRuleEntity = contractRuleMapper.mapRequestToEntity(contractRuleRequest);

        contractRuleEntity.setStatus(CommonStatusEnum.ACTIVE.name());

        ContractRuleEntity savedContractRule = contractRuleEntityService.save(contractRuleEntity);

        return contractRuleMapper.toContractRuleResponse(savedContractRule);
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
        ContractEntity contractEntity = contractEntityService.findById(contractEntityId)
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
        log.info("Found {} orderDetails for contract {}", orderDetails.size(), contractEntityId);

        List<ContractRuleEntity> existingRules =
                contractRuleEntityService.findContractRuleEntitiesByContractEntityId(contractEntityId);
        log.info("Found {} existing contract rules", existingRules.size());

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

            VehicleRuleEntity vehicleRule = vehicleRuleEntityService.findById(vehicleRuleId)
                    .orElseThrow(() -> {
                        log.error("Vehicle rule not found with ID {}", request.vehicleRuleId());
                        return new NotFoundException("Vehicle rule not found: " + request.vehicleRuleId(),
                                ErrorEnum.NOT_FOUND.getErrorCode());
                    });

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

            BigDecimal currentLoad = BigDecimal.ZERO;
            List<UUID> assignedDetails = new ArrayList<>();

            ContractRuleEntity contractRule = contractRuleMapper.mapRequestToEntity(request);
            contractRule.setContractEntity(contractEntity);
            contractRule.setVehicleRuleEntity(vehicleRule);
            contractRule.setStatus(CommonStatusEnum.ACTIVE.name());

            for (OrderDetailEntity detail : unassignedDetails) {
                if (newlyAssigned.contains(detail.getId())) {
                    continue;
                }

                boolean canFit = detail.getWeight().compareTo(vehicleRule.getMaxWeight()) <= 0
                        && detail.getLength().compareTo(vehicleRule.getMaxLength()) <= 0
                        && detail.getWidth().compareTo(vehicleRule.getMaxWidth()) <= 0
                        && detail.getHeight().compareTo(vehicleRule.getMaxHeight()) <= 0;

                if (canFit) {
                    currentLoad = currentLoad.add(detail.getWeight());
                    assignedDetails.add(detail.getId());
                    newlyAssigned.add(detail.getId());

                    detail.setContractRuleEntity(contractRule);
                    contractRule.getOrderDetails().add(detail);
                }
            }

            if (assignedDetails.isEmpty()) {
                log.warn("No order details fit into vehicle {}", vehicleRule.getVehicleRuleName());
            }

            contractRuleEntityService.save(contractRule);
            log.info("Saved contractRule {} with {} assigned details", contractRule.getId(), assignedDetails.size());

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

        return ListContractRuleAssignResult.builder()
                .vehicleAssignments(responses)
                .unassignedDetails(stillUnassigned)
                .build();
    }



    @Override
    public ContractRuleResponse updateContract(UUID id, ContractRuleRequest contractRuleRequest) {
        log.info("Updating contract rule with ID: {}", id);

        ContractRuleEntity existingContractRule = contractRuleEntityService.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        UUID contractEntityUUId = UUID.fromString(contractRuleRequest.contractEntityId());
        UUID vehicleRuleUUId = UUID.fromString(contractRuleRequest.vehicleRuleId());

        Optional<ContractRuleEntity> existing = contractRuleEntityService
                .findContractRuleEntitiesByContractEntityIdAndVehicleRuleEntityId(contractEntityUUId, vehicleRuleUUId);

        if (existing.isPresent() && !existing.get().getId().equals(id)) {
            log.error("Contract rule with vehicle rule ID {} and contract ID {} already exists", vehicleRuleUUId, contractEntityUUId);
            throw new NotFoundException(
                    "Contract rule with this vehicle rule ID and contract ID already exists",
                    ErrorEnum.ALREADY_EXISTED.getErrorCode()
            );
        }

        contractRuleMapper.toContractRuleEntity(contractRuleRequest, existingContractRule);

        ContractRuleEntity savedContractRule = contractRuleEntityService.save(existingContractRule);

        return contractRuleMapper.toContractRuleResponse(savedContractRule);
    }

    @Override
    public void deleteContract(UUID id) {

    }
}
