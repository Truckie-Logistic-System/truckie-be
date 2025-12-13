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
import capstone_project.entity.issue.IssueEntity;
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
import capstone_project.service.services.order.order.OrderDetailStatusWebSocketService;
import capstone_project.service.services.order.order.PhotoCompletionService;
import capstone_project.service.services.order.seal.SealService;
import capstone_project.service.services.order.transaction.payOS.PayOSTransactionService;
import capstone_project.service.services.notification.NotificationService;
import capstone_project.service.services.notification.NotificationBuilder;
import capstone_project.service.services.pricing.InsuranceCalculationService;
import capstone_project.service.services.setting.ContractSettingService;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
import capstone_project.service.services.order.order.OrderCancellationContext;
import capstone_project.config.order.OrderCancellationConfig;
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
    private final OrderDetailStatusWebSocketService orderDetailStatusWebSocketService;
    private final SealService sealService; // Thêm SealService
    private final capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final capstone_project.repository.entityServices.issue.IssueEntityService issueEntityService;
    private final capstone_project.repository.entityServices.issue.IssueImageEntityService issueImageEntityService;
    private final NotificationService notificationService; // For notification creation
    private final capstone_project.service.mapper.issue.IssueMapper issueMapper;
    private final InsuranceCalculationService insuranceCalculationService; // For insurance fee calculation
    private final capstone_project.repository.entityServices.auth.UserEntityService userEntityService; // For staff notifications
    private final ContractSettingService contractSettingService; // For deposit rate calculation
    private final OrderCancellationConfig orderCancellationConfig; // For cancellation reasons

    @Value("${prefix.order.code}")
    private String prefixOrderCode;
    @Value("${prefix.order.detail.code}")
    private String prefixOrderDetailCode;

    /**
     * Helper method to determine if a status requires ALL order details to have that status
     * before updating the parent order status
     */
    private boolean requiresAllAggregation(OrderStatusEnum status) {
        return Set.of(
            OrderStatusEnum.IN_TROUBLES,
            OrderStatusEnum.DELIVERED,
            OrderStatusEnum.RETURNED,
            OrderStatusEnum.SUCCESSFUL,
            OrderStatusEnum.COMPENSATION
        ).contains(status);
    }

    @Override
    public List<OrderForCustomerListResponse> getOrdersForCurrentCustomer() {
        UUID customerId = userContextUtils.getCurrentCustomerId();
        List<OrderEntity> orderEntities = orderEntityService.findBySenderId(customerId);
        return orderMapper.toOrderForCustomerListResponses(orderEntities);
    }

    @Override
    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequest orderRequest, List<CreateOrderDetailRequest> listCreateOrderDetailRequests) {
        
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
            // Xác định có mua bảo hiểm hay không (mặc định: false)
            Boolean hasInsurance = orderRequest.hasInsurance() != null ? orderRequest.hasInsurance() : false;
            
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
                    .hasInsurance(hasInsurance)
                    .totalInsuranceFee(BigDecimal.ZERO)
                    .totalDeclaredValue(BigDecimal.ZERO)
                    .build();
            OrderEntity saveOrder = orderEntityService.save(newOrder);

            saveOrder.setOrderDetailEntities(batchCreateOrderDetails(listCreateOrderDetailRequests, saveOrder, orderRequest.estimateStartTime()));
            
            // Tính phí bảo hiểm nếu có mua bảo hiểm
            CategoryName categoryName = category.getCategoryName();
            insuranceCalculationService.updateOrderInsurance(saveOrder, categoryName);
            saveOrder = orderEntityService.save(saveOrder);
            
            // Create ORDER_CREATED notification for customer with full package details
            try {
                List<OrderDetailEntity> orderDetails = saveOrder.getOrderDetailEntities() != null ? 
                    new ArrayList<>(saveOrder.getOrderDetailEntities()) : new ArrayList<>();
                
                log.info("🔍 Creating ORDER_CREATED notification with {} packages", orderDetails.size());
                
                // Debug: Log all order details
                orderDetails.forEach(detail -> 
                    log.info("🔍 Package: {} - {} - {} {}", 
                        detail.getTrackingCode(), 
                        detail.getDescription(),
                        detail.getWeightBaseUnit(),
                        detail.getUnit())
                );
                
                CreateNotificationRequest notificationRequest = NotificationBuilder.buildOrderCreated(
                    sender.getUser().getId(),
                    saveOrder.getOrderCode(),
                    orderDetails,
                    saveOrder.getId()
                );
                
                notificationService.createNotification(notificationRequest);
                log.info("✅ Created ORDER_CREATED notification for order: {}", saveOrder.getOrderCode());
                
                // STAFF_ORDER_CREATED - Notify all staff about new order
                try {
                    String customerName = sender.getRepresentativeName() != null ? 
                        sender.getRepresentativeName() : sender.getUser().getUsername();
                    String customerPhone = sender.getRepresentativePhone() != null ? 
                        sender.getRepresentativePhone() : "N/A";
                    int packageCount = orderDetails.size();
                    
                    // Calculate total weight
                    double totalWeight = orderDetails.stream()
                        .filter(detail -> detail.getWeightBaseUnit() != null)
                        .mapToDouble(detail -> detail.getWeightBaseUnit().doubleValue())
                        .sum();
                    String weightUnit = orderDetails.stream()
                        .filter(detail -> detail.getUnit() != null && !detail.getUnit().isEmpty())
                        .map(OrderDetailEntity::getUnit)
                        .findFirst()
                        .orElse("kg");
                    
                    var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
                    for (var staff : staffUsers) {
                        CreateNotificationRequest staffNotification = NotificationBuilder.buildStaffOrderCreated(
                            staff.getId(),
                            saveOrder.getOrderCode(),
                            customerName,
                            customerPhone,
                            packageCount,
                            totalWeight,
                            weightUnit,
                            saveOrder.getId()
                        );
                        notificationService.createNotification(staffNotification);
                    }
                    log.info("✅ Created STAFF_ORDER_CREATED notifications for {} staff users", staffUsers.size());
                } catch (Exception staffEx) {
                    log.error("❌ Failed to create STAFF_ORDER_CREATED notifications: {}", staffEx.getMessage());
                }
                
            } catch (Exception e) {
                log.error("❌ Failed to create ORDER_CREATED notification: {}", e.getMessage());
                // Don't fail the order creation if notification fails
            }
            
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

        // Create notification for staff when customer agrees to vehicle proposal (PENDING → PROCESSING)
        if (previousStatus == OrderStatusEnum.PENDING && newStatus == OrderStatusEnum.PROCESSING) {
            try {
                // Extract customer info for notification
                String customerName = order.getSender().getRepresentativeName() != null ? 
                    order.getSender().getRepresentativeName() : order.getSender().getUser().getUsername();
                String customerPhone = order.getSender().getRepresentativePhone() != null ? 
                    order.getSender().getRepresentativePhone() : "N/A";
                int packageCount = order.getOrderDetailEntities() != null ? order.getOrderDetailEntities().size() : 0;
                
                // Create staff notification
                CreateNotificationRequest staffNotificationRequest = NotificationBuilder.buildStaffOrderProcessing(
                    null, // Broadcast to all staff
                    order.getOrderCode(),
                    customerName,
                    customerPhone,
                    packageCount,
                    order.getId()
                );
                
                notificationService.createNotification(staffNotificationRequest);
                log.info("✅ Created STAFF_ORDER_PROCESSING notification for order: {}", order.getOrderCode());
                
                // Create customer notification
                CreateNotificationRequest customerNotificationRequest = NotificationBuilder.buildOrderProcessing(
                    order.getSender().getUser().getId(),
                    order.getOrderCode(),
                    packageCount,
                    order.getId()
                );
                
                notificationService.createNotification(customerNotificationRequest);
                log.info("✅ Created ORDER_PROCESSING notification for customer in order: {}", order.getOrderCode());
                
            } catch (Exception e) {
                log.error("❌ Failed to create notifications for order processing: {}", e.getMessage());
                // Don't throw - notification failure shouldn't break business logic
            }
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
        
        // Validate that cascading is only used for initialization states
        // Delivery progress states should be set via aggregation from OrderDetails
        if (!isValidForCascadingUpdate(newStatus)) {
            throw new BadRequestException(
                    ErrorEnum.INVALID.getMessage() + " Status " + newStatus + " should be updated via aggregation from OrderDetails, not cascading from Order. Use updateOrderStatus() instead.",
                    ErrorEnum.INVALID.getErrorCode()
            );
        }
        // Update Order
        OrderStatusEnum previousStatus = currentStatus;
        order.setStatus(newStatus.name());
        orderEntityService.save(order);

        // Update toàn bộ OrderDetail với status tương ứng
        List<OrderDetailEntity> orderDetailEntities = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
        
        // Map OrderStatusEnum to OrderDetailStatusEnum
        OrderDetailStatusEnum correspondingDetailStatus = mapOrderStatusToDetailStatus(newStatus);
        
        orderDetailEntities.forEach(detail -> detail.setStatus(correspondingDetailStatus.name()));
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
                return  next == OrderStatusEnum.CONTRACT_SIGNED;
            case CONTRACT_SIGNED:
                return next == OrderStatusEnum.ON_PLANNING;
            case ON_PLANNING:
                return next == OrderStatusEnum.ASSIGNED_TO_DRIVER;
            case ASSIGNED_TO_DRIVER:
                return next == OrderStatusEnum.FULLY_PAID;
            case FULLY_PAID:
                return next == OrderStatusEnum.PICKING_UP
                        || next == OrderStatusEnum.IN_TROUBLES;
            case PICKING_UP:
                return next == OrderStatusEnum.ON_DELIVERED
                        || next == OrderStatusEnum.ONGOING_DELIVERED
                        || next == OrderStatusEnum.IN_TROUBLES;
            case ON_DELIVERED:
                return next == OrderStatusEnum.ONGOING_DELIVERED || next == OrderStatusEnum.IN_TROUBLES;
            case ONGOING_DELIVERED:
                return next == OrderStatusEnum.DELIVERED || next == OrderStatusEnum.IN_TROUBLES || next == OrderStatusEnum.RETURNING;
            case IN_TROUBLES:
                return next == OrderStatusEnum.FULLY_PAID ||  next == OrderStatusEnum.PICKING_UP || next == OrderStatusEnum.ON_DELIVERED ||  next == OrderStatusEnum.ONGOING_DELIVERED ||  next == OrderStatusEnum.DELIVERED || next == OrderStatusEnum.COMPENSATION
                        || next == OrderStatusEnum.RETURNING;
            case DELIVERED:
                return next == OrderStatusEnum.SUCCESSFUL || next == OrderStatusEnum.IN_TROUBLES;
            case RETURNING:
                return next == OrderStatusEnum.RETURNED || next == OrderStatusEnum.IN_TROUBLES;
            case RETURNED, COMPENSATION:
                return next == OrderStatusEnum.SUCCESSFUL;
        }
        return false;
    }

    @Override
    public List<CreateOrderResponse> getCreateOrderRequestsBySenderId(UUID senderId) {
        
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
                            .weightTons(convertToTon(request.weight(), request.unit()))  // Converted weight for validation
                            .unit(request.unit())
                            .weightBaseUnit(request.weight())  // Raw weight for display
                            .description(request.description())
                            .status(savedOrder.getStatus())
                            .trackingCode(generateCode(prefixOrderDetailCode))
                            .estimatedStartTime(estimateStartTime)
                            .orderEntity(savedOrder)
                            .orderSizeEntity(orderSizeEntity)
                            .declaredValue(request.declaredValue() != null ? request.declaredValue() : BigDecimal.ZERO)
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
                GetIssueImageResponse issue = issueImageService.getByVehicleAssignment(vehicleAssignmentId);
                
                // Filter OUT_OFF_ROUTE_RUNAWAY issues from customer view
                if (issue != null && issue.issue() != null && 
                        issue.issue().issueCategory() != IssueCategoryEnum.OFF_ROUTE_RUNAWAY) {
                    getIssueImageResponses.add(issue);
                }
                
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

        Map<UUID, List<GetIssueImageResponse>> issuesByVehicleAssignment = new HashMap<>();
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

        // Flatten List<List<GetIssueImageResponse>> to List<GetIssueImageResponse>
        List<GetIssueImageResponse> issueImageResponsesList = issuesByVehicleAssignment.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

        SimpleOrderForCustomerResponse response = simpleOrderMapper.toSimpleOrderForCustomerResponse(
                getOrderResponse,
                issueImageResponsesList,
                photosByVehicleAssignment,
                contractResponse,
                transactionResponses
        );
        
        // Enrich ORDER_REJECTION issues with transactions (post-processing to avoid circular dependency)
        enrichIssuesWithTransactions(response);
        
        return response;
    }

    @Override
    public OrderForStaffResponse getOrderForStaffByOrderId(UUID orderId) {

        // Get the basic order information
        GetOrderResponse orderResponse = getOrderById(orderId);

        // Get issues and photos for vehicle assignments
        Map<UUID, List<GetIssueImageResponse>> issuesByVehicleAssignment = new HashMap<>();
        Map<UUID, List<PhotoCompletionResponse>> photosByVehicleAssignment = new HashMap<>();

        List<GetOrderDetailResponse> details = Optional.ofNullable(orderResponse)
                .map(GetOrderResponse::orderDetails)
                .orElse(Collections.emptyList());

        // Reuse the helper method to process vehicle assignments
        processVehicleAssignments(details, issuesByVehicleAssignment, photosByVehicleAssignment);

        // Flatten issues list
        List<GetIssueImageResponse> issueImageResponsesList = issuesByVehicleAssignment.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

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

        Map<UUID, List<GetIssueImageResponse>> issuesByVehicleAssignment = new HashMap<>();
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

        // Flatten List<List<GetIssueImageResponse>> to List<GetIssueImageResponse>
        List<GetIssueImageResponse> issueImageResponsesList = issuesByVehicleAssignment.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());

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

        // Update contract status to SIGNED
        contractEntity.setStatus(ContractStatusEnum.CONTRACT_SIGNED.name());
        
        // Set deposit payment deadline using contract settings
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        var contractSetting = contractSettingService.getLatestContractSetting();
        if (contractSetting == null) {
            throw new NotFoundException("Contract settings not found", ErrorEnum.NOT_FOUND.getErrorCode());
        }
        contractEntity.setDepositPaymentDeadline(now.plusHours(contractSetting.depositDeadlineHours()));
        
        // Save contract with updated deadline
        contractEntityService.save(contractEntity);
        
        log.info("✅ Contract {} signed successfully. Deposit payment deadline set to: {}", 
                contractId, contractEntity.getDepositPaymentDeadline());
        
        // Update order status
        changeAStatusOrder(orderEntity.getId(), OrderStatusEnum.CONTRACT_SIGNED);

        // Create notifications for contract signing
        try {
            String contractCode = contractEntity.getContractName() != null ? contractEntity.getContractName() : "HĐ-" + orderEntity.getOrderCode();
            
            // Calculate deposit amount using contract settings (same logic as ContractServiceImpl)
            double baseValue = (contractEntity.getAdjustedValue() != null && contractEntity.getAdjustedValue().doubleValue() > 0) ? 
                contractEntity.getAdjustedValue().doubleValue() : 
                (contractEntity.getTotalValue() != null ? contractEntity.getTotalValue().doubleValue() : 0.0);
            
            double depositAmount = 0.0;
            try {
                // Prioritize contract's custom deposit percent, fallback to global setting
                BigDecimal depositPercent = getEffectiveDepositPercent(contractEntity);
                depositAmount = baseValue * depositPercent.doubleValue() / 100.0;
                log.info("📊 Contract signed notification - Using deposit percent: {}% (custom: {})", 
                    depositPercent, contractEntity.getCustomDepositPercent() != null ? "yes" : "no");
            } catch (Exception e) {
                log.warn("Could not get deposit percent, using 10% default: {}", e.getMessage());
                depositAmount = baseValue * 0.1;
            }
            
            log.info("🔍 Final deposit calculation: baseValue={}, depositRate={}, depositAmount={}", 
                baseValue, depositAmount / baseValue * 100, depositAmount);
            
            // Notification 1: Customer - CONTRACT_SIGNED (Email: NO)
            CreateNotificationRequest customerNotification = NotificationBuilder.buildContractSigned(
                orderEntity.getSender().getUser().getId(),
                orderEntity.getOrderCode(),
                contractCode,
                depositAmount,
                contractEntity.getDepositPaymentDeadline(),
                orderEntity.getId(),
                contractEntity.getId()
            );
            notificationService.createNotification(customerNotification);
            log.info("✅ Created CONTRACT_SIGNED notification for customer in order: {}", orderEntity.getOrderCode());
            
            // Notification 2: All Staff - STAFF_CONTRACT_SIGNED
            String customerName = orderEntity.getSender().getRepresentativeName() != null ?
                orderEntity.getSender().getRepresentativeName() : orderEntity.getSender().getUser().getUsername();
            
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            for (var staff : staffUsers) {
                CreateNotificationRequest staffNotification = NotificationBuilder.buildStaffContractSigned(
                    staff.getId(),
                    orderEntity.getOrderCode(),
                    contractCode,
                    customerName,
                    orderEntity.getId(),
                    contractEntity.getId()
                );
                notificationService.createNotification(staffNotification);
            }
            log.info("✅ Created STAFF_CONTRACT_SIGNED notifications for {} staff users", staffUsers.size());
            
        } catch (Exception e) {
            log.error("❌ Failed to create contract signing notifications: {}", e.getMessage());
            // Don't throw - notification failure shouldn't break contract signing
        }

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
        
        // Validate aggregation for statuses that require ALL order details to match
        if (requiresAllAggregation(newStatus)) {
            List<OrderDetailEntity> orderDetails = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
            boolean allDetailsMatch = orderDetails.stream()
                    .allMatch(detail -> detail.getStatus().equals(newStatus.name()));
            
            if (!allDetailsMatch) {
                throw new BadRequestException(
                        ErrorEnum.INVALID.getMessage() + " Cannot update order status to " + newStatus + 
                        " because not all order details have this status. " +
                        "This status requires all order details to be " + newStatus + " first.",
                        ErrorEnum.INVALID.getErrorCode()
                );
            }
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

        UUID driverId = userContextUtils.getCurrentDriverId();

        List<OrderEntity> ordersByDriverId = orderEntityService.findOrdersByDriverId(driverId);

        if (ordersByDriverId.isEmpty()) {
            
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

        if (orderId == null) {
            
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

        // Validate current status is ON_DELIVERED or already ONGOING_DELIVERED
        if (!OrderStatusEnum.ON_DELIVERED.name().equals(order.getStatus()) && 
            !OrderStatusEnum.ONGOING_DELIVERED.name().equals(order.getStatus())) {
            throw new BadRequestException(
                    String.format("Cannot update to ONGOING_DELIVERED. Current status is %s, expected ON_DELIVERED or ONGOING_DELIVERED", 
                            order.getStatus()),
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // If already ONGOING_DELIVERED, just return current order
        if (OrderStatusEnum.ONGOING_DELIVERED.name().equals(order.getStatus())) {
            
            return orderMapper.toCreateOrderResponse(order);
        }

        // Update status to ONGOING_DELIVERED
        OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());
        order.setStatus(OrderStatusEnum.ONGOING_DELIVERED.name());
        OrderEntity updatedOrder = orderEntityService.save(order);

        // Send WebSocket notification for status change
        try {
            orderStatusWebSocketService.sendOrderStatusChange(
                orderId,
                order.getOrderCode(),
                previousStatus,
                OrderStatusEnum.ONGOING_DELIVERED
            );
        } catch (Exception e) {
            log.error("Failed to send WebSocket notification for order status change: {}", e.getMessage());
        }

        return orderMapper.toCreateOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public CreateOrderResponse updateToDelivered(UUID orderId) {

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

        // Use centralized aggregation logic: only allow DELIVERED when ALL order details are DELIVERED
        updateOrderStatus(orderId, OrderStatusEnum.DELIVERED);

        // Reload order after status update
        OrderEntity updatedOrder = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Order not found with ID after update: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        return orderMapper.toCreateOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public CreateOrderResponse updateToSuccessful(UUID orderId) {

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

        // Use centralized aggregation logic: only allow SUCCESSFUL when ALL order details are SUCCESSFUL
        updateOrderStatus(orderId, OrderStatusEnum.SUCCESSFUL);

        // Reload order after status update
        OrderEntity updatedOrder = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Order not found with ID after update: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        return orderMapper.toCreateOrderResponse(updatedOrder);
    }

    @Override
    @Transactional
    public boolean cancelOrder(UUID orderId) {
        // Create context for customer cancellation
        OrderCancellationContext context = OrderCancellationContext.builder()
                .cancellationType(OrderCancellationContext.CancellationType.CUSTOMER_CANCEL)
                .sendNotifications(true)
                .cleanupReservations(true)
                .build();

        return cancelOrderUnified(orderId, context);
    }

    @Override
    @Transactional
    public boolean staffCancelOrder(UUID orderId, String cancellationReason) {
        // Create context for staff cancellation
        OrderCancellationContext context = OrderCancellationContext.builder()
                .cancellationType(OrderCancellationContext.CancellationType.STAFF_CANCEL)
                .customReason(cancellationReason)
                .sendNotifications(true)
                .cleanupReservations(true)
                .build();

        return cancelOrderUnified(orderId, context);
    }

    @Override
    public List<String> getStaffCancellationReasons() {
        return orderCancellationConfig.getStaffReasons();
    }

    @Override
    @Transactional
    public boolean cancelOrderUnified(UUID orderId, OrderCancellationContext context) {
        log.info("🚫 Unified order cancellation initiated for orderId: {}, type: {}, reason: {}", 
                orderId, context.getCancellationType(), context.getCancellationReason());

        // Validate inputs
        if (orderId == null || context == null) {
            throw new BadRequestException(
                    "Order ID and cancellation context cannot be null",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Find order
        OrderEntity order = orderEntityService.findEntityById(orderId)
                .orElseThrow(() -> new NotFoundException(
                        "Order not found with ID: " + orderId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Validate order can be cancelled based on cancellation type
        validateCancellationEligibility(order, context);

        // Store previous status for WebSocket notification
        OrderStatusEnum previousStatus = OrderStatusEnum.valueOf(order.getStatus());

        try {
            // ═══════════════════════════════════════════════════════════════
            // STEP 1: Update order status and cancellation reason
            // ═══════════════════════════════════════════════════════════════
            order.setStatus(OrderStatusEnum.CANCELLED.name());
            order.setCancellationReason(context.getCancellationReason());
            orderEntityService.save(order);
            log.info("✅ Updated order {} status to CANCELLED", order.getOrderCode());

            // ═══════════════════════════════════════════════════════════════
            // STEP 2: Cancel all order details
            // ═══════════════════════════════════════════════════════════════
            List<OrderDetailEntity> orderDetails = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(orderId);
            if (!orderDetails.isEmpty()) {
                orderDetails.forEach(detail -> {
                    detail.setStatus(OrderDetailStatusEnum.CANCELLED.name());
                });
                orderDetailEntityService.saveAllOrderDetailEntities(orderDetails);
                log.info("✅ Updated {} order details to CANCELLED for order {}", orderDetails.size(), order.getOrderCode());
            }

            // ═══════════════════════════════════════════════════════════════
            // STEP 3: Update contract status if exists
            // ═══════════════════════════════════════════════════════════════
            try {
                Optional<ContractEntity> contractOpt = contractEntityService.getContractByOrderId(orderId);
                if (contractOpt.isPresent()) {
                    ContractEntity contract = contractOpt.get();
                    
                    // Set contract status based on cancellation type
                    if (context.getCancellationType() == OrderCancellationContext.CancellationType.CONTRACT_EXPIRY) {
                        contract.setStatus(ContractStatusEnum.EXPIRED.name());
                    } else {
                        contract.setStatus(ContractStatusEnum.CANCELLED.name());
                    }
                    
                    contractEntityService.save(contract);
                    log.info("✅ Updated contract {} status to {} for order {}", 
                            contract.getId(), contract.getStatus(), order.getOrderCode());
                }
            } catch (Exception e) {
                log.error("❌ Failed to update contract status for cancelled order {}: {}", orderId, e.getMessage());
                // Continue with cancellation - contract failure shouldn't block the main cancellation
            }

            // ═══════════════════════════════════════════════════════════════
            // STEP 4: Cleanup vehicle reservations if requested
            // ═══════════════════════════════════════════════════════════════
            if (context.shouldCleanupReservations()) {
                try {
                    cleanupVehicleReservations(orderId);
                    log.info("✅ Cleaned up vehicle reservations for order {}", order.getOrderCode());
                } catch (Exception e) {
                    log.warn("⚠️ Failed to cleanup vehicle reservations for order {}: {}", orderId, e.getMessage());
                    // Continue with cancellation - reservation cleanup failure shouldn't block
                }
            }

            // ═══════════════════════════════════════════════════════════════
            // STEP 5: Send WebSocket notifications
            // ═══════════════════════════════════════════════════════════════
            try {
                orderStatusWebSocketService.sendOrderStatusChange(
                        orderId,
                        order.getOrderCode(),
                        previousStatus,
                        OrderStatusEnum.CANCELLED
                );
                
                // Send order detail status changes for multi-trip support
                for (OrderDetailEntity detail : orderDetails) {
                    UUID vehicleAssignmentId = detail.getVehicleAssignmentEntity() != null ? 
                            detail.getVehicleAssignmentEntity().getId() : null;
                    
                    orderDetailStatusWebSocketService.sendOrderDetailStatusChange(
                            detail.getId(),
                            detail.getTrackingCode(),
                            orderId,
                            order.getOrderCode(),
                            vehicleAssignmentId,
                            OrderDetailStatusEnum.valueOf(detail.getStatus()),
                            OrderDetailStatusEnum.CANCELLED
                    );
                }
            } catch (Exception e) {
                log.error("❌ Failed to send WebSocket notification for cancelled order {}: {}", orderId, e.getMessage());
                // Continue with cancellation - WebSocket failure shouldn't block
            }

            // ═══════════════════════════════════════════════════════════════
            // STEP 6: Send notifications to users if requested
            // ═══════════════════════════════════════════════════════════════
            if (context.shouldSendNotifications()) {
                try {
                    sendCancellationNotifications(order, orderDetails, context);
                    log.info("✅ Sent cancellation notifications for order {}", order.getOrderCode());
                } catch (Exception e) {
                    log.error("❌ Failed to send notifications for cancelled order {}: {}", orderId, e.getMessage());
                    // Continue with cancellation - notification failure shouldn't block
                }
            }

            log.info("✅ Successfully cancelled order {} via unified cancellation", order.getOrderCode());
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to cancel order {} via unified cancellation: {}", orderId, e.getMessage(), e);
            throw new InternalServerException(
                    "Failed to cancel order: " + e.getMessage(),
                    ErrorEnum.INTERNAL_SERVER_ERROR.getErrorCode()
            );
        }
    }

    /**
     * Validate if order can be cancelled based on cancellation type and current status
     */
    private void validateCancellationEligibility(OrderEntity order, OrderCancellationContext context) {
        String currentStatus = order.getStatus();
        List<String> allowedStatuses;

        switch (context.getCancellationType()) {
            case CUSTOMER_CANCEL:
                // Customer can cancel PENDING, PROCESSING, CONTRACT_DRAFT orders
                allowedStatuses = Arrays.asList(
                        OrderStatusEnum.PENDING.name(),
                        OrderStatusEnum.PROCESSING.name(),
                        OrderStatusEnum.CONTRACT_DRAFT.name()
                );
                break;
                
            case STAFF_CANCEL:
                // Staff can only cancel PROCESSING orders
                allowedStatuses = Arrays.asList(OrderStatusEnum.PROCESSING.name());
                break;
                
            case PAYMENT_TIMEOUT:
            case CONTRACT_EXPIRY:
            case SYSTEM_CANCEL:
                // System cancellations have broader permissions
                // Note: ON_PLANNING is included as safety measure, though orders at this stage
                // should not normally be cancelled by payment/contract expiry checks
                allowedStatuses = Arrays.asList(
                        OrderStatusEnum.PENDING.name(),
                        OrderStatusEnum.PROCESSING.name(),
                        OrderStatusEnum.CONTRACT_DRAFT.name(),
                        OrderStatusEnum.CONTRACT_SIGNED.name(),
                        OrderStatusEnum.ON_PLANNING.name()
                );
                break;
                
            default:
                throw new BadRequestException(
                        "Unsupported cancellation type: " + context.getCancellationType(),
                        ErrorEnum.INVALID_REQUEST.getErrorCode()
                );
        }

        if (!allowedStatuses.contains(currentStatus)) {
            throw new BadRequestException(
                    String.format("Cannot cancel order. Current status is %s. Allowed statuses for %s are: %s", 
                            currentStatus, context.getCancellationType(), allowedStatuses),
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }
    }

    /**
     * Clean up vehicle reservations for the cancelled order
     */
    private void cleanupVehicleReservations(UUID orderId) {
        // Implementation would depend on the reservation system
        // This is a placeholder for future reservation cleanup logic
        log.info("🧹 Cleaning up vehicle reservations for order: {}", orderId);
        // TODO: Implement actual reservation cleanup when reservation system is ready
    }

    /**
     * Send cancellation notifications to relevant parties
     */
    private void sendCancellationNotifications(OrderEntity order, List<OrderDetailEntity> orderDetails, OrderCancellationContext context) {
        // Send notification to customer
        if (order.getSender() != null && order.getSender().getUser() != null) {
            int totalPackageCount = orderDetails != null ? orderDetails.size() : 0;
            List<UUID> cancelledOrderDetailIds = orderDetails != null 
                    ? orderDetails.stream().map(OrderDetailEntity::getId).collect(Collectors.toList()) 
                    : List.of();

            CreateNotificationRequest customerNotification = NotificationBuilder.buildOrderCancelledMultiTrip(
                    order.getSender().getUser().getId(),
                    order.getOrderCode(),
                    totalPackageCount, // cancelledCount
                    totalPackageCount, // totalPackageCount
                    context.getCancellationReason(),
                    order.getId(),
                    cancelledOrderDetailIds,
                    true // allPackagesCancelled
            );
            notificationService.createNotification(customerNotification);
        }

        // For staff cancellations, also notify staff
        if (context.getCancellationType() == OrderCancellationContext.CancellationType.STAFF_CANCEL) {
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            String customerName = order.getSender() != null && order.getSender().getUser() != null 
                    ? order.getSender().getUser().getFullName() : "Khách hàng";
            for (var staff : staffUsers) {
                CreateNotificationRequest staffNotification = NotificationBuilder.buildStaffOrderCancelled(
                        staff.getId(),
                        order.getOrderCode(),
                        orderDetails.size(), // cancelledCount
                        orderDetails.size(), // totalPackageCount
                        context.getCancellationReason(),
                        customerName,
                        order.getId()
                );
                notificationService.createNotification(staffNotification);
            }
        }
    }

    /**
     * Enrich ORDER_REJECTION issues with transactions after mapper creates the response
     * This is done as post-processing to avoid circular dependency between services
     */
    private void enrichIssuesWithTransactions(SimpleOrderForCustomerResponse response) {
        if (response == null || response.order() == null || response.order().vehicleAssignments() == null) {
            return;
        }
        
        for (var vehicleAssignment : response.order().vehicleAssignments()) {
            if (vehicleAssignment.issues() == null) continue;
            
            for (var issue : vehicleAssignment.issues()) {
                // Only enrich ORDER_REJECTION issues
                if (issue.issueCategory() == capstone_project.common.enums.IssueCategoryEnum.ORDER_REJECTION && 
                    issue.id() != null) {
                    try {
                        // Fetch transactions for this issue
                        List<TransactionResponse> transactions = payOSTransactionService.getTransactionsByIssueId(
                            UUID.fromString(issue.id())
                        );
                        
                        // Create new enriched issue with transactions
                        var enrichedIssue = new capstone_project.dtos.response.issue.SimpleIssueResponse(
                            issue.id(),
                            issue.description(),
                            issue.locationLatitude(),
                            issue.locationLongitude(),
                            issue.status(),
                            issue.vehicleAssignmentId(),
                            issue.staff(),
                            issue.issueTypeName(),
                            issue.issueTypeDescription(),
                            issue.reportedAt(),
                            issue.issueCategory(),
                            issue.issueImages(),
                            issue.oldSeal(),
                            issue.newSeal(),
                            issue.sealRemovalImage(),
                            issue.newSealAttachedImage(),
                            issue.newSealConfirmedAt(),
                            issue.paymentDeadline(),
                            issue.calculatedFee(),
                            issue.adjustedFee(),
                            issue.finalFee(),
                            issue.affectedOrderDetails(),
                            issue.refund(),
                            issue.transaction(),
                            transactions // Add transactions
                        );
                        
                        // Replace the issue in the list with enriched version
                        // Note: This requires the issues list to be mutable
                        int index = vehicleAssignment.issues().indexOf(issue);
                        if (index >= 0 && vehicleAssignment.issues() instanceof java.util.ArrayList) {
                            ((java.util.ArrayList<capstone_project.dtos.response.issue.SimpleIssueResponse>) vehicleAssignment.issues())
                                .set(index, enrichedIssue);
                        }
                    } catch (Exception e) {
                        log.warn("Failed to enrich issue {} with transactions: {}", issue.id(), e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * Helper method to process vehicle assignments for order details
     * Extracts issue images and photo completions for each vehicle assignment
     */
    private void processVehicleAssignments(
            List<GetOrderDetailResponse> details,
            Map<UUID, List<GetIssueImageResponse>> issuesByVehicleAssignment,
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
                // Get ALL issues for this vehicle assignment
                List<GetIssueImageResponse> issuesList = new ArrayList<>();
                
                var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(va);
                if (vehicleAssignment.isPresent()) {
                    // Query ALL issues (no status filter - get OPEN, IN_PROGRESS, RESOLVED, etc.)
                    List<IssueEntity> allIssues = issueEntityService.findAllByVehicleAssignmentEntity(vehicleAssignment.get());
                    
                    for (IssueEntity issue : allIssues)  {
                        try {
                            // Convert issue entity to response
                            var basicIssue = issueMapper.toIssueBasicResponse(issue);
                            
                            // Fetch issue images for this issue
                            List<String> imageUrls = new ArrayList<>();
                            try {
                                var issueImages = issueImageEntityService.findByIssueEntity_Id(issue.getId());
                                if (issueImages != null && !issueImages.isEmpty()) {
                                    imageUrls = issueImages.stream()
                                            .map(capstone_project.entity.issue.IssueImageEntity::getImageUrl)
                                            .filter(Objects::nonNull)
                                            .collect(Collectors.toList());
                                }
                            } catch (Exception imgEx) {
                                log.warn("Failed to fetch images for issue {}: {}", issue.getId(), imgEx.getMessage());
                            }
                            
                            issuesList.add(new GetIssueImageResponse(
                                    basicIssue,
                                    imageUrls
                            ));
                        } catch (Exception e2) {
                            log.warn("Failed to map issue {} for vehicleAssignment {}: {}", 
                                    issue.getId(), va, e2.getMessage());
                        }
                    }
                    
                    if (!issuesList.isEmpty()) {
                        issuesByVehicleAssignment.put(va, issuesList);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to fetch issues for vehicleAssignment {}: {}", va, e.getMessage());
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

    @Override
    public RecipientOrderTrackingResponse getOrderForRecipientByOrderCode(String orderCode) {
        if (orderCode == null || orderCode.trim().isEmpty()) {
            throw new BadRequestException(
                    "Mã đơn hàng không được để trống",
                    ErrorEnum.INVALID_REQUEST.getErrorCode()
            );
        }

        // Find order by order code
        OrderEntity orderEntity = orderEntityService.findByOrderCode(orderCode.trim())
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy đơn hàng với mã: " + orderCode,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        UUID orderId = orderEntity.getId();

        // Get order response (reuse existing logic)
        GetOrderResponse orderResponse = getOrderById(orderId);

        // Get issues and photo completions (reuse existing logic)
        Map<UUID, List<GetIssueImageResponse>> issuesByVehicleAssignment = new HashMap<>();
        Map<UUID, List<PhotoCompletionResponse>> photosByVehicleAssignment = new HashMap<>();

        List<GetOrderDetailResponse> details = Optional.ofNullable(orderResponse)
                .map(GetOrderResponse::orderDetails)
                .orElse(Collections.emptyList());

        processVehicleAssignments(details, issuesByVehicleAssignment, photosByVehicleAssignment);

        // Filter OUT_OFF_ROUTE_RUNAWAY issues from customer/guest view
        List<GetIssueImageResponse> issueImageResponsesList = issuesByVehicleAssignment.values().stream()
                .flatMap(List::stream)
                .filter(issue -> issue.issue() != null && 
                        issue.issue().issueCategory() != IssueCategoryEnum.OFF_ROUTE_RUNAWAY)
                .collect(Collectors.toList());

        // Convert to SimpleOrderResponse WITHOUT contract/transaction data
        SimpleOrderResponse simpleOrderResponse = simpleOrderMapper.toSimpleOrderForCustomerResponse(
                orderResponse,
                issueImageResponsesList,
                photosByVehicleAssignment,
                null, // No contract data for recipient
                Collections.emptyList() // No transaction data for recipient
        ).order();

        return new RecipientOrderTrackingResponse(simpleOrderResponse);
    }

    /**
     * Check if the status is valid for cascading update from Order to OrderDetails
     * Only initialization states should use cascading, delivery progress states should aggregate from OrderDetails
     */
    private boolean isValidForCascadingUpdate(OrderStatusEnum status) {
        return switch (status) {
            // Initialization states - safe for cascading
            case PENDING, PROCESSING, CONTRACT_DRAFT, CONTRACT_SIGNED, ON_PLANNING, CANCELLED -> true;
            
            // Delivery progress states - should aggregate from OrderDetails, not cascade
            case ASSIGNED_TO_DRIVER, FULLY_PAID, PICKING_UP, ON_DELIVERED, ONGOING_DELIVERED, 
                 IN_TROUBLES, COMPENSATION, DELIVERED, SUCCESSFUL, RETURNING, RETURNED -> false;
        };
    }

    /**
     * Map OrderStatusEnum to corresponding OrderDetailStatusEnum
     * Used for cascading status updates from Order to OrderDetails
     */
    private OrderDetailStatusEnum mapOrderStatusToDetailStatus(OrderStatusEnum orderStatus) {
        return switch (orderStatus) {
            case PENDING -> OrderDetailStatusEnum.PENDING;
            case PROCESSING -> OrderDetailStatusEnum.PENDING; // Processing order maps to PENDING details
            case CANCELLED -> OrderDetailStatusEnum.CANCELLED;
            case CONTRACT_DRAFT -> OrderDetailStatusEnum.PENDING;
            case CONTRACT_SIGNED -> OrderDetailStatusEnum.PENDING;
            case ON_PLANNING -> OrderDetailStatusEnum.ON_PLANNING;
            case ASSIGNED_TO_DRIVER -> OrderDetailStatusEnum.ASSIGNED_TO_DRIVER;
            case FULLY_PAID -> OrderDetailStatusEnum.ASSIGNED_TO_DRIVER; // Fully paid order maps to assigned details
            case PICKING_UP -> OrderDetailStatusEnum.PICKING_UP;
            case ON_DELIVERED -> OrderDetailStatusEnum.ON_DELIVERED;
            case ONGOING_DELIVERED -> OrderDetailStatusEnum.ONGOING_DELIVERED;
            case IN_TROUBLES -> OrderDetailStatusEnum.IN_TROUBLES;
            case COMPENSATION -> OrderDetailStatusEnum.COMPENSATION;
            case DELIVERED -> OrderDetailStatusEnum.DELIVERED;
            case SUCCESSFUL -> OrderDetailStatusEnum.DELIVERED; // SUCCESSFUL maps to DELIVERED for order details
            case RETURNING -> OrderDetailStatusEnum.RETURNING;
            case RETURNED -> OrderDetailStatusEnum.RETURNED;
        };
    }
    
    /**
     * Get effective deposit percent for a contract.
     * Prioritizes contract's custom deposit percent if set, otherwise falls back to global setting.
     * 
     * @param contract The contract to get deposit percent for
     * @return The effective deposit percent (0-100)
     */
    private BigDecimal getEffectiveDepositPercent(ContractEntity contract) {
        // First, check if contract has custom deposit percent
        if (contract.getCustomDepositPercent() != null 
            && contract.getCustomDepositPercent().compareTo(BigDecimal.ZERO) > 0
            && contract.getCustomDepositPercent().compareTo(BigDecimal.valueOf(100)) <= 0) {
            return contract.getCustomDepositPercent();
        }
        
        // Fallback to global setting
        var contractSetting = contractSettingService.getLatestContractSetting();
        if (contractSetting != null && contractSetting.depositPercent() != null) {
            BigDecimal depositPercent = contractSetting.depositPercent();
            if (depositPercent.compareTo(BigDecimal.ZERO) > 0 && depositPercent.compareTo(BigDecimal.valueOf(100)) <= 0) {
                return depositPercent;
            }
        }
        
        // Default fallback
        log.warn("No valid deposit percent found, using 10% default");
        return BigDecimal.valueOf(10);
    }
}
