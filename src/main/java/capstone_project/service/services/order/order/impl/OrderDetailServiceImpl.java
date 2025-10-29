package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.CommonStatusEnum;
import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.request.order.CreateOrderDetailRequest;
import capstone_project.dtos.request.order.UpdateOrderDetailRequest;
import capstone_project.dtos.request.vehicle.CreateAndAssignForDetailsRequest;
import capstone_project.dtos.request.vehicle.VehicleAssignmentRequest;
import capstone_project.dtos.response.order.CreateOrderResponse;
import capstone_project.dtos.response.order.GetOrderDetailResponse;
import capstone_project.dtos.response.order.GetOrderDetailsResponseForList;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.vehicle.VehicleAssignmentResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.pricing.VehicleTypeRuleEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.entity.vehicle.VehicleTypeEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.order.OrderSizeEntityService;
import capstone_project.repository.entityServices.pricing.VehicleTypeRuleEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleTypeEntityService;
import capstone_project.service.mapper.order.OrderDetailMapper;
import capstone_project.service.mapper.order.OrderMapper;
import capstone_project.service.services.order.order.ContractRuleService;
import capstone_project.service.services.order.order.ContractService;
import capstone_project.service.services.order.order.OrderDetailService;
import capstone_project.service.services.order.order.OrderService;
import capstone_project.service.services.vehicle.VehicleAssignmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderDetailServiceImpl implements OrderDetailService {
    private final OrderDetailEntityService orderDetailEntityService;
    private final OrderEntityService orderEntityService;
    private final OrderSizeEntityService orderSizeEntityService;
    private final ContractEntityService contractEntityService;
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final ObjectProvider<VehicleAssignmentService> vehicleAssignmentServiceProvider;
    private final VehicleTypeRuleEntityService vehicleTypeRuleEntityService;
    private final ContractRuleService contractRuleService;
    private final VehicleTypeEntityService vehicleTypeEntityService;
    private final OrderService orderService;
    private final ContractService contractService;
    private final OrderDetailMapper orderDetailMapper;
    private final OrderMapper orderMapper;


    @Override
    @Transactional
    public GetOrderDetailResponse changeStatusOrderDetailExceptTroubles(UUID orderDetailId, OrderStatusEnum orderDetailStatus) {
        log.info("Change status order detail except troubles");
        if(orderDetailStatus.name().equals(OrderStatusEnum.IN_TROUBLES.name())){
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Cannot change status to troubles by this method for a order detail ID: " + orderDetailId,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }


        OrderDetailEntity orderDetailEntity = orderDetailEntityService.findEntityById(orderDetailId)
                .orElseThrow(() -> new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage() + " orderDetailEntity with ID: " + orderDetailId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        OrderStatusEnum currentStatus;
        try {
            currentStatus = OrderStatusEnum.valueOf(orderDetailEntity.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Invalid current status: " + orderDetailEntity.getStatus(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }
        // Check transition hợp lệ using order detail specific validation
        if (currentStatus == null || !isValidTransitionForOrderDetail(currentStatus, orderDetailStatus)) {
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Cannot change from " + orderDetailEntity.getStatus() + " to " + orderDetailStatus,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        // Update OrderDetail
        orderDetailEntity.setStatus(orderDetailStatus.name());
        orderDetailEntityService.save(orderDetailEntity);

        //Check change status for order
        OrderEntity orderEntity = orderDetailEntity.getOrderEntity();
        List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderEntity.getId());
        boolean allSameStatus = details.stream()
                .allMatch(d -> d.getStatus().equals(orderDetailStatus.name()));

        if (allSameStatus) {
            // Update status của Order and send WebSocket notification
            orderService.updateOrderStatus(orderEntity.getId(), orderDetailStatus);
            log.info("Updated order {} status to {} after all order details reached same status", orderEntity.getOrderCode(), orderDetailStatus);
        }


        return orderDetailMapper.toGetOrderDetailResponse(orderDetailEntity);
    }

    @Override
    public GetOrderDetailResponse changeStatusOrderDetailForTroublesByDriver(UUID orderDetailId) {
        log.info("change status order detail trouble for driver");
        OrderDetailEntity orderDetailEntity = orderDetailEntityService.findEntityById(orderDetailId)
                .orElseThrow(() -> new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage() + " orderDetailEntity with ID: " + orderDetailId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Update OrderDetail
        orderDetailEntity.setStatus(OrderStatusEnum.IN_TROUBLES.name());
        orderDetailEntityService.save(orderDetailEntity);


        OrderEntity orderEntity = orderDetailEntity.getOrderEntity();
        // Update status của Order and send WebSocket notification
        orderService.updateOrderStatus(orderEntity.getId(), OrderStatusEnum.IN_TROUBLES);
        log.info("Updated order {} status to IN_TROUBLES after driver reported issue for order detail {}", orderEntity.getOrderCode(), orderDetailId);


        return orderDetailMapper.toGetOrderDetailResponse(orderDetailEntity);
    }

    @Override
    public CreateOrderResponse createOrderDetailByOrderId(UUID orderId, List<CreateOrderDetailRequest> createOrderDetailRequest) {
        log.info("Create order detail by order ID: " + orderId);
        OrderEntity orderEntity = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage() + " orderEntity with ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        if(createOrderDetailRequest == null || createOrderDetailRequest.isEmpty()){
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Account customer's status is inactive so cannot create order  ",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        if(orderEntity.getSender().getStatus().equals(CommonStatusEnum.INACTIVE.name())){
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        Set<String> allowedStatuses = Set.of(
                OrderStatusEnum.PENDING.name(),
                OrderStatusEnum.CONTRACT_DRAFT.name(),
                OrderStatusEnum.PROCESSING.name()
        );

        if (!allowedStatuses.contains(orderEntity.getStatus())) {
            throw new BadRequestException(ErrorEnum.INVALID.getMessage() + " Cannot add more order detail for orderId: " + orderId +", because order only can add orderDetail when order's status is PENDING, PROCESSING or CONTRACT_DRAFT",
                    ErrorEnum.INVALID.getErrorCode());
        }

        List<OrderDetailEntity> listOrderDetail = orderService.batchCreateOrderDetails(createOrderDetailRequest,orderEntity,orderEntity.getOrderDetailEntities().get(0).getEstimatedStartTime());
        if(listOrderDetail.isEmpty()){
            log.error("listOrderDetail empty");
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(), ErrorEnum.NOT_FOUND.getErrorCode());
        }

        orderEntity.setTotalQuantity(orderEntity.getTotalQuantity() + listOrderDetail.size());

//        orderEntity.setTotalWeight(orderEntity.getTotalWeight().add(totalWeight));


        orderEntityService.save(orderEntity);
        orderEntity.setOrderDetailEntities(listOrderDetail);

        return orderMapper.toCreateOrderResponse(orderEntity);

    }

    @Override
    public List<GetOrderDetailsResponseForList> getOrderDetailByOrderIdResponseList(UUID orderId) {
        log.info("Fetching order details for order ID: {}", orderId);

        if(orderEntityService.findEntityById(orderId).isPresent()){
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + " No order found for order ID: " + orderId,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        List<OrderDetailEntity> details = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);

        if (details.isEmpty()) {
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + " No order details found for order ID: " + orderId,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return orderDetailMapper.toGetOrderDetailResponseListBasic(details);
    }

    @Override
    public GetOrderDetailResponse getOrderDetailById(UUID orderDetailId) {
        log.info("Fetching order detail by ID: {}", orderDetailId);

        OrderDetailEntity orderDetailEntity = orderDetailEntityService.findEntityById(orderDetailId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " OrderDetailEntity with ID: " + orderDetailId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        return orderDetailMapper.toGetOrderDetailResponse(orderDetailEntity);
    }

    @Override
    @Transactional
    public GetOrderDetailResponse updateOrderDetailBasicInPendingOrProcessing(UpdateOrderDetailRequest updateOrderDetailRequest) {
        log.info("Updating order detail with ID: {}", updateOrderDetailRequest.orderDetailId());




        OrderDetailEntity orderDetailEntity = orderDetailEntityService.findEntityById(UUID.fromString(updateOrderDetailRequest.orderDetailId()))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " OrderDetailEntity with ID: " + updateOrderDetailRequest.orderDetailId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        if(!(orderDetailEntity.getStatus().equals(OrderStatusEnum.PENDING.name()) || orderDetailEntity.getStatus().equals(OrderStatusEnum.PROCESSING.name()))){
            throw new NotFoundException(
                    ErrorEnum.INVALID_REQUEST.getMessage() + " Cannot update order detail with ID: " + orderDetailEntity.getId() +", because order detail status is not PENDING or PROCESSING",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        OrderEntity orderEntity = orderDetailEntity.getOrderEntity();



        OrderSizeEntity orderSizeEntity = orderSizeEntityService.findEntityById(UUID.fromString(updateOrderDetailRequest.orderSizeId()))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " OrderSizeEntity with ID: " + updateOrderDetailRequest.orderSizeId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));


//        if(updateOrderDetailRequest.weight().compareTo(orderDetailEntity.getWeight()) != 0){
//            BigDecimal totalWeight = orderEntity.getTotalWeight()
//                    .subtract(orderDetailEntity.getWeight())
//                    .add(updateOrderDetailRequest.weight());
//
//            orderEntity.setTotalWeight(totalWeight);
//        }
        orderDetailEntity.setWeight(updateOrderDetailRequest.weight());
        orderDetailEntity.setDescription(updateOrderDetailRequest.description());
        orderDetailEntity.setOrderSizeEntity(orderSizeEntity);

        orderDetailEntityService.save(orderDetailEntity);
        orderEntityService.save(orderEntity);


        return orderDetailMapper.toGetOrderDetailResponse(orderDetailEntity);
    }

    @Override
    public boolean changeStatusOrderDetailOnlyForAdmin(UUID orderId, UUID orderDetailId, OrderStatusEnum status) {
        return false;
    }

//    @Override
//    @Transactional
//    public List<GetOrderDetailResponse> updateVehicleAssigmentForEachOrderDetails(UUID orderId) {
//        log.info("Updating vehicle assigment for order ID: {}", orderId);
//
//        //Lấy list assign phan bo tung detail cho moi vehicle rule
//        List<ContractRuleAssignResponse> assignResponses = contractService.assignVehicles(orderId);
//        Map<UUID,List<UUID>> mapVehicleTypeAndOrderDetail = new HashMap<>();
//        for(ContractRuleAssignResponse contractRuleAssignResponse : assignResponses){
//            VehicleTypeEntity vehicleTypeEntity = vehicleTypeEntityService.findContractRuleEntitiesById(vehicleTypeRuleEntityService.findContractRuleEntitiesById(contractRuleAssignResponse.getVehicleRuleId()).get().getId()).get();
//            mapVehicleTypeAndOrderDetail.put(
//                    vehicleTypeEntity.getId(),
//                    contractRuleAssignResponse.getAssignedDetails()
//            );
//        }
//
//        List<VehicleAssignmentEntity> listVehicleAssignments = vehicleAssignmentEntityService.findByStatus(CommonStatusEnum.ACTIVE.name());
//        if(listVehicleAssignments.isEmpty()){
//            throw new NotFoundException(
//                    ErrorEnum.INVALID_REQUEST.getMessage() + " listVehicleAssignments doesn't have any Active status now" ,
//                    ErrorEnum.INVALID_REQUEST.getErrorCode()
//            );
//        }
//
//
//
//        for (VehicleAssignmentEntity vehicleAssignmentEntity : listVehicleAssignments) {
//            UUID vehicleTypeId = vehicleAssignmentEntity.getVehicleEntity().getVehicleTypeEntity().getId();
//            List<OrderDetailEntity> toUpdate = new ArrayList<>();
//            if (mapVehicleTypeAndOrderDetail.containsKey(vehicleTypeId)) {
//                for (UUID orderDetailId : mapVehicleTypeAndOrderDetail.get(vehicleTypeId)) {
//                    OrderDetailEntity detail = orderDetailEntityService.findContractRuleEntitiesById(orderDetailId)
//                            .orElseThrow(() -> new NotFoundException(
//                                    ErrorEnum.NOT_FOUND.getMessage() + " order detail with ID: " + orderDetailId,
//                                    ErrorEnum.NOT_FOUND.getErrorCode()
//                            ));
//                    detail.setVehicleAssignmentEntity(vehicleAssignmentEntity);
//                    detail.setStatus(OrderStatusEnum.ASSIGNED_TO_DRIVER.name());
//                    toUpdate.add(detail);
//                }
//                orderDetailEntityService.saveAllOrderDetailEntities(toUpdate);
//                vehicleAssignmentEntity.setStatus(VehicleAssignmentStatusEnum.ASSIGNED.name());
//            }
//        }
//
//
//
//        return orderDetailMapper.toGetOrderDetailResponseList(orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId));
//    }


    @Override
    @Transactional
    public List<GetOrderDetailsResponseForList> updateVehicleAssigmentForEachOrderDetails(UUID orderId) {
        log.info("Updating vehicle assignment for order ID: {}", orderId);

        //Lấy list assign phan bo tung detail cho moi vehicle rule
        List<ContractRuleAssignResponse> assignResponses = contractService.assignVehiclesWithAvailability(orderId);
        Map<UUID, List<UUID>> mapVehicleTypeAndOrderDetail = new HashMap<>();
        for(ContractRuleAssignResponse contractRuleAssignResponse : assignResponses){
            VehicleTypeEntity vehicleTypeEntity = vehicleTypeEntityService.findEntityById(vehicleTypeRuleEntityService.findEntityById(contractRuleAssignResponse.getVehicleTypeRuleId()).get().getId()).get();
            mapVehicleTypeAndOrderDetail.put(
                    vehicleTypeEntity.getId(),
                    contractRuleAssignResponse.getAssignedDetails()
                            .stream()
                            .map(detail -> UUID.fromString(detail.id()))
                            .toList()
            );
        }

        // 1. Lấy contractEntity từ orderId
        ContractEntity contractEntity = contractEntityService.getContractByOrderId(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Contract not found for orderId=" + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // 2. Gọi service getListAssignOrUnAssignContractRule
        ListContractRuleAssignResult assignResult =
                contractRuleService.getListAssignOrUnAssignContractRule(contractEntity.getId());

        // 3. Map VehicleTypeId -> List<OrderDetailId>
        for (ContractRuleAssignResponse response : assignResult.vehicleAssignments()) {
            UUID vehicleRuleId = response.getVehicleTypeRuleId();
            VehicleTypeRuleEntity vehicleRule = vehicleTypeRuleEntityService.findEntityById(vehicleRuleId)
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle rule not found: " + vehicleRuleId,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
            UUID vehicleTypeId = vehicleRule.getVehicleTypeEntity().getId();
            mapVehicleTypeAndOrderDetail.put(
                    vehicleTypeId,
                    response.getAssignedDetails()
                            .stream()
                            .map(detail -> UUID.fromString(detail.id()))
                            .toList()
            );        }

        // 5. Assign
        for(UUID vehicleTypeId : mapVehicleTypeAndOrderDetail.keySet()){
            VehicleAssignmentService vehicleAssignmentService = vehicleAssignmentServiceProvider.getIfAvailable();
            if (vehicleAssignmentService == null) {
                throw new IllegalStateException("VehicleAssignmentService not available");
            }
            List<VehicleAssignmentResponse> listAssignmentByVehicleType = vehicleAssignmentService.getAllAssignmentsWithOrder(vehicleTypeId);
            List<OrderDetailEntity> toUpdate = new ArrayList<>();
            VehicleAssignmentEntity vehicleAssignmentEntity = vehicleAssignmentEntityService.findEntityById(listAssignmentByVehicleType.get(0).id()).get();
            for (UUID orderDetailId : mapVehicleTypeAndOrderDetail.get(vehicleTypeId)) {
                OrderDetailEntity detail = orderDetailEntityService.findEntityById(orderDetailId)
                        .orElseThrow(() -> new NotFoundException(
                                "Order detail not found: " + orderDetailId,
                                ErrorEnum.NOT_FOUND.getErrorCode()
                        ));
                detail.setVehicleAssignmentEntity(vehicleAssignmentEntity);
                changeStatusOrderDetailExceptTroubles(detail.getId(),OrderStatusEnum.ASSIGNED_TO_DRIVER);
                toUpdate.add(detail);
            }
            orderDetailEntityService.saveAllOrderDetailEntities(toUpdate);
            vehicleAssignmentEntity.setStatus(CommonStatusEnum.ACTIVE.name());
            vehicleAssignmentEntityService.save(vehicleAssignmentEntity);
        }



        // 6. Return kết quả cuối cùng
        return orderDetailMapper.toGetOrderDetailResponseListBasic(
                orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId)
        );
    }

    @Override
    @Transactional
    public List<GetOrderDetailsResponseForList> createAndAssignVehicleAssignmentForDetails(CreateAndAssignForDetailsRequest request) {
        VehicleAssignmentService vehicleAssignmentService = vehicleAssignmentServiceProvider.getIfAvailable();
        if (vehicleAssignmentService == null) {
            throw new IllegalStateException("VehicleAssignmentService not available");
        }
        List<GetOrderDetailsResponseForList> resultResponse = new ArrayList<>();

        // Iterate through tracking codes and their assignments
        for(Map.Entry<String, VehicleAssignmentRequest> entry : request.assignments().entrySet()) {
            String trackingCode = entry.getKey();
            VehicleAssignmentRequest assignmentRequest = entry.getValue();

            // Create the vehicle assignment
            VehicleAssignmentResponse response = vehicleAssignmentService.createAssignment(assignmentRequest);
            VehicleAssignmentEntity vehicleAssignmentEntity = vehicleAssignmentEntityService.findEntityById(response.id())
                    .orElseThrow(() -> new NotFoundException(
                            "Vehicle assignment not found: " + response.id(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            // Find order detail using tracking code and update it
            OrderDetailEntity detail = orderDetailEntityService.findByTrackingCode(trackingCode)
                    .orElseThrow(() -> new NotFoundException(
                            "Order detail not found with tracking code: " + trackingCode,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            // Update and save the order detail
            detail.setVehicleAssignmentEntity(vehicleAssignmentEntity);
            changeStatusOrderDetailExceptTroubles(detail.getId(), OrderStatusEnum.ASSIGNED_TO_DRIVER);
            orderDetailEntityService.save(detail);

            // Add to result
            resultResponse.add(orderDetailMapper.toGetOrderDetailsResponseForList(detail));
        }

        return resultResponse;
    }


    @Override
    public List<GetOrderDetailsResponseForList> getAllOrderDetails() {
        return orderDetailMapper.toGetOrderDetailResponseListBasic(orderDetailEntityService.findAll());
    }

    /**
     * Validates if a transition is valid for an order detail
     * Order details represent individual cargo/shipment lots with a simplified flow
     */
    @Override
    public boolean isValidTransitionForOrderDetail(OrderStatusEnum current, OrderStatusEnum next) {
        switch(current) {
            case PENDING:
                return next == OrderStatusEnum.PROCESSING || next == OrderStatusEnum.ASSIGNED_TO_DRIVER;
            case PROCESSING:
                return next == OrderStatusEnum.ASSIGNED_TO_DRIVER;
            case ASSIGNED_TO_DRIVER:
                return next == OrderStatusEnum.PICKED_UP;
            case PICKED_UP:
                return next == OrderStatusEnum.SEALED_COMPLETED;
            case SEALED_COMPLETED:
                return next == OrderStatusEnum.ONGOING_DELIVERED;
            case ONGOING_DELIVERED:
                return next == OrderStatusEnum.DELIVERED;
            case DELIVERED:
                return next == OrderStatusEnum.SUCCESSFUL || next == OrderStatusEnum.RETURNING;
            case RETURNING:
                return next == OrderStatusEnum.RETURNED;
            case RETURNED:
                return next == OrderStatusEnum.SUCCESSFUL;
            // Terminal states
            case CANCELLED:
            case SUCCESSFUL:
                return false;
            default:
                // For any status that can be directly cancelled
                if (next == OrderStatusEnum.CANCELLED) {
                    return true;
                }
                return false;
        }
    }

    @Override
    public boolean updateOrderDetailStatus(UUID orderDetailId, OrderStatusEnum newStatus) {
        Optional<OrderDetailEntity> optionalDetail = orderDetailEntityService.findEntityById(orderDetailId);
        if (optionalDetail.isEmpty()) {
            return false;
        }
        OrderDetailEntity detail = optionalDetail.get();
        OrderStatusEnum currentStatus;
        try {
            currentStatus = OrderStatusEnum.valueOf(detail.getStatus());
        } catch (IllegalArgumentException e) {
            return false;
        }
        if (!isValidTransitionForOrderDetail(currentStatus, newStatus)) {
            return false;
        }
        detail.setStatus(newStatus.name());
        orderDetailEntityService.save(detail);
        return true;
    }
}
