package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.*;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.InternalServerException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.common.utils.UserContextUtils;
import capstone_project.dtos.request.order.CreateOrderDetailRequest;
import capstone_project.dtos.request.order.CreateOrderRequest;
import capstone_project.dtos.request.order.UpdateOrderRequest;
import capstone_project.dtos.response.issue.GetIssueImageResponse;
import capstone_project.dtos.response.order.*;
import capstone_project.dtos.response.order.contract.ContractResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.CategoryEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.OrderSizeEntity;
import capstone_project.entity.user.address.AddressEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.repository.entityServices.order.VehicleFuelConsumptionEntityService;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.CategoryEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.order.order.OrderSizeEntityService;
import capstone_project.repository.entityServices.user.AddressEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.repository.entityServices.user.PenaltyHistoryEntityService;
import capstone_project.service.mapper.order.*;
import capstone_project.service.services.issue.IssueImageService;
import capstone_project.service.services.order.order.ContractService;
import capstone_project.service.services.order.order.OrderService;
import capstone_project.service.services.order.order.OrderStatusWebSocketService;
import capstone_project.service.services.order.order.PhotoCompletionService;
import capstone_project.service.services.order.seal.SealService;
import capstone_project.service.services.order.transaction.payOS.PayOSTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {
    private final OrderEntityService orderEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final CustomerEntityService customerEntityService;
    private final AddressEntityService addressEntityService;
    private final OrderSizeEntityService orderSizeEntityService;
    private final CategoryEntityService categoryEntityService;
    private final IssueImageService issueImageService;
    private final ContractEntityService contractEntityService;
    private final ContractService contractService;
    private final PayOSTransactionService payOSTransactionService;
    private final PhotoCompletionService photoCompletionService;
    private final OrderMapper orderMapper;
    private final OrderDetailMapper orderDetailMapper;
    private final SimpleOrderMapper simpleOrderMapper;
    private final UserContextUtils userContextUtils;
    private final StaffOrderMapper staffOrderMapper;
    private final DriverOrderMapper driverOrderMapper;
    private final PenaltyHistoryEntityService penaltyHistoryEntityService;
    private final VehicleFuelConsumptionEntityService vehicleFuelConsumptionEntityService;
    private final OrderStatusWebSocketService orderStatusWebSocketService;
    private final SealService sealService; // Thêm SealService

    @Value("${prefix.order.code}")
    private String prefixOrderCode;
    @Value("${prefix.order.detail.code}")
    private String prefixOrderDetailCode;

    @Override
    public List<OrderForCustomerListResponse> getOrdersForCurrentCustomer() {
        UUID customerId = userContextUtils.getCurrentCustomerId();
        List<OrderEntity> orderEntities = orderEntityService.findBySenderId(customerId);
        return orderMapper.toOrderForCustomerListResponses(orderEntities);
    }

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest orderRequest, List<CreateOrderDetailRequest> listCreateOrderDetailRequests) {
        log.info("[Create Order and OrderDetails] Bat Dau Chien");
        if (orderRequest == null || listCreateOrderDetailRequests == null || listCreateOrderDetailRequests.isEmpty()) {
            log.error("Order or Order detail is null");
            throw new BadRequestException(ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        UUID customerId = userContextUtils.getCurrentCustomerId();
        if (customerId == null) {
            log.error("Current customer id is null");
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage() + " sender not found",
                    ErrorEnum.NOT_FOUND.getErrorCode());
        }

        CustomerEntity sender = customerEntityService.findEntityById(customerId)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage() + " sender not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        if (sender.getStatus().equals(CommonStatusEnum.INACTIVE.name())) {
            log.error("[Create Order and OrderDetails] Bat Dau Chien ");
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Account customer's status is inactive so cannot create order  ",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        AddressEntity deliveryAddress = addressEntityService.findEntityById(UUID.fromString(orderRequest.deliveryAddressId()))
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage() + "deliveryAddress not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        AddressEntity pickupAddress = addressEntityService.findEntityById(UUID.fromString(orderRequest.pickupAddressId()))
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage() + "pickupAddress not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        CategoryEntity category = categoryEntityService.findEntityById(UUID.fromString(orderRequest.categoryId()))
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage() + "category not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()));


        try {
            //Save order
            OrderEntity newOrder = OrderEntity.builder()
                    .notes(orderRequest.notes())
                    .totalQuantity(listCreateOrderDetailRequests.size())
//                    .totalWeight(orderRequest.totalWeight())
                    .orderCode(generateCode(prefixOrderCode))
                    .receiverName(orderRequest.receiverName())
                    .receiverPhone(orderRequest.receiverPhone())
                    .receiverIdentity(orderRequest.receiverIdentity())
                    .status(OrderStatusEnum.PENDING.name())
                    .packageDescription(orderRequest.packageDescription())
                    .category(category)
                    .sender(sender)
                    .deliveryAddress(deliveryAddress)
                    .pickupAddress(pickupAddress)
                    .build();
            OrderEntity saveOrder = orderEntityService.save(newOrder);


            saveOrder.setOrderDetailEntities(batchCreateOrderDetails(listCreateOrderDetailRequests, saveOrder, orderRequest.estimateStartTime()));
            return orderMapper.toCreateOrderResponse(saveOrder);

        } catch (Exception e) {
            log.error(e.getMessage());
            throw new InternalServerException(ErrorEnum.INTERNAL_SERVER_ERROR.getMessage(), ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode());
        }

    }

    @Override
    public CreateOrderResponse changeAStatusOrder(UUID orderId, OrderStatusEnum newStatus) {
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Order with ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        // Lấy current status
        OrderStatusEnum currentStatus;
        try {
            currentStatus = OrderStatusEnum.valueOf(order.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Invalid current status: " + order.getStatus(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        if (currentStatus == null || !isValidTransition(currentStatus, newStatus)) {
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Cannot change from " + order.getStatus() + " to " + newStatus,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }

        OrderStatusEnum previousStatus = currentStatus;
        order.setStatus(newStatus.name());
        orderEntityService.save(order);

        // Send WebSocket notification for status change
        try {
            orderStatusWebSocketService.sendOrderStatusChange(
                orderId,
                order.getOrderCode(),
                previousStatus,
                newStatus
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for order status change: {}", e.getMessage());
            // Don't throw - WebSocket failure shouldn't break business logic
        }

        return orderMapper.toCreateOrderResponse(order);
    }

    @Override
    @Transactional
    public CreateOrderResponse changeStatusOrderWithAllOrderDetail(UUID orderId, OrderStatusEnum newStatus) {
        // Tìm Order
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Order with ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        // Lấy current status
        OrderStatusEnum currentStatus;
        try {
            currentStatus = OrderStatusEnum.valueOf(order.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Invalid current status: " + order.getStatus(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }
        // Check transition hợp lệ
        if (currentStatus == null || !isValidTransition(currentStatus, newStatus)) {
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Cannot change from " + order.getStatus() + " to " + newStatus,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }
        // Update Order
        OrderStatusEnum previousStatus = currentStatus;
        order.setStatus(newStatus.name());
        orderEntityService.save(order);

        // Update toàn bộ OrderDetail
        List<OrderDetailEntity> orderDetailEntities = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);

        orderDetailEntities.forEach(detail -> detail.setStatus(newStatus.name()));
        orderDetailEntityService.saveAllOrderDetailEntities(orderDetailEntities);

        // Send WebSocket notification for status change
        try {
            orderStatusWebSocketService.sendOrderStatusChange(
                orderId,
                order.getOrderCode(),
                previousStatus,
                newStatus
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for order status change: {}", e.getMessage());
            // Don't throw - WebSocket failure shouldn't break business logic
        }

        return orderMapper.toCreateOrderResponse(order);
    }


    @Override
    public boolean isValidTransition(OrderStatusEnum current, OrderStatusEnum next) {
        switch (current) {
            case PENDING:
                return next == OrderStatusEnum.PROCESSING;
            case PROCESSING:
                return next == OrderStatusEnum.CONTRACT_DRAFT;
            case CONTRACT_DRAFT:
                return next == OrderStatusEnum.CONTRACT_DENIED || next == OrderStatusEnum.CONTRACT_SIGNED;
            case CONTRACT_DENIED:
                return next == OrderStatusEnum.CANCELLED;
            case CONTRACT_SIGNED:
                return next == OrderStatusEnum.ON_PLANNING || next == OrderStatusEnum.FULLY_PAID;
            case ON_PLANNING:
                return next == OrderStatusEnum.ASSIGNED_TO_DRIVER;
            case ASSIGNED_TO_DRIVER:
                return next == OrderStatusEnum.DRIVER_CONFIRM
                        || next == OrderStatusEnum.FULLY_PAID
                        || next == OrderStatusEnum.PICKING_UP;
            case DRIVER_CONFIRM:
                return next == OrderStatusEnum.PICKED_UP
                        || next == OrderStatusEnum.PICKING_UP;
            case FULLY_PAID:
                return next == OrderStatusEnum.PICKING_UP
                        || next == OrderStatusEnum.ON_DELIVERED;
            case PICKING_UP:
                return next == OrderStatusEnum.ON_DELIVERED
                        || next == OrderStatusEnum.ONGOING_DELIVERED
                        || next == OrderStatusEnum.SEALED_COMPLETED
                        || next == OrderStatusEnum.IN_TROUBLES;
            case PICKED_UP:
                return next == OrderStatusEnum.SEALED_COMPLETED;
            case SEALED_COMPLETED:
                return next == OrderStatusEnum.ON_DELIVERED
                        || next == OrderStatusEnum.ONGOING_DELIVERED;
            case ON_DELIVERED:
                return next == OrderStatusEnum.ONGOING_DELIVERED || next == OrderStatusEnum.IN_TROUBLES;
            case ONGOING_DELIVERED:
                return next == OrderStatusEnum.DELIVERED || next == OrderStatusEnum.IN_TROUBLES;
            case IN_TROUBLES:
                return next == OrderStatusEnum.RESOLVED;
            case RESOLVED:
                return next == OrderStatusEnum.COMPENSATION;
            case DELIVERED:
                return next == OrderStatusEnum.SUCCESSFUL || next == OrderStatusEnum.REJECT_ORDER;
            case REJECT_ORDER:
                return next == OrderStatusEnum.RETURNING;
            case RETURNING:
                return next == OrderStatusEnum.RETURNED;
        }
        return false;
    }

    @Override
    public List<CreateOrderResponse> getCreateOrderRequestsBySenderId(UUID senderId) {
        log.info("getCreateOrderRequestsBySenderId: start");
        if (senderId == null) {
            log.error("Sender id is null");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + "senderId cannot be null",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        List<OrderEntity> orderEntities = orderEntityService.findBySenderId(senderId);
        return orderMapper.toCreateOrderResponses(orderEntities);
    }

    @Override
    public List<CreateOrderResponse> getCreateOrderRequestsByDeliveryAddressId(UUID deliveryAddressId) {
        log.info("getCreateOrderRequestsByDeliveryAddressId: start");
        if (deliveryAddressId == null) {
            log.error("deliveryAddressId is null");
            throw new NotFoundException(
                    ErrorEnum.NOT_FOUND.getMessage() + "deliveryAddressId cannot be null",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        List<OrderEntity> orderEntities = orderEntityService.findByDeliveryAddressId(deliveryAddressId);
        return orderMapper.toCreateOrderResponses(orderEntities);
    }

    @Override
    public boolean changeStatusOrderOnlyForAdmin(UUID orderId, OrderStatusEnum status) {
        return false;
    }

    @Override
    public CreateOrderResponse updateOrderBasicInPendingOrProcessing(UpdateOrderRequest updateOrderRequest) {
        log.info("Updating order with ID: {}", updateOrderRequest.orderId());

        OrderEntity order = orderEntityService.findEntityById(UUID.fromString(updateOrderRequest.orderId()))
                .orElseThrow(() -> new BadRequestException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Order with ID: " + updateOrderRequest.orderId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));


        if (!(order.getStatus().equals(OrderStatusEnum.PENDING.name()) || order.getStatus().equals(OrderStatusEnum.PROCESSING.name()))) {
            throw new NotFoundException(
                    ErrorEnum.INVALID_REQUEST.getMessage() + " Cannot update order with ID: " + order.getId() + ", because order status is not PENDING or PROCESSING",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        CategoryEntity categoryEntity = categoryEntityService.findEntityById(UUID.fromString(updateOrderRequest.categoryId()))
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Cate with ID: " + updateOrderRequest.categoryId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        AddressEntity deliveryAddress = addressEntityService.findEntityById(UUID.fromString(updateOrderRequest.deliveryAddressId()))
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage() + "deliveryAddress not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        AddressEntity pickupAddress = addressEntityService.findEntityById(UUID.fromString(updateOrderRequest.pickupAddressId()))
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage() + "pickupAddress not found",
                        ErrorEnum.NOT_FOUND.getErrorCode()));


        order.setNotes(updateOrderRequest.notes());
        order.setReceiverName(updateOrderRequest.receiverName());
        order.setReceiverPhone(updateOrderRequest.receiverPhone());
        order.setPickupAddress(pickupAddress);
        order.setDeliveryAddress(deliveryAddress);
        order.setCategory(categoryEntity);
        order.setPackageDescription(updateOrderRequest.packageDescription());

        orderEntityService.save(order);


        return orderMapper.toCreateOrderResponse(order);
    }


    @Override
    public List<OrderDetailEntity> batchCreateOrderDetails(
            List<CreateOrderDetailRequest> requests,
            OrderEntity savedOrder, LocalDateTime estimateStartTime) {


        // Build all order details in memory first
        List<OrderDetailEntity> orderDetails = requests.stream()
                .map(request -> {
                    OrderSizeEntity orderSizeEntity = orderSizeEntityService.findEntityById(UUID.fromString(request.orderSizeId()))
                            .orElseThrow(() -> new NotFoundException(
                                    ErrorEnum.NOT_FOUND.getMessage() + " orderSize with id: " + request.orderSizeId(),
                                    ErrorEnum.NOT_FOUND.getErrorCode()));
//                    if(orderSizeEntity.getMaxWeight().compareTo(request.weight()) < 0){
//                        throw new BadRequestException(ErrorEnum.INVALID_REQUEST.getMessage() + "orderSize's max weight have to be more than detail's weight", ErrorEnum.NOT_FOUND.getErrorCode());
//                    }
                    return OrderDetailEntity.builder()
                            .weight(request.weight())  // Trọng lượng gốc người dùng nhập
                            .unit(request.unit())
                            .weightBaseUnit(convertToTon(request.weight(), request.unit()))  // Convert về tấn
                            .description(request.description())
                            .status(savedOrder.getStatus())
                            .trackingCode(generateCode(prefixOrderDetailCode))
                            .estimatedStartTime(estimateStartTime)
                            .orderEntity(savedOrder)
                            .orderSizeEntity(orderSizeEntity)
                            .build();
                })
                .collect(Collectors.toList());
        return orderDetailEntityService.saveAllOrderDetailEntities(orderDetails);
    }

    @Override
    public List<GetOrderForGetAllResponse> getAllOrders() {
        return orderMapper.toGetOrderForGetAllResponses(orderEntityService.findAll());
    }

    @Override
    public List<CreateOrderResponse> getOrdersForCusByUserId(UUID userId) {
        log.info("getOrdersForCusByUserId: start");
        CustomerEntity customer = customerEntityService.findByUserId(userId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + ", Customer not found with user id: " + userId,
                        ErrorEnum.NOT_FOUND.getErrorCode()));
        List<OrderEntity> orderEntities = orderEntityService.findBySenderId(customer.getId());
        return orderMapper.toCreateOrderResponses(orderEntities);
    }

    @Override
    public GetOrderResponse getOrderById(UUID orderId) {
        Optional<OrderEntity> order = orderEntityService.findEntityById(orderId);
        if (!order.isPresent()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage() + "Not found order with id: " + orderId, ErrorEnum.NOT_FOUND.getErrorCode());
        }

        return orderMapper.toGetOrderResponse(order.get());
    }

    @Override
    public List<UnitEnum> responseListUnitEnum() {
        return Arrays.asList(UnitEnum.values());
    }


    private boolean checkTotalWeight(BigDecimal totalWeight, List<CreateOrderDetailRequest> listCreateOrderDetailRequests) {
        BigDecimal totalWeightTest = BigDecimal.ZERO;
        for (CreateOrderDetailRequest req : listCreateOrderDetailRequests) {
            totalWeightTest = totalWeightTest.add(req.weight());
        }
        return totalWeightTest.compareTo(totalWeight) <= 0;
    }

    private String generateCode(String prefix) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String randomPart = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        return prefix + timestamp + "-" + randomPart;
    }

    @Override
    public BigDecimal convertToTon(BigDecimal weightBaseUnit, String unit) {
        if (weightBaseUnit == null || unit == null) {
            throw new IllegalArgumentException("weightBaseUnit và unit không được null");
        }

        UnitEnum unitEnum;
        try {
            unitEnum = UnitEnum.valueOf(unit);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Đơn vị không hợp lệ: " + unit);
        }

        return weightBaseUnit.multiply(unitEnum.toTon());
    }

    @Override
    public GetOrderForCustomerResponse getOrderForCustomerByOrderId(UUID orderId) {
        GetOrderResponse getOrderResponse = getOrderById(orderId);

        List<GetIssueImageResponse> getIssueImageResponses = new ArrayList<>();
        Map<UUID, List<PhotoCompletionResponse>> photoCompletionResponses = new HashMap<>();
        for (GetOrderDetailResponse detail : getOrderResponse.orderDetails()) {
            if (detail.vehicleAssignmentId() != null) {
                UUID vehicleAssignmentId = detail.vehicleAssignmentId(); // Use the UUID directly
                getIssueImageResponses.add(
                        issueImageService.getByVehicleAssignment(vehicleAssignmentId)
                );
                photoCompletionResponses.put(vehicleAssignmentId, photoCompletionService.getByVehicleAssignmentId(vehicleAssignmentId));
            }
        }

        ContractResponse contractResponse = null;
        List<TransactionResponse> transactionResponses = new ArrayList<>();

        Optional<ContractEntity> contractEntity = contractEntityService.getContractByOrderId(orderId);
        if (contractEntity.isPresent()) {
            contractResponse = contractService.getContractById(contractEntity.get().getId());
            transactionResponses = payOSTransactionService.getTransactionsByContractId(contractEntity.get().getId());
        }

        return new GetOrderForCustomerResponse(
                getOrderResponse,
                getIssueImageResponses,
                photoCompletionResponses,
                contractResponse,
                transactionResponses
        );
    }

    // java
    @Override
    public SimpleOrderForCustomerResponse getSimplifiedOrderForCustomerByOrderId(UUID orderId) {
        GetOrderResponse getOrderResponse = getOrderById(orderId);

        Map<UUID, GetIssueImageResponse> issuesByVehicleAssignment = new HashMap<>();
        Map<UUID, List<PhotoCompletionResponse>> photosByVehicleAssignment = new HashMap<>();

        // defensive: handle null orderDetails and avoid duplicate calls per vehicleAssignmentId
        Set<UUID> processed = new HashSet<>();
        List<GetOrderDetailResponse> details = Optional.ofNullable(getOrderResponse)
                .map(GetOrderResponse::orderDetails)
                .orElse(Collections.emptyList());

        // Reuse the new helper method to process vehicle assignments
        processVehicleAssignments(details, issuesByVehicleAssignment, photosByVehicleAssignment);

        // contract / transactions same as before
        ContractResponse contractResponse = null;
        List<TransactionResponse> transactionResponses = new ArrayList<>();
        Optional<ContractEntity> contractEntity = contractEntityService.getContractByOrderId(orderId);
        if (contractEntity.isPresent()) {
            contractResponse = contractService.getContractById(contractEntity.get().getId());
            transactionResponses = payOSTransactionService.getTransactionsByContractId(contractEntity.get().getId());
        }

        List<GetIssueImageResponse> issueImageResponsesList = new ArrayList<>(issuesByVehicleAssignment.values());

        return simpleOrderMapper.toSimpleOrderForCustomerResponse(
                getOrderResponse,
                issueImageResponsesList,
                photosByVehicleAssignment,
                contractResponse,
                transactionResponses
        );
    }


    @Override
    public OrderForStaffResponse getOrderForStaffByOrderId(UUID orderId) {
        log.info("Getting order for staff with ID: {}", orderId);

        // Get the basic order information
        GetOrderResponse orderResponse = getOrderById(orderId);

        // Get contract information if available
        ContractResponse contractResponse = null;
        List<TransactionResponse> transactionResponses = new ArrayList<>();

        // Find the contract for this order if it exists
        Optional<ContractEntity> contractEntity = contractEntityService.getContractByOrderId(orderId);
        if (contractEntity.isPresent()) {
            contractResponse = contractService.getContractById(contractEntity.get().getId());
            transactionResponses = payOSTransactionService.getTransactionsByContractId(contractEntity.get().getId());
        }

        // Use our mapper to convert to staff order response
        return staffOrderMapper.toStaffOrderForStaffResponse(
                orderResponse,
                contractResponse,
                transactionResponses
        );
    }

    @Override
    public OrderForDriverResponse getOrderForDriverByOrderId(UUID orderId) {
        GetOrderResponse getOrderResponse = getOrderById(orderId);

        Map<UUID, GetIssueImageResponse> issuesByVehicleAssignment = new HashMap<>();
        Map<UUID, List<PhotoCompletionResponse>> photosByVehicleAssignment = new HashMap<>();

        // defensive: handle null orderDetails and avoid duplicate calls per vehicleAssignmentId
        Set<UUID> processed = new HashSet<>();
        List<GetOrderDetailResponse> details = Optional.ofNullable(getOrderResponse)
                .map(GetOrderResponse::orderDetails)
                .orElse(Collections.emptyList());

        // Reuse the new helper method to process vehicle assignments
        processVehicleAssignments(details, issuesByVehicleAssignment, photosByVehicleAssignment);

        // contract / transactions same as before
        ContractResponse contractResponse = null;
        List<TransactionResponse> transactionResponses = new ArrayList<>();
        Optional<ContractEntity> contractEntity = contractEntityService.getContractByOrderId(orderId);
        if (contractEntity.isPresent()) {
            contractResponse = contractService.getContractById(contractEntity.get().getId());
            transactionResponses = payOSTransactionService.getTransactionsByContractId(contractEntity.get().getId());
        }

        List<GetIssueImageResponse> issueImageResponsesList = new ArrayList<>(issuesByVehicleAssignment.values());

        return driverOrderMapper.toOrderForDriverResponse(
                getOrderResponse,
                issueImageResponsesList,
                photosByVehicleAssignment
        );
    }

    @Override
    @Transactional
    public boolean signContractAndOrder(UUID contractId) {
        ContractEntity contractEntity = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new NotFoundException(
                        "Contract not found with user id: " + contractId,
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        OrderEntity orderEntity = contractEntity.getOrderEntity();

        contractEntity.setStatus(ContractStatusEnum.CONTRACT_SIGNED.name());
        changeAStatusOrder(orderEntity.getId(), OrderStatusEnum.CONTRACT_SIGNED);

        return true;
    }

    @Override
    public boolean updateOrderStatus(UUID orderId, OrderStatusEnum newStatus) {
        Optional<OrderEntity> optionalOrder = orderEntityService.findEntityById(orderId);
        if (optionalOrder.isEmpty()) {
            return false;
        }
        OrderEntity order = optionalOrder.get();
        OrderStatusEnum currentStatus;
        try {
            currentStatus = OrderStatusEnum.valueOf(order.getStatus());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Invalid current status: " + order.getStatus(),
                    ErrorEnum.INVALID.getErrorCode()
            );
        }
        if (currentStatus == null || !isValidTransition(currentStatus, newStatus)) {
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Cannot change from " + order.getStatus() + " to " + newStatus,
                    ErrorEnum.INVALID.getErrorCode()
            );
        }
        
        OrderStatusEnum previousStatus = currentStatus;
        order.setStatus(newStatus.name());
        orderEntityService.save(order);
        
        // Cập nhật trạng thái Seal thành USED khi đơn hàng hoàn thành (DELIVERED hoặc SUCCESSFUL)
        if (newStatus == OrderStatusEnum.DELIVERED || newStatus == OrderStatusEnum.SUCCESSFUL) {
            try {
                // Tìm tất cả Seal liên quan đến Order
                List<OrderDetailEntity> orderDetails = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);

                // Cập nhật trạng thái Seal cho từng VehicleAssignment liên quan
                for (OrderDetailEntity detail : orderDetails) {
                    VehicleAssignmentEntity vehicleAssignment = detail.getVehicleAssignmentEntity();
                    if (vehicleAssignment != null) {
                        // Cập nhật trạng thái các seal từ IN_USE -> USED
                        int updatedSeals = sealService.updateSealsToUsed(vehicleAssignment);
                        if (updatedSeals > 0) {
                            log.info("Đã cập nhật {} seal thành USED cho VehicleAssignment {} khi Order {} chuyển sang trạng thái {}",
                                    updatedSeals, vehicleAssignment.getId(), orderId, newStatus);
                        }
                    }
                }
            } catch (Exception e) {
                // Không throw exception để tránh ảnh hưởng đến luồng xử lý chính
                log.error("Lỗi khi cập nhật trạng thái seal cho đơn hàng {}: {}", orderId, e.getMessage());
            }
        }

        // Send WebSocket notification for status change
        try {
            orderStatusWebSocketService.sendOrderStatusChange(
                orderId,
                order.getOrderCode(),
                previousStatus,
                newStatus
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for order status change: {}", e.getMessage());
            // Don't throw - WebSocket failure shouldn't break business logic
        }
        
        return true;
    }

    @Override
    public List<GetOrderForDriverResponse> getOrderForDriverByCurrentDrive() {
        log.info("Getting order for current driver");

        UUID driverId = userContextUtils.getCurrentDriverId();

        List<OrderEntity> ordersByDriverId = orderEntityService.findOrdersByDriverId(driverId);

        if (ordersByDriverId.isEmpty()) {
            log.info("No orders found for driver with ID: {}", driverId);
            throw new BadRequestException(
                    ErrorEnum.NOT_FOUND.getMessage() + " Cannot found orders from " + driverId,
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        return ordersByDriverId.stream()
                .map(orderMapper::toGetOrderForDriverResponse)
                .collect(Collectors.toList());
    }

    @Override
    public GetOrderByJpaResponse getSimplifiedOrderForCustomerV2ByOrderId(UUID orderId) {
        log.info("Getting order for order with ID: {}", orderId);

        if (orderId == null) {
            log.info("No orders found for order with ID: {}", orderId);
            throw new BadRequestException(
                    ErrorEnum.NOT_FOUND.getMessage(),
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()));

        return orderMapper.toGetOrderByJpaResponse(order);
    }

    @Override
    @Transactional
    public CreateOrderResponse updateToOngoingDelivered(UUID orderId) {
        log.info("Updating order {} to ONGOING_DELIVERED status", orderId);

        // Validate order ID
        if (orderId == null) {
            throw new BadRequestException(
                    "Order ID cannot be null",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Find order
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Order not found with ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Validate current status is ON_DELIVERED
        if (!OrderStatusEnum.ON_DELIVERED.name().equals(order.getStatus())) {
            throw new BadRequestException(
                    String.format("Cannot update to ONGOING_DELIVERED. Current status is %s, expected ON_DELIVERED", 
                            order.getStatus()),
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Update status to ONGOING_DELIVERED
        order.setStatus(OrderStatusEnum.ONGOING_DELIVERED.name());
        OrderEntity updatedOrder = orderEntityService.save(order);

        log.info("Successfully updated order {} to ONGOING_DELIVERED", orderId);

        // Send WebSocket notification
//        orderStatusWebSocketService.sendOrderStatusUpdate(
//                orderId,
//                OrderStatusEnum.ONGOING_DELIVERED,
//                "Xe đang trên đường giao hàng (trong phạm vi 3km)"
//        );

        return orderMapper.toCreateOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public CreateOrderResponse updateToDelivered(UUID orderId) {
        log.info("Updating order {} to DELIVERED status", orderId);

        // Validate order ID
        if (orderId == null) {
            throw new BadRequestException(
                    "Order ID cannot be null",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Find order
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Order not found with ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Validate current status is ONGOING_DELIVERED
        if (!OrderStatusEnum.ONGOING_DELIVERED.name().equals(order.getStatus())) {
            throw new BadRequestException(
                    String.format("Cannot update to DELIVERED. Current status is %s, expected ONGOING_DELIVERED", 
                            order.getStatus()),
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Update status to DELIVERED
        order.setStatus(OrderStatusEnum.DELIVERED.name());
        OrderEntity updatedOrder = orderEntityService.save(order);

        log.info("Successfully updated order {} to DELIVERED", orderId);

        // Send WebSocket notification
//        orderStatusWebSocketService.sendOrderStatusUpdate(
//                orderId,
//                OrderStatusEnum.DELIVERED,
//                "Đã đến điểm giao hàng"
//        );

        return orderMapper.toCreateOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public CreateOrderResponse updateToSuccessful(UUID orderId) {
        log.info("Updating order {} to SUCCESSFUL status", orderId);

        // Validate order ID
        if (orderId == null) {
            throw new BadRequestException(
                    "Order ID cannot be null",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Find order
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Order not found with ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Validate current status is DELIVERED
        if (!OrderStatusEnum.DELIVERED.name().equals(order.getStatus())) {
            throw new BadRequestException(
                    String.format("Cannot update to SUCCESSFUL. Current status is %s, expected DELIVERED", 
                            order.getStatus()),
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Update status to SUCCESSFUL
        order.setStatus(OrderStatusEnum.SUCCESSFUL.name());
        OrderEntity updatedOrder = orderEntityService.save(order);

        log.info("Successfully updated order {} to SUCCESSFUL", orderId);

        // Send WebSocket notification
//        orderStatusWebSocketService.sendOrderStatusUpdate(
//                orderId,
//                OrderStatusEnum.SUCCESSFUL,
//                "Chuyến xe đã hoàn thành thành công"
//        );

        return orderMapper.toCreateOrderResponse(updatedOrder);
    }

    /**
     * Helper method to process vehicle assignments for order details
     * Extracts issue images and photo completions for each vehicle assignment
     */
    private void processVehicleAssignments(
            List<GetOrderDetailResponse> details,
            Map<UUID, GetIssueImageResponse> issuesByVehicleAssignment,
            Map<UUID, List<PhotoCompletionResponse>> photosByVehicleAssignment
    ) {
        // defensive: handle null orderDetails and avoid duplicate calls per vehicleAssignmentId
        Set<UUID> processed = new HashSet<>();

        for (GetOrderDetailResponse detail : details) {
            if (detail == null) continue;
            UUID va = detail.vehicleAssignmentId();
            if (va == null) continue;
            if (!processed.add(va)) continue; // skip duplicates

            try {
                GetIssueImageResponse issueImageResponse = issueImageService.getByVehicleAssignment(va);
                if (issueImageResponse != null) {
                    issuesByVehicleAssignment.put(va, issueImageResponse);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch issue images for vehicleAssignment {}: {}", va, e.getMessage());
            }

            try {
                List<PhotoCompletionResponse> photoCompletions = photoCompletionService.getByVehicleAssignmentId(va);
                if (photoCompletions != null && !photoCompletions.isEmpty()) {
                    photosByVehicleAssignment.put(va, photoCompletions);
                }
            } catch (Exception e) {
                log.warn("Failed to fetch photo completions for vehicleAssignment {}: {}", va, e.getMessage());
            }
        }
    }
}
