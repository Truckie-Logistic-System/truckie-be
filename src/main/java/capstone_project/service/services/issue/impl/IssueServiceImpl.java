package capstone_project.service.services.issue.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.IssueEnum;
import capstone_project.common.enums.IssueCategoryEnum;
import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.exceptions.dto.BadRequestException;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.response.order.contract.PriceCalculationResponse;
import capstone_project.entity.issue.IssueImageEntity;
import capstone_project.entity.issue.IssueTypeEntity;
import capstone_project.entity.vehicle.VehicleAssignmentEntity;
import capstone_project.service.services.websocket.IssueWebSocketService;
import capstone_project.dtos.request.issue.*;
import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.user.driver.PenaltyHistoryEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.issue.IssueEntityService;
import capstone_project.repository.entityServices.issue.IssueTypeEntityService;
import capstone_project.repository.entityServices.vehicle.VehicleAssignmentEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.SealEntityService;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.SealEntity;
import capstone_project.common.enums.SealEnum;
import org.springframework.transaction.annotation.Transactional;
import capstone_project.service.mapper.issue.IssueMapper;
import capstone_project.service.mapper.order.SealMapper;
import capstone_project.service.services.issue.IssueService;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.notification.NotificationService;
import capstone_project.service.services.notification.NotificationBuilder;
import capstone_project.dtos.request.notification.CreateNotificationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.service.services.user.UserService;

@Service
@RequiredArgsConstructor
@Slf4j
public class IssueServiceImpl implements IssueService {
    private final VehicleAssignmentEntityService vehicleAssignmentEntityService;
    private final UserEntityService userEntityService;
    private final IssueEntityService issueEntityService;
    private final IssueTypeEntityService issueTypeEntityService;
    private final IssueMapper issueMapper;
    private final IssueWebSocketService issueWebSocketService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final SealEntityService sealEntityService;
    private final CloudinaryService cloudinaryService;
    private final SealMapper sealMapper;
    private final capstone_project.repository.entityServices.issue.IssueImageEntityService issueImageEntityService;
    
    // ORDER_REJECTION dependencies
    private final capstone_project.service.services.order.order.ContractService contractService;
    private final capstone_project.repository.entityServices.order.order.OrderEntityService orderEntityService;
    private final capstone_project.repository.entityServices.order.contract.ContractEntityService contractEntityService;
    private final capstone_project.repository.entityServices.order.contract.ContractRuleEntityService contractRuleEntityService;
    private final capstone_project.repository.entityServices.user.CustomerEntityService customerEntityService;
    private final capstone_project.repository.entityServices.order.order.JourneyHistoryEntityService journeyHistoryEntityService;
    private final capstone_project.repository.entityServices.order.transaction.TransactionEntityService transactionEntityService;
    private final capstone_project.service.services.order.transaction.payOS.PayOSTransactionService payOSTransactionService;
    private final capstone_project.service.services.order.order.OrderDetailStatusWebSocketService orderDetailStatusWebSocketService;
    
    // ✅ NEW: OrderDetailStatusService for centralized Order status aggregation
    private final capstone_project.service.services.order.order.OrderDetailStatusService orderDetailStatusService;
    
    // ✅ REROUTE dependencies
    private final capstone_project.service.mapper.vehicle.VehicleAssignmentMapper vehicleAssignmentMapper;
    private final capstone_project.service.mapper.order.JourneyHistoryMapper journeyHistoryMapper;
    private final capstone_project.repository.repositories.order.order.JourneyHistoryRepository journeyHistoryRepository;
    private final UserService userService;
    
    
    // Return payment deadline configuration
    @org.springframework.beans.factory.annotation.Value("${issue.return-payment.deadline-minutes:30}")
    private int returnPaymentDeadlineMinutes;
    
    // Payment timeout scheduler for real-time timeout detection
    private final capstone_project.config.expired.PaymentTimeoutSchedulerService paymentTimeoutSchedulerService;
    
    // ✅ NEW: Email service for customer notifications
    private final capstone_project.service.services.email.EmailNotificationService emailNotificationService;
    
    // ✅ Vietmap Service for getting suggested routes (Route V3 API)
    private final capstone_project.service.services.thirdPartyServices.Vietmap.VietmapService vietmapService;
    
    // ✅ Notification Service for persistent notifications
    private final capstone_project.service.services.notification.NotificationService notificationService;
    
    // ✅ OFF_ROUTE_RUNAWAY dependencies
    private final capstone_project.repository.entityServices.offroute.OffRouteEventEntityService offRouteEventEntityService;
    private final capstone_project.service.services.refund.RefundService refundService;
    private final capstone_project.repository.entityServices.user.PenaltyHistoryEntityService penaltyHistoryEntityService;
    
    // ✅ DAMAGE/OFF_ROUTE compensation - unified service
    private final capstone_project.service.CompensationService compensationService;

    @Override
    public GetBasicIssueResponse getBasicIssue(UUID issueId) {
        // Use findByIdWithDetails to eagerly fetch all related entities
        IssueEntity getIssue = issueEntityService.findByIdWithDetails(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        // Mapper will handle all nested mappings including:
        // - vehicleAssignment with vehicle, drivers
        // - issueImages
        // - orderDetail
        // - affectedSegment (for REROUTE)
        // - reroutedJourney (for REROUTE)
        return issueMapper.toIssueBasicResponse(getIssue);
    }
    
    private capstone_project.dtos.response.vehicle.VehicleAssignmentResponse mapVehicleAssignmentWithDetails(
            capstone_project.entity.vehicle.VehicleAssignmentEntity entity) {
        if (entity == null) return null;
        
        // Map vehicle info
        capstone_project.dtos.response.vehicle.VehicleAssignmentResponse.VehicleInfo vehicleInfo = null;
        if (entity.getVehicleEntity() != null) {
            var vehicle = entity.getVehicleEntity();
            var vehicleType = vehicle.getVehicleTypeEntity() != null
                ? new capstone_project.dtos.response.vehicle.VehicleAssignmentResponse.VehicleTypeInfo(
                    vehicle.getVehicleTypeEntity().getId(),
                    vehicle.getVehicleTypeEntity().getVehicleTypeName(),
                    vehicle.getVehicleTypeEntity().getDescription()
                )
                : null;
                
            vehicleInfo = new capstone_project.dtos.response.vehicle.VehicleAssignmentResponse.VehicleInfo(
                vehicle.getId(),
                vehicle.getLicensePlateNumber(),
                vehicle.getModel(),
                vehicle.getManufacturer(),
                vehicle.getYear(),
                vehicleType
            );
        }
        
        // Map driver 1
        capstone_project.dtos.response.vehicle.VehicleAssignmentResponse.DriverInfo driver1 = null;
        if (entity.getDriver1() != null) {
            var d1 = entity.getDriver1();
            driver1 = new capstone_project.dtos.response.vehicle.VehicleAssignmentResponse.DriverInfo(
                d1.getId(),
                d1.getUser() != null ? d1.getUser().getFullName() : "N/A",
                d1.getUser() != null ? d1.getUser().getPhoneNumber() : null,
                d1.getDriverLicenseNumber(),
                d1.getLicenseClass(),
                null // experienceYears không có trong entity
            );
        }
        
        // Map driver 2
        capstone_project.dtos.response.vehicle.VehicleAssignmentResponse.DriverInfo driver2 = null;
        if (entity.getDriver2() != null) {
            var d2 = entity.getDriver2();
            driver2 = new capstone_project.dtos.response.vehicle.VehicleAssignmentResponse.DriverInfo(
                d2.getId(),
                d2.getUser() != null ? d2.getUser().getFullName() : "N/A",
                d2.getUser() != null ? d2.getUser().getPhoneNumber() : null,
                d2.getDriverLicenseNumber(),
                d2.getLicenseClass(),
                null // experienceYears không có trong entity
            );
        }
        
        return new capstone_project.dtos.response.vehicle.VehicleAssignmentResponse(
            entity.getId(),
            entity.getVehicleEntity() != null ? entity.getVehicleEntity().getId() : null,
            entity.getDriver1() != null ? entity.getDriver1().getId() : null,
            entity.getDriver2() != null ? entity.getDriver2().getId() : null,
            entity.getStatus(),
            entity.getTrackingCode(),
            vehicleInfo,
            driver1,
            driver2,
            new java.util.ArrayList<>()  // Empty devices list for this context
        );
    }

    @Override
    public GetBasicIssueResponse getByVehicleAssignment(UUID vehicleAssignmentId) {
        IssueEntity entity = issueEntityService.findByVehicleAssignmentEntity(
                vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId).get()
        );

        return issueMapper.toIssueBasicResponse(entity);
    }

    @Override
    public List<GetBasicIssueResponse> getByStaffId(UUID staffId) {
        List<IssueEntity> entity = issueEntityService.findByStaff(
                userEntityService.findEntityById(staffId).get()
        );
        return issueMapper.toIssueBasicResponses(entity);
    }

    @Override
    public List<GetBasicIssueResponse> getByActiveStatus() {
        List<IssueEntity> list = issueEntityService.findByStatus(IssueEnum.OPEN.name());
        if(list.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),ErrorEnum.NOT_FOUND.getErrorCode());
        }
        return issueMapper.toIssueBasicResponses(list);
    }

    @Override
    public List<GetBasicIssueResponse> getIssueType(UUID issueTypeId) {
        List<IssueEntity> issueType = issueEntityService.findByIssueTypeEntity(issueTypeEntityService.findEntityById(issueTypeId).get());
        if(issueType.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),ErrorEnum.NOT_FOUND.getErrorCode());
        }
        return issueMapper.toIssueBasicResponses(issueType);
    }

    @Override
    @Transactional
    public GetBasicIssueResponse createIssue(CreateBasicIssueRequest request) {
        // Lấy VehicleAssignment
        var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(request.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Lấy IssueType
        var issueType = issueTypeEntityService.findEntityById(request.issueTypeId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.issueTypeId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Lấy tất cả order details của vehicle assignment này
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(vehicleAssignment);

        // ✅ CRITICAL: Save statuses as JSON map to support combined reports
        // Format: {"orderDetailId1":"STATUS1","orderDetailId2":"STATUS2"}
        // This handles cases where different packages have different statuses (e.g., DELIVERED, IN_TROUBLES, RETURNING)
        String tripStatusAtReport = null;
        if (!orderDetails.isEmpty()) {
            StringBuilder jsonBuilder = new StringBuilder("{");
            for (int i = 0; i < orderDetails.size(); i++) {
                OrderDetailEntity od = orderDetails.get(i);
                if (i > 0) jsonBuilder.append(",");
                jsonBuilder.append("\"").append(od.getId()).append("\":\"").append(od.getStatus()).append("\"");
            }
            jsonBuilder.append("}");
            tripStatusAtReport = jsonBuilder.toString();
            log.info("💾 Saved trip status at report (JSON): {}", tripStatusAtReport);
        }

        // Update tất cả order details thành IN_TROUBLES with WebSocket notification
        orderDetails.forEach(orderDetail -> {
            updateOrderDetailStatusWithNotification(
                orderDetail,
                OrderDetailStatusEnum.IN_TROUBLES,
                vehicleAssignment.getId()
            );
        });

        // Tạo entity
        IssueEntity issue = IssueEntity.builder()
                .description(request.description())
                .locationLatitude(request.locationLatitude())
                .locationLongitude(request.locationLongitude())
                .status(IssueEnum.OPEN.name()) // mặc định OPEN khi tạo
                // issueCategory đã được di chuyển sang IssueTypeEntity
                .reportedAt(java.time.LocalDateTime.now())
                .tripStatusAtReport(tripStatusAtReport) // Lưu status cũ để restore
                .vehicleAssignmentEntity(vehicleAssignment)
                .staff(null)
                .issueTypeEntity(issueType)
                .build();

        // Lưu
        IssueEntity saved = issueEntityService.save(issue);
        
        // Create issue notification
        createIssueNotifications(saved);

        // Convert sang response
        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(saved);
        
        // 📢 Broadcast new issue to all staff clients via WebSocket
        
        issueWebSocketService.broadcastNewIssue(response);
        
        return response;
    }

    @Override
    public GetBasicIssueResponse updateIssue(UpdateBasicIssueRequest request) {
        // Tìm Issue cũ
        IssueEntity existing = issueEntityService.findEntityById(request.issueId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.issueId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Lấy IssueType mới
        var issueType = issueTypeEntityService.findEntityById(request.issueTypeId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.issueTypeId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Update thông tin
        existing.setDescription(request.description());
        existing.setLocationLatitude(request.locationLatitude());
        existing.setLocationLongitude(request.locationLongitude());
        existing.setIssueTypeEntity(issueType);

        // Lưu lại
        IssueEntity updated = issueEntityService.save(existing);

        // Convert sang response
        return issueMapper.toIssueBasicResponse(updated);
    }

    @Override
    public GetBasicIssueResponse updateStaffForIssue(UUID staffId, UUID issueId) {
        // Tìm Issue cũ
        IssueEntity existing = issueEntityService.findEntityById(issueId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + issueId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        UserEntity staff = userEntityService.findEntityById(staffId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + staffId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        existing.setStaff(staff);
        existing.setStatus(IssueEnum.IN_PROGRESS.name());

        // Lưu lại
        IssueEntity updated = issueEntityService.save(existing);

        // Convert sang response
        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(updated);
        
        // 📢 Broadcast status change (OPEN -> IN_PROGRESS)
        
        issueWebSocketService.broadcastIssueStatusChange(response);
        
        return response;
    }

    @Override
    public List<GetBasicIssueResponse> getInactiveStatus() {
        List<IssueEntity> list = issueEntityService.findByStatus(IssueEnum.RESOLVED.name());
        if(list.isEmpty()) {
            throw new NotFoundException(ErrorEnum.NOT_FOUND.getMessage(),ErrorEnum.NOT_FOUND.getErrorCode());
        }
        return issueMapper.toIssueBasicResponses(list);
    }

    @Override
    public List<GetBasicIssueResponse> getAllIssues() {
        return issueMapper.toIssueBasicResponses(issueEntityService.findAllSortedByReportedAtDesc());
    }

    @Override
    @Transactional
    public GetBasicIssueResponse resolveIssue(UUID issueId) {
        // Tìm Issue
        IssueEntity issue = issueEntityService.findEntityById(issueId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + issueId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Kiểm tra issue đã được assign staff chưa
        if (issue.getStaff() == null) {
            throw new IllegalStateException("Issue must be assigned to staff before resolving");
        }

        // Kiểm tra issue đang ở trạng thái IN_PROGRESS
        if (!IssueEnum.IN_PROGRESS.name().equals(issue.getStatus())) {
            throw new IllegalStateException("Only IN_PROGRESS issues can be resolved");
        }

        // ✅ CRITICAL: Restore order detail statuses from JSON map
        var vehicleAssignment = issue.getVehicleAssignmentEntity();
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(vehicleAssignment);

        String tripStatusAtReport = issue.getTripStatusAtReport();
        
        if (tripStatusAtReport != null && !tripStatusAtReport.isEmpty()) {
            // Get only order details linked to this issue
            List<OrderDetailEntity> issueOrderDetails = orderDetails.stream()
                    .filter(od -> od.getIssueEntity() != null && od.getIssueEntity().getId().equals(issue.getId()))
                    .collect(java.util.stream.Collectors.toList());
            
            UUID vehicleAssignmentId = issue.getVehicleAssignmentEntity() != null ? 
                    issue.getVehicleAssignmentEntity().getId() : null;
            
            try {
                // Parse JSON map: {"uuid1":"STATUS1","uuid2":"STATUS2"}
                if (tripStatusAtReport.startsWith("{") && tripStatusAtReport.endsWith("}")) {
                    log.info("🔄 Restoring statuses from JSON (resolveIssue): {}", tripStatusAtReport);
                    
                    // Simple JSON parsing
                    String jsonContent = tripStatusAtReport.substring(1, tripStatusAtReport.length() - 1);
                    String[] entries = jsonContent.split(",");
                    
                    java.util.Map<String, String> statusMap = new java.util.HashMap<>();
                    for (String entry : entries) {
                        String[] parts = entry.split(":");
                        if (parts.length == 2) {
                            String id = parts[0].replace("\"", "").trim();
                            String status = parts[1].replace("\"", "").trim();
                            statusMap.put(id, status);
                        }
                    }
                    
                    // Restore each OrderDetail to its original status
                    for (OrderDetailEntity orderDetail : issueOrderDetails) {
                        String savedStatus = statusMap.get(orderDetail.getId().toString());
                        if (savedStatus != null) {
                            try {
                                OrderDetailStatusEnum restoredStatusEnum = OrderDetailStatusEnum.valueOf(savedStatus);
                                updateOrderDetailStatusWithNotification(
                                    orderDetail,
                                    restoredStatusEnum,
                                    vehicleAssignmentId
                                );
                                log.info("✅ Restored OrderDetail {} to status {}", orderDetail.getId(), savedStatus);
                            } catch (IllegalArgumentException e) {
                                log.warn("⚠️ Invalid status '{}' for OrderDetail {}", savedStatus, orderDetail.getId());
                            }
                        }
                    }
                } else {
                    // Fallback: Old format (single status for all - backward compatibility)
                    log.warn("⚠️ Old format detected (resolveIssue), restoring all to: {}", tripStatusAtReport);
                    OrderDetailStatusEnum restoredStatusEnum = OrderDetailStatusEnum.valueOf(tripStatusAtReport.trim());
                    for (OrderDetailEntity orderDetail : issueOrderDetails) {
                        updateOrderDetailStatusWithNotification(
                            orderDetail,
                            restoredStatusEnum,
                            vehicleAssignmentId
                        );
                    }
                }
            } catch (Exception e) {
                log.error("❌ Failed to restore statuses (resolveIssue): {}", e.getMessage(), e);
            }
        } else {
            log.warn("⚠️ No tripStatusAtReport found for issue {}, cannot restore statuses", issueId);
        }
        
        // ✅ CRITICAL FIX: Trigger Order status aggregation after restoring OrderDetails
        // This ensures Order status is updated based on ALL OrderDetails after restoration
        // Handles case: All OrderDetails were IN_TROUBLES → Order IN_TROUBLES
        //               After restore → Order status should reflect restored statuses
        if (!orderDetails.isEmpty()) {
            UUID orderId = orderDetails.get(0).getOrderEntity().getId();
            log.info("🔄 Triggering Order status update after OrderDetail restoration for Order {}", orderId);
            try {
                orderDetailStatusService.triggerOrderStatusUpdate(orderId);
                log.info("✅ Order status updated successfully after issue resolution");
            } catch (Exception e) {
                log.error("❌ Failed to update Order status after issue resolution: {}", e.getMessage(), e);
            }
        }

        // Update issue status to RESOLVED
        issue.setStatus(IssueEnum.RESOLVED.name());
        issue.setResolvedAt(java.time.LocalDateTime.now());
        
        IssueEntity updated = issueEntityService.save(issue);

        // Convert sang response
        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(updated);
        
        // 📢 Broadcast status change (IN_PROGRESS -> RESOLVED)
        
        issueWebSocketService.broadcastIssueStatusChange(response);
        
        // 📲 Send notification to driver if this is a DAMAGE issue
        // if (issue.getIssueTypeEntity() != null && 
        //     IssueCategoryEnum.DAMAGE.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            
        //     if (issue.getVehicleAssignmentEntity() != null) {
        //         var driver1 = issue.getVehicleAssignmentEntity().getDriver1();
        //         if (driver1 != null && driver1.getUser() != null) {
        //             String staffName = issue.getStaff() != null ? 
        //                               issue.getStaff().getFullName() : "Nhân viên";
                    
        //             // CRITICAL FIX: Use driver ID (not user ID) to match mobile app subscription
        //             
        //             issueWebSocketService.sendDamageResolvedNotification(
        //                 driver1.getId().toString(),
        //                 response,
        //                 staffName
        //             );
        //         }
        //     }
        // }
        
        return response;
    }

    @Override
    @Transactional
    public GetBasicIssueResponse updateIssueStatus(UUID issueId, String status) {

        // Validate status
        try {
            IssueEnum.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid issue status: " + status);
        }

        // Find issue
        IssueEntity issue = issueEntityService.findEntityById(issueId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.ISSUE_NOT_FOUND.getMessage() + " Issue ID: " + issueId,
                        ErrorEnum.ISSUE_NOT_FOUND.getErrorCode()
                ));

        String oldStatus = issue.getStatus();
        
        // Update status
        issue.setStatus(status);
        
        // Set resolvedAt if status is RESOLVED
        if (IssueEnum.RESOLVED.name().equals(status)) {
            issue.setResolvedAt(java.time.LocalDateTime.now());
            
        }

        IssueEntity updated = issueEntityService.save(issue);
        GetBasicIssueResponse response = getBasicIssue(updated.getId());

        // Broadcast status change
        
        issueWebSocketService.broadcastIssueStatusChange(response);

        return response;
    }

    @Override
    @Transactional
    public GetBasicIssueResponse reportSealIssue(ReportSealIssueRequest request) {

        // Lấy VehicleAssignment
        var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(request.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Lấy IssueType
        var issueType = issueTypeEntityService.findEntityById(request.issueTypeId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.issueTypeId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // 🆕 Debug IssueType

        // Lấy Seal cũ bị gỡ
        SealEntity oldSeal = sealEntityService.findEntityById(request.sealId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.sealId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Kiểm tra seal có thuộc vehicle assignment này không
        if (!oldSeal.getVehicleAssignment().getId().equals(vehicleAssignment.getId())) {
            throw new IllegalStateException("Seal does not belong to this vehicle assignment");
        }

        // ✅ CRITICAL: Save statuses as JSON map to support combined reports
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(vehicleAssignment);
        String tripStatusAtReport = null;
        if (!orderDetails.isEmpty()) {
            StringBuilder jsonBuilder = new StringBuilder("{");
            for (int i = 0; i < orderDetails.size(); i++) {
                OrderDetailEntity od = orderDetails.get(i);
                if (i > 0) jsonBuilder.append(",");
                jsonBuilder.append("\"").append(od.getId()).append("\":\"").append(od.getStatus()).append("\"");
            }
            jsonBuilder.append("}");
            tripStatusAtReport = jsonBuilder.toString();
            log.info("💾 Saved trip status at report for damage issue (JSON): {}", tripStatusAtReport);
        }

        // Update tất cả order details thành IN_TROUBLES with WebSocket notification
        orderDetails.forEach(orderDetail -> {
            updateOrderDetailStatusWithNotification(
                orderDetail,
                OrderDetailStatusEnum.IN_TROUBLES,
                vehicleAssignment.getId()
            );
        });

        // Upload seal removal image to Cloudinary
        String sealRemovalImageUrl;
        try {
            
            String fileName = "seal_removal_" + oldSeal.getId() + "_" + System.currentTimeMillis();
            var uploadResult = cloudinaryService.uploadFile(
                    request.sealRemovalImage().getBytes(),
                    fileName,
                    "issues/seal-removal"
            );
            sealRemovalImageUrl = (String) uploadResult.get("secure_url");
            
        } catch (Exception e) {
            log.error("❌ Error uploading seal removal image: {}", e.getMessage());
            throw new RuntimeException("Failed to upload seal removal image", e);
        }

        // Tạo Issue với thông tin seal
        IssueEntity issue = IssueEntity.builder()
                .description(request.description())
                .locationLatitude(request.locationLatitude() != null ? 
                                 java.math.BigDecimal.valueOf(request.locationLatitude()) : null)
                .locationLongitude(request.locationLongitude() != null ? 
                                  java.math.BigDecimal.valueOf(request.locationLongitude()) : null)
                .status(IssueEnum.OPEN.name())
                // issueCategory đã được di chuyển sang IssueTypeEntity
                .reportedAt(java.time.LocalDateTime.now())
                .tripStatusAtReport(tripStatusAtReport)
                .vehicleAssignmentEntity(vehicleAssignment)
                .staff(null)
                .issueTypeEntity(issueType)
                .oldSeal(oldSeal) // Seal bị gỡ
                .sealRemovalImage(sealRemovalImageUrl) // Cloudinary URL
                .build();

        // Update old seal status to REMOVED
        oldSeal.setStatus(SealEnum.REMOVED.name());
        sealEntityService.save(oldSeal);

        // Lưu issue
        IssueEntity saved = issueEntityService.save(issue);

        // Fetch full issue with all nested objects (vehicle, drivers, images)
        GetBasicIssueResponse response = getBasicIssue(saved.getId());

        // Broadcast seal issue to staff
        issueWebSocketService.broadcastNewIssue(response);
        
        // 📧 Send persistent notification to STAFF about new seal issue
        try {
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            if (!staffUsers.isEmpty()) {
                var driver = vehicleAssignment.getDriver1();
                var order = !orderDetails.isEmpty() ? orderDetails.get(0).getOrderEntity() : null;
                
                for (var staff : staffUsers) {
                    var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildNewIssueReported(
                        staff.getId(),
                        capstone_project.service.services.notification.NotificationBuilder.generateIssueCode(saved.getId()),
                        issueType.getIssueTypeName(),
                        order != null ? order.getOrderCode() : "N/A",
                        driver != null && driver.getUser() != null ? driver.getUser().getFullName() : "Tài xế",
                        driver != null && driver.getUser() != null ? driver.getUser().getPhoneNumber() : "N/A",
                        vehicleAssignment.getVehicleEntity() != null ? vehicleAssignment.getVehicleEntity().getLicensePlateNumber() : "N/A",
                        order != null ? order.getId() : null,
                        saved.getId(),
                        vehicleAssignment.getId()
                    );
                    
                    notificationService.createNotification(notificationRequest);
                }
                log.info("📧 Staff notifications created for new seal issue");
            }
        } catch (Exception e) {
            log.error("❌ Failed to create staff notifications for seal issue", e);
            // Don't fail the main flow if notification fails
        }
        
        // 📧 Send persistent notification to CUSTOMER about seal removal
        try {
            if (!orderDetails.isEmpty()) {
                var order = orderDetails.get(0).getOrderEntity();
                var customer = order.getSender();
                
                var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildSealReplaced(
                    customer.getId(),
                    order.getOrderCode(),
                    capstone_project.service.services.notification.NotificationBuilder.generateIssueCode(saved.getId()),
                    oldSeal.getSealCode(),
                    "ĐANG CHỜ GÁN SEAL MỚI",
                    "Tài xế",
                    "Seal " + oldSeal.getSealCode() + " đã được tháo do: " + request.description(),
                    order.getId(),
                    saved.getId(),
                    vehicleAssignment.getId()
                );
                
                notificationService.createNotification(notificationRequest);
                log.info("📧 Customer notification created for seal removal");
            }
        } catch (Exception e) {
            log.error("❌ Failed to create customer notification for seal removal", e);
            // Don't fail the main flow if notification fails
        }

        return response;
    }

    @Override
    @Transactional
    public GetBasicIssueResponse assignNewSeal(AssignNewSealRequest request) {

        // Tìm Issue
        IssueEntity issue = issueEntityService.findEntityById(request.issueId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.issueId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Kiểm tra issue có phải seal issue không
        if (issue.getOldSeal() == null) {
            throw new IllegalStateException("This is not a seal replacement issue");
        }

        // Kiểm tra issue đang ở trạng thái OPEN
        if (!IssueEnum.OPEN.name().equals(issue.getStatus())) {
            throw new IllegalStateException("Only OPEN seal issues can be assigned new seal");
        }

        // Lấy Staff
        UserEntity staff = userEntityService.findEntityById(request.staffId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.staffId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Lấy Seal mới
        SealEntity newSeal = sealEntityService.findEntityById(request.newSealId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.newSealId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Debug log để kiểm tra seal status

        // Kiểm tra seal mới có status ACTIVE không
        if (!SealEnum.ACTIVE.name().equals(newSeal.getStatus())) {
            log.error("❌ Seal status validation failed - Actual: '{}', Expected: '{}'", 
                    newSeal.getStatus(), 
                    SealEnum.ACTIVE.name());
            throw new IllegalStateException("New seal must have ACTIVE status");
        }

        // Kiểm tra seal mới có thuộc cùng vehicle assignment không
        if (!newSeal.getVehicleAssignment().getId().equals(issue.getVehicleAssignmentEntity().getId())) {
            throw new IllegalStateException("New seal must belong to the same vehicle assignment");
        }

        // Update issue
        issue.setNewSeal(newSeal);
        issue.setStaff(staff);
        issue.setStatus(IssueEnum.IN_PROGRESS.name());

        IssueEntity updated = issueEntityService.save(issue);

        // Convert sang response
        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(updated);

        // 📢 Broadcast status change
        
        issueWebSocketService.broadcastIssueStatusChange(response);

        // 📢 Send notification to driver
        if (issue.getVehicleAssignmentEntity() != null) {
            var driver1 = issue.getVehicleAssignmentEntity().getDriver1();
            if (driver1 != null) {
                String driverId = driver1.getId().toString();
                String driverUserId = driver1.getUser().getId().toString();
                log.info("🔔 Sending SEAL_ASSIGNMENT notification:");
                log.info("   Driver ID (CORRECT for WebSocket): {}", driverId);
                log.info("   Driver User ID (DO NOT USE): {}", driverUserId);
                log.info("   Topic: /topic/driver/{}/notifications", driverId);
                log.info("   Old Seal: {}", issue.getOldSeal().getSealCode());
                log.info("   New Seal: {}", newSeal.getSealCode());
                log.info("   Staff: {}", staff.getFullName());
                
                issueWebSocketService.sendSealAssignmentNotification(
                    driverId,
                    response,
                    staff.getFullName(),
                    newSeal.getSealCode(),
                    issue.getOldSeal().getSealCode()
                );
                
                log.info("✅ Seal assignment notification sent successfully");
            } else {
                log.warn("⚠️ Cannot send notification: driver1 is null");
            }
        } else {
            log.warn("⚠️ Cannot send notification: vehicle assignment is null");
        }
        
        // 📧 Send persistent notification to CUSTOMER about seal replacement
        try {
            var vehicleAssignment = issue.getVehicleAssignmentEntity();
            if (vehicleAssignment != null) {
                var orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(vehicleAssignment);
                if (orderDetails != null && !orderDetails.isEmpty()) {
                    var order = orderDetails.get(0).getOrderEntity();
                    var customer = order.getSender();
                    
                    // Use new NotificationBuilder with enhanced seal change information
                    var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildSealReplaced(
                        customer.getId(),
                        order.getOrderCode(),
                        capstone_project.service.services.notification.NotificationBuilder.generateIssueCode(issue.getId()),
                        issue.getOldSeal().getSealCode(),
                        newSeal.getSealCode(),
                        staff.getFullName(),
                        "Seal " + issue.getOldSeal().getSealCode() + " đã được tháo, seal " + newSeal.getSealCode() + " đã được gán. Lý do: " + issue.getDescription(),
                        order.getId(),
                        issue.getId(),
                        vehicleAssignment.getId()
                    );
                    
                    notificationService.createNotification(notificationRequest);
                    log.info("📧 Customer notification created for seal replacement using NotificationBuilder");
                    
                    // Send detailed email notification to customer
                    try {
                        emailNotificationService.sendSealReplacementEmail(
                            customer.getUser().getEmail(),
                            order.getOrderCode(),
                            issue.getOldSeal().getSealCode(),
                            newSeal.getSealCode(),
                            issue.getDescription(),
                            staff.getFullName()
                        );
                        log.info("📧 Seal replacement email sent to customer: {}", customer.getUser().getEmail());
                    } catch (Exception e) {
                        log.error("❌ Failed to send seal replacement email to {}: {}", customer.getUser().getEmail(), e.getMessage(), e);
                        // Don't fail the main flow if email fails
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to create customer notification for seal replacement", e);
            // Don't fail the main flow if notification fails
        }

        return response;
    }

    @Override
    @Transactional
    public GetBasicIssueResponse confirmNewSeal(ConfirmNewSealRequest request) {

        // Tìm Issue
        IssueEntity issue = issueEntityService.findEntityById(request.issueId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.issueId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Kiểm tra issue đang ở trạng thái IN_PROGRESS
        if (!IssueEnum.IN_PROGRESS.name().equals(issue.getStatus())) {
            throw new IllegalStateException("Only IN_PROGRESS seal issues can be confirmed");
        }

        // Kiểm tra đã có seal mới được assign chưa
        if (issue.getNewSeal() == null) {
            throw new IllegalStateException("No new seal has been assigned yet");
        }

        // Update seal statuses
        SealEntity oldSeal = issue.getOldSeal();
        SealEntity newSeal = issue.getNewSeal();

        // Old seal: REMOVED
        oldSeal.setStatus(SealEnum.REMOVED.name());
        sealEntityService.save(oldSeal);

        // New seal: IN_USE + Upload image to Cloudinary
        newSeal.setStatus(SealEnum.IN_USE.name());
        
        // Upload seal image to Cloudinary instead of storing base64
        try {
            // Extract base64 data from "data:image/jpeg;base64,..." format
            String base64Data = request.newSealAttachedImage();
            if (base64Data.startsWith("data:image/")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
            var uploadResult = cloudinaryService.uploadFile(
                imageBytes, 
                "seal_" + newSeal.getSealCode() + "_" + UUID.randomUUID(), 
                "seal_attachments"
            );
            String imageUrl = uploadResult.get("secure_url").toString();
            newSeal.setSealAttachedImage(imageUrl);
            
        } catch (Exception e) {
            log.error("❌ Failed to upload seal image to Cloudinary: {}", e.getMessage(), e);
            // Fallback: store base64 if upload fails
            newSeal.setSealAttachedImage(request.newSealAttachedImage());
        }
        
        newSeal.setSealDate(java.time.LocalDateTime.now());
        sealEntityService.save(newSeal);

        // Update issue
        // Store the same URL in IssueEntity for consistency
        try {
            String base64Data = request.newSealAttachedImage();
            if (base64Data.startsWith("data:image/")) {
                base64Data = base64Data.substring(base64Data.indexOf(",") + 1);
            }
            
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64Data);
            var uploadResult = cloudinaryService.uploadFile(
                imageBytes, 
                "seal_issue_" + issue.getId() + "_" + UUID.randomUUID(), 
                "seal_issue_attachments"
            );
            String imageUrl = uploadResult.get("secure_url").toString();
            issue.setNewSealAttachedImage(imageUrl);
            
        } catch (Exception e) {
            log.error("❌ Failed to upload issue seal image to Cloudinary: {}", e.getMessage(), e);
            // Fallback: store base64 if upload fails
            issue.setNewSealAttachedImage(request.newSealAttachedImage());
        }
        
        issue.setNewSealConfirmedAt(java.time.LocalDateTime.now());
        issue.setStatus(IssueEnum.RESOLVED.name());
        issue.setResolvedAt(java.time.LocalDateTime.now());

        // ✅ CRITICAL: Restore order detail statuses from JSON map
        // Format: {"orderDetailId1":"STATUS1","orderDetailId2":"STATUS2"}
        // This correctly restores each OrderDetail to its original status (supports combined reports)
        var vehicleAssignment = issue.getVehicleAssignmentEntity();
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(vehicleAssignment);
        String tripStatusAtReport = issue.getTripStatusAtReport();

        if (tripStatusAtReport != null && !tripStatusAtReport.isEmpty()) {
            UUID vehicleAssignmentId = vehicleAssignment != null ? vehicleAssignment.getId() : null;
            
            try {
                // Parse JSON map: {"uuid1":"STATUS1","uuid2":"STATUS2"}
                if (tripStatusAtReport.startsWith("{") && tripStatusAtReport.endsWith("}")) {
                    log.info("🔄 Restoring statuses from JSON: {}", tripStatusAtReport);
                    
                    // Simple JSON parsing (avoiding dependencies)
                    String jsonContent = tripStatusAtReport.substring(1, tripStatusAtReport.length() - 1); // Remove { }
                    String[] entries = jsonContent.split(",");
                    
                    java.util.Map<String, String> statusMap = new java.util.HashMap<>();
                    for (String entry : entries) {
                        String[] parts = entry.split(":");
                        if (parts.length == 2) {
                            String id = parts[0].replace("\"", "").trim();
                            String status = parts[1].replace("\"", "").trim();
                            statusMap.put(id, status);
                        }
                    }
                    
                    // Restore each OrderDetail to its original status
                    for (OrderDetailEntity orderDetail : orderDetails) {
                        String savedStatus = statusMap.get(orderDetail.getId().toString());
                        if (savedStatus != null) {
                            try {
                                OrderDetailStatusEnum restoredStatusEnum = OrderDetailStatusEnum.valueOf(savedStatus);
                                updateOrderDetailStatusWithNotification(
                                    orderDetail,
                                    restoredStatusEnum,
                                    vehicleAssignmentId
                                );
                                log.info("✅ Restored OrderDetail {} to status {}", orderDetail.getId(), savedStatus);
                            } catch (IllegalArgumentException e) {
                                log.warn("⚠️ Invalid status '{}' for OrderDetail {}", savedStatus, orderDetail.getId());
                            }
                        }
                    }
                } else {
                    // Fallback: Old format (single status for all - backward compatibility)
                    log.warn("⚠️ Old format detected, restoring all to: {}", tripStatusAtReport);
                    OrderDetailStatusEnum restoredStatusEnum = OrderDetailStatusEnum.valueOf(tripStatusAtReport.trim());
                    for (OrderDetailEntity orderDetail : orderDetails) {
                        updateOrderDetailStatusWithNotification(
                            orderDetail,
                            restoredStatusEnum,
                            vehicleAssignmentId
                        );
                    }
                }
            } catch (Exception e) {
                log.error("❌ Failed to restore statuses: {}", e.getMessage(), e);
            }
        }
        
        // ✅ CRITICAL FIX: Trigger Order status aggregation after restoring OrderDetails
        // This ensures Order status is updated based on ALL OrderDetails after restoration
        // Handles case: All OrderDetails were IN_TROUBLES → Order IN_TROUBLES
        //               After seal replacement → Order status should reflect restored statuses
        if (!orderDetails.isEmpty()) {
            UUID orderId = orderDetails.get(0).getOrderEntity().getId();
            log.info("🔄 Triggering Order status update after OrderDetail restoration for Order {}", orderId);
            try {
                orderDetailStatusService.triggerOrderStatusUpdate(orderId);
                log.info("✅ Order status updated successfully after seal replacement");
            } catch (Exception e) {
                log.error("❌ Failed to update Order status after seal replacement: {}", e.getMessage(), e);
            }
        }

        IssueEntity updated = issueEntityService.save(issue);

        // Convert sang response
        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(updated);

        // 📢 Broadcast resolution
        
        issueWebSocketService.broadcastIssueStatusChange(response);

        // 📲 Send confirmation message to staff who assigned the seal
        if (issue.getStaff() != null && issue.getVehicleAssignmentEntity() != null) {
            var driver1 = issue.getVehicleAssignmentEntity().getDriver1();
            if (driver1 != null) {
                String staffId = issue.getStaff().getId().toString();
                String driverName = driver1.getUser() != null ? 
                                  driver1.getUser().getFullName() : "Driver";
                String newSealCode = newSeal.getSealCode();
                String oldSealCode = oldSeal.getSealCode();
                String sealImageUrl = newSeal.getSealAttachedImage();
                String oldSealImageUrl = issue.getSealRemovalImage();
                String vehicleAssignmentId = issue.getVehicleAssignmentEntity().getId().toString();
                
                // Create journey code from vehicle assignment tracking code
                String journeyCode = issue.getVehicleAssignmentEntity().getTrackingCode() != null ?
                        issue.getVehicleAssignmentEntity().getTrackingCode() :
                        "Chuyến #" + vehicleAssignmentId.substring(0, 8);

                issueWebSocketService.sendSealConfirmationMessageToStaff(
                    staffId,
                    driverName,
                    newSealCode,
                    oldSealCode,
                    sealImageUrl,
                    oldSealImageUrl,
                    vehicleAssignmentId,
                    journeyCode
                );
            }
        }
        
        // 📧 Send persistent notification to CUSTOMER about seal replacement completion
        try {
            if (issue.getVehicleAssignmentEntity() != null) {
                var notificationOrderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(issue.getVehicleAssignmentEntity());
                if (notificationOrderDetails != null && !notificationOrderDetails.isEmpty()) {
                    var order = notificationOrderDetails.get(0).getOrderEntity();
                    var customer = order.getSender();
                    var driver = issue.getVehicleAssignmentEntity().getDriver1();
                    
                    // Use new NotificationBuilder
                    var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildSealReplacementCompleted(
                        customer.getId(),
                        order.getOrderCode(),
                        capstone_project.service.services.notification.NotificationBuilder.generateIssueCode(issue.getId()),
                        oldSeal.getSealCode(),
                        newSeal.getSealCode(),
                        driver != null && driver.getUser() != null ? driver.getUser().getFullName() : "Tài xế",
                        newSeal.getSealAttachedImage(),
                        issue.getSealRemovalImage(),
                        order.getId(),
                        issue.getId(),
                        issue.getVehicleAssignmentEntity().getId()
                    );
                    
                    notificationService.createNotification(notificationRequest);
                    log.info("📧 Customer notification created for seal replacement confirmation using NotificationBuilder");
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to create customer notification for seal replacement confirmation", e);
            // Don't fail the main flow if notification fails
        }

        return response;
    }

    @Override
    public capstone_project.dtos.response.order.seal.GetSealResponse getInUseSealByVehicleAssignment(UUID vehicleAssignmentId) {

        // Tìm vehicle assignment
        var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy vehicle assignment với ID: " + vehicleAssignmentId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Find IN_USE seal
        List<SealEntity> allSeals = sealEntityService.findAllByVehicleAssignment(vehicleAssignment);
        SealEntity activeSeal = allSeals.stream()
                .filter(seal -> SealEnum.IN_USE.name().equals(seal.getStatus()))
                .findFirst()
                .orElse(null);
        
        return activeSeal != null ? sealMapper.toGetSealResponse(activeSeal) : null;
    }
    
    @Override
    public List<capstone_project.dtos.response.order.seal.GetSealResponse> getActiveSealsByVehicleAssignment(UUID vehicleAssignmentId) {
        
        // Tìm vehicle assignment
        var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy vehicle assignment với ID: " + vehicleAssignmentId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Lấy tất cả seals của vehicle assignment này
        List<SealEntity> allSeals = sealEntityService.findAllByVehicleAssignment(vehicleAssignment);
        
        // Filter chỉ lấy seals có status ACTIVE
        List<SealEntity> activeSeals = allSeals.stream()
                .filter(seal -> SealEnum.ACTIVE.name().equals(seal.getStatus()))
                .toList();

        // Convert to response
        return activeSeals.stream()
                .map(sealMapper::toGetSealResponse)
                .toList();
    }
    
    // Get active seals by tracking code (fallback mechanism)
    @Override
    public List<capstone_project.dtos.response.order.seal.GetSealResponse> getActiveSealsByTrackingCode(String trackingCode) {
        
        // Tìm vehicle assignment theo tracking code
        var vehicleAssignment = vehicleAssignmentEntityService.findByTrackingCode(trackingCode)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy vehicle assignment với tracking code: " + trackingCode,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Reuse existing logic
        return getActiveSealsByVehicleAssignment(vehicleAssignment.getId());
    }

    @Override
    public List<GetBasicIssueResponse> getPendingSealReplacementsByVehicleAssignment(UUID vehicleAssignmentId) {

        // Tìm vehicle assignment
        var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy vehicle assignment với ID: " + vehicleAssignmentId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Lấy tất cả issues rồi filter theo vehicle assignment và các điều kiện khác
        List<IssueEntity> allIssues = issueEntityService.findAllSortedByReportedAtDesc();

        // Debug: Print all issues info
        for (IssueEntity issue : allIssues) {
            
        }
        
        // Filter chỉ lấy issues:
        // - Cùng vehicle assignment
        // - Status IN_PROGRESS (đã được staff gán seal mới)
        // - Category SEAL_REPLACEMENT
        // - Có newSeal (đã được staff gán)
        // - Chưa có newSealConfirmedAt (chưa được driver xác nhận)
        List<IssueEntity> pendingReplacements = allIssues.stream()
                .filter(issue -> {
                    boolean matchesVA = vehicleAssignment.equals(issue.getVehicleAssignmentEntity());
                    
                    return matchesVA;
                })
                .filter(issue -> {
                    boolean matchesStatus = IssueEnum.IN_PROGRESS.name().equals(issue.getStatus());
                    
                    return matchesStatus;
                })
                .filter(issue -> {
                    String category = issue.getIssueTypeEntity() != null ? 
                            issue.getIssueTypeEntity().getIssueCategory() : "NULL";
                    boolean matchesCategory = IssueCategoryEnum.SEAL_REPLACEMENT.name().equals(category);
                    
                    return matchesCategory;
                })
                .filter(issue -> {
                    boolean hasNewSeal = issue.getNewSeal() != null;
                    
                    return hasNewSeal;
                })
                .filter(issue -> {
                    boolean notConfirmed = issue.getNewSealConfirmedAt() == null;
                    
                    return notConfirmed;
                })
                .toList();

        // Convert to response
        return pendingReplacements.stream()
                .map(issueMapper::toIssueBasicResponse)
                .toList();
    }

    @Override
    @Transactional
    public GetBasicIssueResponse reportDamageIssue(ReportDamageIssueRequest request) {

        // Lấy VehicleAssignment
        var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(request.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Lấy IssueType (DAMAGE category)
        var issueType = issueTypeEntityService.findEntityById(request.issueTypeId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + request.issueTypeId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Validate IssueType has DAMAGE category
        if (!IssueCategoryEnum.DAMAGE.name().equals(issueType.getIssueCategory())) {
            throw new IllegalStateException("Issue type must have DAMAGE category");
        }

        // ✅ CRITICAL: Pre-fetch selected order details to build JSON status map
        List<OrderDetailEntity> selectedOrderDetails = new java.util.ArrayList<>();
        if (request.orderDetailIds() != null && !request.orderDetailIds().isEmpty()) {
            for (String trackingCode : request.orderDetailIds()) {
                try {
                    OrderDetailEntity orderDetail = orderDetailEntityService.findByTrackingCode(trackingCode)
                            .orElseThrow(() -> new NotFoundException(
                                    ErrorEnum.ORDER_DETAIL_NOT_FOUND.getMessage() + trackingCode,
                                    ErrorEnum.ORDER_DETAIL_NOT_FOUND.getErrorCode()
                            ));
                    selectedOrderDetails.add(orderDetail);
                } catch (Exception e) {
                    log.error("❌ Error fetching order detail {}: {}", trackingCode, e.getMessage());
                    throw new RuntimeException("Failed to fetch order detail: " + trackingCode, e);
                }
            }
        }

        // ✅ CRITICAL: Build JSON status map from selected order details
        String tripStatusAtReport = null;
        if (!selectedOrderDetails.isEmpty()) {
            StringBuilder jsonBuilder = new StringBuilder("{");
            for (int i = 0; i < selectedOrderDetails.size(); i++) {
                OrderDetailEntity od = selectedOrderDetails.get(i);
                if (i > 0) jsonBuilder.append(",");
                jsonBuilder.append("\"").append(od.getId()).append("\":\"").append(od.getStatus()).append("\"");
            }
            jsonBuilder.append("}");
            tripStatusAtReport = jsonBuilder.toString();
            log.info("💾 Saved trip status for combined damage report (JSON): {}", tripStatusAtReport);
        }

        // Tạo Issue với JSON status map
        IssueEntity issue = IssueEntity.builder()
                .description(request.description())
                .locationLatitude(request.locationLatitude() != null ? 
                                 java.math.BigDecimal.valueOf(request.locationLatitude()) : null)
                .locationLongitude(request.locationLongitude() != null ? 
                                  java.math.BigDecimal.valueOf(request.locationLongitude()) : null)
                .status(IssueEnum.OPEN.name())
                .reportedAt(java.time.LocalDateTime.now())
                .tripStatusAtReport(tripStatusAtReport)
                .vehicleAssignmentEntity(vehicleAssignment)
                .staff(null)
                .issueTypeEntity(issueType)
                .build();

        // Lưu issue
        IssueEntity saved = issueEntityService.save(issue);
        
        // Create damage notification
        createIssueNotifications(saved);

        // NOW update order details and link to issue
        if (!selectedOrderDetails.isEmpty()) {
            for (OrderDetailEntity orderDetail : selectedOrderDetails) {
                try {
                    // Link order detail to issue and mark as IN_TROUBLES
                    orderDetail.setIssueEntity(saved);
                    updateOrderDetailStatusWithNotification(
                        orderDetail,
                        OrderDetailStatusEnum.IN_TROUBLES,
                        vehicleAssignment.getId()
                    );
                } catch (Exception e) {
                    log.error("❌ Error updating order detail {}: {}", orderDetail.getId(), e.getMessage());
                    throw new RuntimeException("Failed to update order detail: " + orderDetail.getId(), e);
                }
            }
            
            // Set orderDetails to issue for bidirectional relationship
            saved.setOrderDetails(selectedOrderDetails);
            issueEntityService.save(saved);

            // ❌ REMOVED: Auto-update remaining order details to DELIVERED
            // ✅ NEW FLOW: Driver must upload delivery photos for packages without issues
            // This ensures we have photo confirmation for all successfully delivered packages
            // Mobile app will prompt driver to take delivery photos after reporting damage
            
            var affectedOrderDetailIds = selectedOrderDetails.stream()
                    .map(OrderDetailEntity::getId)
                    .collect(java.util.stream.Collectors.toSet());
            
            var allOrderDetails = orderDetailEntityService.findByVehicleAssignmentId(request.vehicleAssignmentId());
            var remainingOrderDetails = allOrderDetails.stream()
                    .filter(od -> !affectedOrderDetailIds.contains(od.getId()))
                    .filter(od -> "ONGOING_DELIVERED".equals(od.getStatus()) || "ON_DELIVERED".equals(od.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
            
            log.info("📦 Damage report: {} packages marked IN_TROUBLES, {} packages require delivery photo confirmation",
                    selectedOrderDetails.size(), remainingOrderDetails.size());
        }

        // Upload damage images to Cloudinary and save to issue_images table
        if (request.damageImages() != null && !request.damageImages().isEmpty()) {
            for (MultipartFile imageFile : request.damageImages()) {
                try {
                    
                    // Don't add .jpg extension - Cloudinary will add it based on the file
                    String imageUrl = cloudinaryService.uploadFile(imageFile.getBytes(), 
                            "damage_" + System.currentTimeMillis(), 
                            "damage_reports").get("secure_url").toString();
                    
                    // Check for double .jpg extension
                    if (imageUrl.contains(".jpg.jpg")) {
                        log.warn("⚠️ Double .jpg extension detected in URL: {}", imageUrl);
                    } else {
                        
                    }

                    // Save to issue_images table
                    capstone_project.entity.issue.IssueImageEntity issueImage = 
                        capstone_project.entity.issue.IssueImageEntity.builder()
                            .imageUrl(imageUrl)
                            .description("Ảnh hàng hóa bị hư hại")
                            .issueEntity(saved)
                            .build();
                    
                    issueImageEntityService.save(issueImage);

                } catch (Exception e) {
                    log.error("❌ Error uploading damage image: {}", e.getMessage());
                    throw new RuntimeException("Failed to upload damage image", e);
                }
            }
        }

        // 🔐 SEAL REMOVAL: Removed auto-removal logic
        // Driver will manually report seal removal when needed through the app
        // This prevents conflicts with return goods flow where seal removal report is required AFTER customer payment

        // Fetch full issue with all nested objects (vehicle, drivers, images)
        GetBasicIssueResponse response = getBasicIssue(saved.getId());

        // 📢 Broadcast damage issue to staff
        issueWebSocketService.broadcastNewIssue(response);
        
        // 📧 Send persistent notification to STAFF about new damage issue
        try {
            // Find staff users (simplified - get first staff for now)
            // TODO: Implement proper staff selection logic (by role, department, etc.)
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            if (!staffUsers.isEmpty()) {
                var driver = vehicleAssignment.getDriver1();
                var order = !selectedOrderDetails.isEmpty() ? selectedOrderDetails.get(0).getOrderEntity() : null;
                
                for (var staff : staffUsers) {
                    var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildNewIssueReported(
                        staff.getId(),
                        capstone_project.service.services.notification.NotificationBuilder.generateIssueCode(saved.getId()),
                        issueType.getIssueTypeName(),
                        order != null ? order.getOrderCode() : "N/A",
                        driver != null && driver.getUser() != null ? driver.getUser().getFullName() : "Tài xế",
                        driver != null && driver.getUser() != null ? driver.getUser().getPhoneNumber() : "N/A",
                        vehicleAssignment.getVehicleEntity() != null ? vehicleAssignment.getVehicleEntity().getLicensePlateNumber() : "N/A",
                        order != null ? order.getId() : null,
                        saved.getId(),
                        vehicleAssignment.getId()
                    );
                    
                    notificationService.createNotification(notificationRequest);
                }
                log.info("📧 Staff notifications created for new damage issue");
            }
        } catch (Exception e) {
            log.error("❌ Failed to create staff notifications for damage issue", e);
            // Don't fail the main flow if notification fails
        }
        
        // 📧 Send persistent notification to CUSTOMER about package damage
        try {
            if (!selectedOrderDetails.isEmpty()) {
                var order = selectedOrderDetails.get(0).getOrderEntity();
                var customer = order.getSender();

                if (customer == null || customer.getUser() == null) {
                    log.warn("⚠️ Skip customer damage notification - customer or user is null for order {}", 
                        order.getId());
                } else {
                    // 🔧 Get tracking code for trip info
                    String vehicleAssignmentTrackingCode = vehicleAssignment != null 
                        ? vehicleAssignment.getTrackingCode() : null;
                    
                    var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildPackageDamaged(
                        customer.getUser().getId(),
                        order.getOrderCode(),
                        selectedOrderDetails.size(),
                        orderDetailEntityService.findByVehicleAssignmentId(vehicleAssignment.getId()).size(),
                        selectedOrderDetails,
                        vehicleAssignmentTrackingCode,
                        order.getId(),
                        saved.getId(),
                        selectedOrderDetails.stream().map(OrderDetailEntity::getId).collect(java.util.stream.Collectors.toList()),
                        vehicleAssignment.getId()
                    );
                    
                    notificationService.createNotification(notificationRequest);
                    log.info("📧 Customer notification created for package damage");
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to create customer notification for package damage", e);
            // Don't fail the main flow if notification fails
        }

        return response;
    }

    @Override
    @Transactional
    public GetBasicIssueResponse reportPenaltyIssue(
            UUID vehicleAssignmentId,
            UUID issueTypeId,
            String violationType,
            MultipartFile violationImage,
            Double locationLatitude,
            Double locationLongitude) {

        // Get VehicleAssignment
        var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Vehicle Assignment: " + vehicleAssignmentId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Get IssueType (PENALTY category)
        var issueType = issueTypeEntityService.findEntityById(issueTypeId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Issue Type: " + issueTypeId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Validate IssueType has PENALTY category
        if (!IssueCategoryEnum.PENALTY.name().equals(issueType.getIssueCategory())) {
            throw new IllegalStateException("Issue type must have PENALTY category");
        }

        // Create Issue

        IssueEntity issue = IssueEntity.builder()
                .description("Vi phạm giao thông: " + violationType)
                .locationLatitude(locationLatitude != null ? 
                                 java.math.BigDecimal.valueOf(locationLatitude) : null)
                .locationLongitude(locationLongitude != null ? 
                                  java.math.BigDecimal.valueOf(locationLongitude) : null)
                .status(IssueEnum.OPEN.name())
                .reportedAt(java.time.LocalDateTime.now())
                .tripStatusAtReport(null) // Penalty doesn't affect order details
                .vehicleAssignmentEntity(vehicleAssignment)
                .staff(null)
                .issueTypeEntity(issueType)
                .build();

        // Save issue
        IssueEntity saved = issueEntityService.save(issue);

        String imageUrl = null;

        if (violationImage != null && !violationImage.isEmpty()) {
            try {

                imageUrl = cloudinaryService.uploadFile(
                        violationImage.getBytes(),
                        "penalty_" + saved.getId() + "_" + System.currentTimeMillis(),
                        "penalties/traffic-violations"
                ).get("secure_url").toString();

                IssueImageEntity imageEntity = IssueImageEntity.builder()
                        .imageUrl(imageUrl)
                        .description("Biên bản vi phạm giao thông")
                        .issueEntity(saved)
                        .build();

                issueImageEntityService.save(imageEntity);

            } catch (Exception e) {
                log.error("❌ Error uploading penalty image: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to upload penalty violation image", e);
            }
        }

        PenaltyHistoryEntity penaltyHistory = PenaltyHistoryEntity.builder()
                .violationType(violationType)
                .penaltyDate(LocalDate.now())
                .trafficViolationRecordImageUrl(imageUrl)
                .issueBy(vehicleAssignment.getDriver1())
                .vehicleAssignmentEntity(vehicleAssignment)
                .build();

        penaltyHistoryEntityService.save(penaltyHistory);

        // Fetch full issue with all nested objects
        GetBasicIssueResponse response = getBasicIssue(saved.getId());

        // Broadcast penalty issue to staff
        issueWebSocketService.broadcastNewIssue(response);
        
        // 📧 Send persistent notification to STAFF about new penalty issue
        try {
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            if (!staffUsers.isEmpty()) {
                var driver = vehicleAssignment.getDriver1();
                var orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(vehicleAssignment);
                var order = !orderDetails.isEmpty() ? orderDetails.get(0).getOrderEntity() : null;
                
                for (var staff : staffUsers) {
                    var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildNewIssueReported(
                        staff.getId(),
                        capstone_project.service.services.notification.NotificationBuilder.generateIssueCode(saved.getId()),
                        issueType.getIssueTypeName(),
                        order != null ? order.getOrderCode() : "N/A",
                        driver != null && driver.getUser() != null ? driver.getUser().getFullName() : "Tài xế",
                        driver != null && driver.getUser() != null ? driver.getUser().getPhoneNumber() : "N/A",
                        vehicleAssignment.getVehicleEntity() != null ? vehicleAssignment.getVehicleEntity().getLicensePlateNumber() : "N/A",
                        order != null ? order.getId() : null,
                        saved.getId(),
                        vehicleAssignment.getId()
                    );
                    
                    notificationService.createNotification(notificationRequest);
                }
                log.info("📧 Staff notifications created for new penalty issue");
            }
        } catch (Exception e) {
            log.error("❌ Failed to create staff notifications for penalty issue", e);
            // Don't fail the main flow if notification fails
        }

        return response;
    }

    // ===== ORDER_REJECTION flow implementations =====

    @Override
    @Transactional
    public GetBasicIssueResponse reportOrderRejection(capstone_project.dtos.request.issue.ReportOrderRejectionRequest request) {

        // Get Vehicle Assignment
        var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(request.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Vehicle Assignment: " + request.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Auto-find ORDER_REJECTION issue type (server determines issue type)
        var issueType = issueTypeEntityService.findAll().stream()
                .filter(it -> IssueCategoryEnum.ORDER_REJECTION.name().equals(it.getIssueCategory()))
                .filter(IssueTypeEntity::getIsActive)
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "No active ORDER_REJECTION issue type found",
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Get and validate selected order details (orderDetailIds are tracking codes)
        List<OrderDetailEntity> selectedOrderDetails = new java.util.ArrayList<>();
        for (String trackingCode : request.orderDetailIds()) {
            OrderDetailEntity orderDetail = orderDetailEntityService.findByTrackingCode(trackingCode)
                    .orElseThrow(() -> new NotFoundException(
                            "Order detail not found with tracking code: " + trackingCode,
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));
            
            // Validate order detail belongs to this vehicle assignment
            if (!orderDetail.getVehicleAssignmentEntity().getId().equals(vehicleAssignment.getId())) {
                throw new BadRequestException(
                        "Order detail " + trackingCode + " does not belong to vehicle assignment " + vehicleAssignment.getId(),
                        ErrorEnum.INVALID.getErrorCode()
                );
            }
            
            selectedOrderDetails.add(orderDetail);
        }

        // Auto-generate description based on number of packages
        String autoDescription = String.format(
                "Người nhận từ chối nhận %d kiện hàng. Tracking codes: %s",
                selectedOrderDetails.size(),
                selectedOrderDetails.stream()
                        .map(od -> od.getOrderEntity().getOrderCode())
                        .collect(java.util.stream.Collectors.joining(", "))
        );

        // ✅ CRITICAL: Save statuses as JSON map for SELECTED order details only
        // Format: {"orderDetailId1":"STATUS1","orderDetailId2":"STATUS2"}
        // Only save statuses for rejected packages, not all packages in vehicle assignment
        String tripStatusAtReport = null;
        if (!selectedOrderDetails.isEmpty()) {
            StringBuilder jsonBuilder = new StringBuilder("{");
            for (int i = 0; i < selectedOrderDetails.size(); i++) {
                OrderDetailEntity od = selectedOrderDetails.get(i);
                if (i > 0) jsonBuilder.append(",");
                jsonBuilder.append("\"").append(od.getId()).append("\":\"").append(od.getStatus()).append("\"");
            }
            jsonBuilder.append("}");
            tripStatusAtReport = jsonBuilder.toString();
            log.info("💾 Saved trip status for order rejection (JSON, selected only): {}", tripStatusAtReport);
        }

        // Update ONLY selected order details to IN_TROUBLES with WebSocket notification
        selectedOrderDetails.forEach(orderDetail -> {
            updateOrderDetailStatusWithNotification(
                orderDetail,
                OrderDetailStatusEnum.IN_TROUBLES,
                vehicleAssignment.getId()
            );
        });

        // Create Issue
        IssueEntity issue = IssueEntity.builder()
                .description(autoDescription)
                .locationLatitude(request.locationLatitude() != null ? java.math.BigDecimal.valueOf(request.locationLatitude()) : null)
                .locationLongitude(request.locationLongitude() != null ? java.math.BigDecimal.valueOf(request.locationLongitude()) : null)
                .status(IssueEnum.OPEN.name())
                .reportedAt(java.time.LocalDateTime.now())
                .tripStatusAtReport(tripStatusAtReport)
                .vehicleAssignmentEntity(vehicleAssignment)
                .staff(null)
                .issueTypeEntity(issueType)
                .build();

        // Save issue first to get ID
        IssueEntity saved = issueEntityService.save(issue);

        // Link selected order details to this issue (bidirectional)
        selectedOrderDetails.forEach(orderDetail -> {
            orderDetail.setIssueEntity(saved);
            orderDetailEntityService.save(orderDetail);
        });
        
        // Set orderDetails to issue for bidirectional relationship
        saved.setOrderDetails(selectedOrderDetails);
        issueEntityService.save(saved);

        // Fetch full issue with all nested objects
        GetBasicIssueResponse response = getBasicIssue(saved.getId());

        // Broadcast to staff
        issueWebSocketService.broadcastNewIssue(response);
        
        // 📧 Send persistent notification to STAFF about new order rejection issue
        try {
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            if (!staffUsers.isEmpty()) {
                var driver = vehicleAssignment.getDriver1();
                var order = !selectedOrderDetails.isEmpty() ? selectedOrderDetails.get(0).getOrderEntity() : null;
                
                for (var staff : staffUsers) {
                    var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildNewIssueReported(
                        staff.getId(),
                        capstone_project.service.services.notification.NotificationBuilder.generateIssueCode(saved.getId()),
                        issueType.getIssueTypeName(),
                        order != null ? order.getOrderCode() : "N/A",
                        driver != null && driver.getUser() != null ? driver.getUser().getFullName() : "Tài xế",
                        driver != null && driver.getUser() != null ? driver.getUser().getPhoneNumber() : "N/A",
                        vehicleAssignment.getVehicleEntity() != null ? vehicleAssignment.getVehicleEntity().getLicensePlateNumber() : "N/A",
                        order != null ? order.getId() : null,
                        saved.getId(),
                        vehicleAssignment.getId()
                    );
                    
                    notificationService.createNotification(notificationRequest);
                }
                log.info("📧 Staff notifications created for new order rejection issue");
            }
        } catch (Exception e) {
            log.error("❌ Failed to create staff notifications for order rejection issue", e);
            // Don't fail the main flow if notification fails
        }
        
        return response;
    }

    @Override
    public capstone_project.dtos.response.issue.ReturnShippingFeeResponse calculateReturnShippingFee(UUID issueId) {

        // Use the overloaded method with null distance to use default calculation
        return calculateReturnShippingFee(issueId, null);
    }
    
    @Override
    public capstone_project.dtos.response.issue.ReturnShippingFeeResponse calculateReturnShippingFee(UUID issueId, java.math.BigDecimal actualDistanceKm) {

        // Get Issue
        IssueEntity issue = issueEntityService.findEntityById(issueId)
                .orElseThrow(() -> new NotFoundException(
                        "Issue not found: " + issueId, ErrorEnum.NOT_FOUND.getErrorCode()));

        // Validate issue type
        if (!IssueCategoryEnum.ORDER_REJECTION.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            throw new IllegalStateException("Issue is not ORDER_REJECTION type");
        }

        // Get ONLY the selected order details for return (linked to this issue)
        List<OrderDetailEntity> selectedOrderDetails = issue.getOrderDetails() != null 
                ? issue.getOrderDetails() 
                : java.util.Collections.emptyList();

        if (selectedOrderDetails.isEmpty()) {
            throw new IllegalStateException("No order details selected for return in this issue");
        }

        // Get order from first order detail (all should belong to same order)
        var order = selectedOrderDetails.get(0).getOrderEntity();

        // Get vehicle type from issue's vehicle assignment
        String vehicleType = null;
        if (issue.getVehicleAssignmentEntity() != null 
            && issue.getVehicleAssignmentEntity().getVehicleEntity() != null 
            && issue.getVehicleAssignmentEntity().getVehicleEntity().getVehicleTypeEntity() != null) {
            vehicleType = issue.getVehicleAssignmentEntity().getVehicleEntity().getVehicleTypeEntity().getVehicleTypeName();
            
        } else {
            log.warn("⚠️ No vehicle type found in assignment, using default");
            vehicleType = "car"; // Default fallback
        }

        // Use actual distance from client if provided, otherwise calculate from addresses
        java.math.BigDecimal distanceKm;
        if (actualDistanceKm != null && actualDistanceKm.compareTo(java.math.BigDecimal.ZERO) > 0) {
            distanceKm = actualDistanceKm;
            
        } else {
            // Calculate distance from delivery address back to pickup address (return route) with vehicle type
            distanceKm = contractService.calculateDistanceKm(
                    order.getDeliveryAddress(), 
                    order.getPickupAddress(),
                    vehicleType
            );
            
        }

        // Get contract to calculate pricing
        var contract = contractEntityService.getContractByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("No contract found for order: " + order.getId()));

        // Build vehicle count map using SizeRuleEntity IDs from ContractRules
        // Match each selected OrderDetail to its ContractRule to get the correct SizeRule
        java.util.Map<UUID, Integer> vehicleCountMap = new java.util.HashMap<>();
        
        // Get all contract rules for this contract
        var contractRules = contractRuleEntityService.findContractRuleEntityByContractEntityId(contract.getId());
        
        for (var orderDetail : selectedOrderDetails) {
            // Find the ContractRule that contains this orderDetail
            var contractRule = contractRules.stream()
                    .filter(cr -> cr.getOrderDetails() != null 
                            && cr.getOrderDetails().stream()
                                .anyMatch(od -> od.getId().equals(orderDetail.getId())))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "No contract rule found for order detail: " + orderDetail.getId()));
            
            // Get the SizeRule ID from the ContractRule
            UUID sizeRuleId = contractRule.getSizeRuleEntity().getId();
            
            // Increment count for this SizeRule
            vehicleCountMap.put(sizeRuleId, vehicleCountMap.getOrDefault(sizeRuleId, 0) + 1);
        }

        // Calculate total weight of selected packages for logging
        double totalWeight = selectedOrderDetails.stream()
                .mapToDouble(od -> od.getWeightBaseUnit() != null ? od.getWeightBaseUnit().doubleValue() : 0.0)
                .sum();

        // Calculate return shipping fee using contract pricing logic
        PriceCalculationResponse priceResponse =
                contractService.calculateTotalPrice(contract, distanceKm, vehicleCountMap);

        // Determine final fee (adjusted fee if set by staff, otherwise calculated fee)
        java.math.BigDecimal finalFee = issue.getAdjustedReturnFee() != null 
                ? issue.getAdjustedReturnFee() 
                : priceResponse.getTotalPrice();

        return new capstone_project.dtos.response.issue.ReturnShippingFeeResponse(
                issue.getId(),
                priceResponse.getTotalPrice(),
                issue.getAdjustedReturnFee(),
                finalFee,
                distanceKm,
                priceResponse
        );
    }

    @Override
    @Transactional
    public capstone_project.dtos.response.issue.OrderRejectionDetailResponse processOrderRejection(
            capstone_project.dtos.request.issue.ProcessOrderRejectionRequest request
    ) {

        // Log first segment details for debugging
        if (request.routeSegments() != null && !request.routeSegments().isEmpty()) {
            var firstSegment = request.routeSegments().get(0);
            
        }

        // Get Issue
        IssueEntity issue = issueEntityService.findEntityById(request.issueId())
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Issue: " + request.issueId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Validate issue type
        if (!IssueCategoryEnum.ORDER_REJECTION.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            throw new IllegalStateException("Issue is not ORDER_REJECTION type");
        }

        // Calculate return shipping fee
        capstone_project.dtos.response.issue.ReturnShippingFeeResponse feeResponse = calculateReturnShippingFee(request.issueId());

        // Update adjusted fee if provided
        if (request.adjustedReturnFee() != null) {
            issue.setAdjustedReturnFee(request.adjustedReturnFee());
            
        }

        // Set return shipping fee (use calculated fee)
        issue.setReturnShippingFee(feeResponse.calculatedFee());

        // Get final fee for transaction
        java.math.BigDecimal finalFee = issue.getAdjustedReturnFee() != null 
                ? issue.getAdjustedReturnFee() 
                : issue.getReturnShippingFee();

        // Get order and contract for reference (needed for journey creation)
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(
                issue.getVehicleAssignmentEntity()
        );
        var order = orderDetails.get(0).getOrderEntity();
        var contract = contractEntityService.getContractByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("No contract found for order: " + order.getId()));

        // NOTE: Payment link will be created separately via POST /api/payos-transaction/{contractId}/return-shipping
        // This avoids nested transaction issues and follows the same pattern as deposit/full payment

        // ========== MERGE OLD JOURNEY + RETURN SEGMENTS ==========
        // Step 1: ✅ Lấy journey history ACTIVE/COMPLETED mới nhất để copy các segments đã đi
        capstone_project.entity.order.order.JourneyHistoryEntity oldJourney = 
                journeyHistoryEntityService.findLatestActiveJourney(
                        issue.getVehicleAssignmentEntity().getId()
                ).orElse(null);
        
        if (oldJourney != null) {
            
        } else {
            log.warn("⚠️ No ACTIVE/COMPLETED journey found for vehicle assignment {}", 
                    issue.getVehicleAssignmentEntity().getId());
        }
        
        // Step 2: Create new journey for return route with merged segments
        capstone_project.entity.order.order.JourneyHistoryEntity returnJourney = 
                capstone_project.entity.order.order.JourneyHistoryEntity.builder()
                .journeyName("Return Journey for " + issue.getVehicleAssignmentEntity().getTrackingCode())
                .journeyType("RETURN")
                .status("INACTIVE") // Will be activated when customer pays
                .reasonForReroute("Order rejection by recipient")
                .totalTollFee(request.totalTollFee())
                .totalTollCount(request.totalTollCount())
                .vehicleAssignment(issue.getVehicleAssignmentEntity())
                .build();

        // Step 3: Copy old segments first (excluding the last delivery → carrier segment)
        List<capstone_project.entity.order.order.JourneySegmentEntity> allSegments = new java.util.ArrayList<>();
        int nextSegmentOrder = 1;
        
        if (oldJourney != null && oldJourney.getJourneySegments() != null) {
            List<capstone_project.entity.order.order.JourneySegmentEntity> oldSegments = 
                    oldJourney.getJourneySegments();

            // Copy all segments EXCEPT the last one (delivery → carrier)
            // The last segment will be replaced by return segments
            for (int i = 0; i < oldSegments.size() - 1; i++) {
                capstone_project.entity.order.order.JourneySegmentEntity oldSeg = oldSegments.get(i);
                
                capstone_project.entity.order.order.JourneySegmentEntity copiedSegment = 
                        capstone_project.entity.order.order.JourneySegmentEntity.builder()
                        .segmentOrder(nextSegmentOrder++)
                        .startPointName(oldSeg.getStartPointName())
                        .endPointName(oldSeg.getEndPointName())
                        .startLatitude(oldSeg.getStartLatitude())
                        .startLongitude(oldSeg.getStartLongitude())
                        .endLatitude(oldSeg.getEndLatitude())
                        .endLongitude(oldSeg.getEndLongitude())
                        .distanceKilometers(oldSeg.getDistanceKilometers())
                        .status(oldSeg.getStatus()) // Keep original status (COMPLETED/ACTIVE)
                        .estimatedTollFee(oldSeg.getEstimatedTollFee())
                        .pathCoordinatesJson(oldSeg.getPathCoordinatesJson())
                        .tollDetailsJson(oldSeg.getTollDetailsJson())
                        .journeyHistory(returnJourney)
                        .build();
                
                allSegments.add(copiedSegment);
            }

        } else {
            log.warn("⚠️ No old journey found, creating return journey with only return segments");
        }
        
        // Step 4: Add return segments (delivery → pickup → carrier)

        for (capstone_project.dtos.request.order.RouteSegmentInfo segmentInfo : request.routeSegments()) {

            capstone_project.entity.order.order.JourneySegmentEntity segment = 
                    capstone_project.entity.order.order.JourneySegmentEntity.builder()
                    .segmentOrder(nextSegmentOrder++) // Continue from old segments
                    .startPointName(segmentInfo.startPointName())
                    .endPointName(segmentInfo.endPointName())
                    .startLatitude(segmentInfo.startLatitude())
                    .startLongitude(segmentInfo.startLongitude())
                    .endLatitude(segmentInfo.endLatitude())
                    .endLongitude(segmentInfo.endLongitude())
                    .distanceKilometers(segmentInfo.distanceKilometers())
                    .estimatedTollFee(segmentInfo.estimatedTollFee() != null ? segmentInfo.estimatedTollFee().longValue() : null) // Convert BigDecimal to Long
                    .status("PENDING")
                    .journeyHistory(returnJourney)
                    .build();

            // Store path coordinates and toll details JSON (already in JSON format)
            segment.setPathCoordinatesJson(segmentInfo.pathCoordinatesJson());
            
            // Convert toll details list to JSON if not empty
            if (segmentInfo.tollDetails() != null && !segmentInfo.tollDetails().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    segment.setTollDetailsJson(objectMapper.writeValueAsString(segmentInfo.tollDetails()));
                } catch (Exception e) {
                    log.warn("Failed to serialize toll details: {}", e.getMessage());
                }
            }

            allSegments.add(segment);
        }

        returnJourney.setJourneySegments(allSegments);
        returnJourney = journeyHistoryEntityService.save(returnJourney);

        issue.setReturnJourney(returnJourney);

        // Set payment deadline from configuration (driver cannot wait too long)
        issue.setPaymentDeadline(java.time.LocalDateTime.now().plusMinutes(returnPaymentDeadlineMinutes));

        // Update issue status to IN_PROGRESS
        issue.setStatus(IssueEnum.IN_PROGRESS.name());

        // Save issue
        issue = issueEntityService.save(issue);

        // ⏰ CRITICAL: Schedule real-time timeout check
        // This ensures driver gets notification within seconds of deadline expiring
        // Safety net scheduler will catch any missed cases due to server restart
        paymentTimeoutSchedulerService.scheduleTimeoutCheck(issue);

        // Broadcast WebSocket notification for issue status change
        // This ensures customer order detail page receives update and refetches
        try {
            GetBasicIssueResponse issueResponse = issueMapper.toIssueBasicResponse(issue);
            issueWebSocketService.broadcastIssueStatusChange(issueResponse);
            
        } catch (Exception e) {
            log.error("❌ Failed to broadcast issue status change: {}", e.getMessage());
            // Don't fail the whole operation if broadcast fails
        }

        // NOTE: Transaction will be created when customer clicks "Pay" button
        // This follows the same pattern as deposit/full payment:
        // 1. Customer sees issue with fee amount
        // 2. Customer clicks "Pay" button  
        // 3. Frontend calls POST /api/payos-transaction/{contractId}/return-shipping
        // 4. Backend creates transaction + PayOS link
        // 5. Frontend redirects to PayOS checkout URL

        // 📧 Send persistent notification to CUSTOMER about order rejection
        // Moved from reportOrderRejection so that content uses finalized return route & pricing
        try {
            // Use ONLY order details linked to this issue as return packages
            List<OrderDetailEntity> returnOrderDetails = issue.getOrderDetails() != null
                    ? issue.getOrderDetails()
                    : java.util.Collections.emptyList();

            if (!returnOrderDetails.isEmpty()) {
                var firstOrderDetail = returnOrderDetails.get(0);
                var customerOrder = firstOrderDetail.getOrderEntity();
                var customer = customerOrder != null ? customerOrder.getSender() : null;

                if (customer == null || customer.getUser() == null) {
                    log.warn("⚠️ Skip customer order rejection notification - customer or user is null for order {}",
                            customerOrder != null ? customerOrder.getId() : null);
                } else {
                    // Build delivery location string (same logic as before)
                    String deliveryLocation;
                    if (customerOrder.getDeliveryAddress() != null) {
                        var address = customerOrder.getDeliveryAddress();
                        deliveryLocation = String.format("%s, %s, %s",
                                address.getStreet() != null ? address.getStreet() : "",
                                address.getWard() != null ? address.getWard() : "",
                                address.getProvince() != null ? address.getProvince() : "").trim();
                        if (deliveryLocation.startsWith(", ")) {
                            deliveryLocation = deliveryLocation.substring(2);
                        }
                        if (deliveryLocation.isEmpty()) {
                            deliveryLocation = "Địa chỉ nhận hàng";
                        }
                    } else {
                        deliveryLocation = "Địa chỉ nhận hàng";
                    }

                    // Total packages in this vehicle assignment (for x/y display)
                    int totalPackageCountInAssignment = orderDetails.size();

                    String vehicleAssignmentTrackingCode = issue.getVehicleAssignmentEntity() != null
                            ? issue.getVehicleAssignmentEntity().getTrackingCode()
                            : null;

                    var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildOrderRejectedByReceiver(
                            customer.getUser().getId(),
                            customerOrder.getOrderCode(),
                            returnOrderDetails.size(),
                            totalPackageCountInAssignment,
                            deliveryLocation,
                            returnOrderDetails,
                            vehicleAssignmentTrackingCode,
                            customerOrder.getId(),
                            issue.getId(),
                            returnOrderDetails.stream().map(OrderDetailEntity::getId).collect(java.util.stream.Collectors.toList()),
                            issue.getVehicleAssignmentEntity() != null ? issue.getVehicleAssignmentEntity().getId() : null,
                            returnPaymentDeadlineMinutes
                    );

                    notificationService.createNotification(notificationRequest);
                    log.info("📧 Customer notification created for order rejection (staff processed)");
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to create customer notification for order rejection after staff processing", e);
            // Don't fail the main flow if notification fails
        }

        // Return detail response
        return getOrderRejectionDetail(issue.getId());
    }

    @Override
    public capstone_project.dtos.response.issue.OrderRejectionDetailResponse getOrderRejectionDetail(UUID issueId) {

        // Get Issue with all relations
        IssueEntity issue = issueEntityService.findByIdWithDetails(issueId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Issue: " + issueId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Validate issue type
        if (!IssueCategoryEnum.ORDER_REJECTION.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            throw new IllegalStateException("Issue is not ORDER_REJECTION type");
        }

        // Get ONLY order details linked to this issue (not all order details in vehicle assignment)
        List<OrderDetailEntity> orderDetails = issue.getOrderDetails();
        
        if (orderDetails == null || orderDetails.isEmpty()) {
            throw new IllegalStateException("Issue has no linked order details");
        }
        
        // Debug log
//        log.info("📦 Issue {} has {} linked order details: {}",
//                issueId,
//                orderDetails.size(),
//                orderDetails.stream()
//                    .map(OrderDetailEntity::getTrackingCode)
//                    .collect(java.util.stream.Collectors.joining(", ")));

        // Map to response
        List<capstone_project.dtos.response.issue.OrderDetailForIssueResponse> affectedOrderDetails = 
                orderDetails.stream()
                .map(od -> {
                    String orderId = od.getOrderEntity() != null ? 
                                    od.getOrderEntity().getId().toString() : null;
                    return new capstone_project.dtos.response.issue.OrderDetailForIssueResponse(
                            od.getTrackingCode(),
                            od.getDescription(),
                            od.getWeightBaseUnit(),
                            od.getUnit(),
                            orderId
                    );
                })
                .collect(java.util.stream.Collectors.toList());

        // Get customer info from order
        var order = orderDetails.get(0).getOrderEntity();
        var customer = order.getSender();
        capstone_project.dtos.response.user.CustomerInfoResponse customerInfo = 
                new capstone_project.dtos.response.user.CustomerInfoResponse(
                        customer.getId(),
                        customer.getRepresentativeName(),
                        customer.getUser() != null ? customer.getUser().getEmail() : null,
                        customer.getRepresentativePhone(),
                        customer.getCompanyName()
                );

        // Get contract ID for payment link creation
        var contract = contractEntityService.getContractByOrderId(order.getId())
                .orElse(null);
        UUID contractId = contract != null ? contract.getId() : null;

        // Map transaction if exists - find by issueId
        capstone_project.dtos.response.order.transaction.TransactionResponse transactionResponse = null;
        var transactionOpt = transactionEntityService.findAll().stream()
                .filter(tx -> issue.getId().equals(tx.getIssueId()))
                .findFirst();
        
        if (transactionOpt.isPresent()) {
            var tx = transactionOpt.get();
            // Convert gateway order code from String to Long
            Long gatewayOrderCode = null;
            if (tx.getGatewayOrderCode() != null) {
                try {
                    gatewayOrderCode = Long.parseLong(tx.getGatewayOrderCode());
                } catch (NumberFormatException e) {
                    log.warn("Cannot convert gateway order code to Long: {}", tx.getGatewayOrderCode());
                }
            }
            
            transactionResponse = new capstone_project.dtos.response.order.transaction.TransactionResponse(
                    tx.getId().toString(),
                    tx.getPaymentProvider(),
                    null, // orderCode - not available in return transaction
                    tx.getAmount(),
                    tx.getCurrencyCode(),
                    tx.getGatewayResponse(),
                    gatewayOrderCode,
                    tx.getStatus(),
                    tx.getPaymentDate(),
                    tx.getTransactionType(), // transactionType
                    null // contractId not needed here
            );
        }

        // Map return journey if exists
        capstone_project.dtos.response.order.JourneyHistoryResponse journeyResponse = null;
        if (issue.getReturnJourney() != null) {
            var journey = issue.getReturnJourney();
            // Simple journey response without full segment details
            // Calculate total distance from segments (already in km)
            Double totalDistance = journey.getJourneySegments() != null
                    ? journey.getJourneySegments().stream()
                            .mapToDouble(seg -> seg.getDistanceKilometers() != null ? seg.getDistanceKilometers().doubleValue() : 0.0)
                            .sum()
                    : 0.0;
            
            journeyResponse = new capstone_project.dtos.response.order.JourneyHistoryResponse(
                    journey.getId(),
                    journey.getJourneyName(),
                    journey.getJourneyType(),
                    journey.getStatus(),
                    journey.getTotalTollFee(),
                    journey.getTotalTollCount(),
                    totalDistance,
                    journey.getReasonForReroute(),
                    journey.getVehicleAssignment() != null ? journey.getVehicleAssignment().getId() : null,
                    null, // segments - will be null for now, can be expanded if needed
                    journey.getCreatedAt(),
                    journey.getModifiedAt()
            );
        }

        // Calculate fees
        java.math.BigDecimal finalFee = issue.getAdjustedReturnFee() != null 
                ? issue.getAdjustedReturnFee() 
                : issue.getReturnShippingFee();

        return new capstone_project.dtos.response.issue.OrderRejectionDetailResponse(
                issue.getId(),
                issue.getId().toString(), // issueCode - can be enhanced
                issue.getDescription(),
                issue.getStatus(),
                issue.getReportedAt(),
                issue.getResolvedAt(),
                customerInfo,
                contractId, // Contract ID for creating payment link via separate API call
                issue.getReturnShippingFee(),
                issue.getAdjustedReturnFee(),
                finalFee,
                transactionResponse,
                issue.getPaymentDeadline(),
                journeyResponse,
                affectedOrderDetails,
                // Get return delivery images from issueImages
                issue.getIssueImages() != null
                        ? issue.getIssueImages().stream()
                                .filter(img -> "RETURN_DELIVERY".equals(img.getDescription()))
                                .map(IssueImageEntity::getImageUrl)
                                .collect(java.util.stream.Collectors.toList())
                        : java.util.Collections.emptyList()
        );
    }

    @Override
    @Transactional
    public GetBasicIssueResponse confirmReturnDelivery(
            List<org.springframework.web.multipart.MultipartFile> files,
            UUID issueId
    ) throws java.io.IOException {

        // Get Issue
        IssueEntity issue = issueEntityService.findEntityById(issueId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Issue: " + issueId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        // Validate issue type
        if (!IssueCategoryEnum.ORDER_REJECTION.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            throw new IllegalStateException("Issue is not ORDER_REJECTION type");
        }

        // Validate issue is in IN_PROGRESS status
        if (!IssueEnum.IN_PROGRESS.name().equals(issue.getStatus())) {
            throw new IllegalStateException("Issue must be IN_PROGRESS to confirm return delivery");
        }

        // Upload images to Cloudinary and save to IssueImageEntity
        if (files != null && !files.isEmpty()) {
            for (org.springframework.web.multipart.MultipartFile file : files) {
                // Upload to Cloudinary
                var uploadResult = cloudinaryService.uploadFile(
                        file.getBytes(),
                        UUID.randomUUID().toString(),
                        "return_delivery"
                );
                String imageUrl = uploadResult.get("secure_url").toString();
                
                // Save to IssueImageEntity
                IssueImageEntity imageEntity = IssueImageEntity.builder()
                        .imageUrl(imageUrl)
                        .description("RETURN_DELIVERY")
                        .issueEntity(issue)
                        .build();
                issueImageEntityService.save(imageEntity);

            }
        }

        // Update ONLY selected order details to RETURNED status
        List<OrderDetailEntity> selectedOrderDetails = issue.getOrderDetails() != null 
                ? issue.getOrderDetails() 
                : java.util.Collections.emptyList();
        
        if (selectedOrderDetails.isEmpty()) {
            throw new IllegalStateException("No order details found in issue for return");
        }
        
        UUID vehicleAssignmentId = issue.getVehicleAssignmentEntity() != null ? 
                issue.getVehicleAssignmentEntity().getId() : null;
        
        selectedOrderDetails.forEach(orderDetail -> {
            updateOrderDetailStatusWithNotification(
                orderDetail,
                OrderDetailStatusEnum.RETURNED,
                vehicleAssignmentId
            );
        });

        // ✅ CRITICAL FIX: Use OrderDetailStatusService to auto-update Order status
        // This ensures correct priority logic (COMPENSATION > IN_TROUBLES > CANCELLED > RETURNING/RETURNED > DELIVERED)
        // NEVER manually calculate Order status - delegate to the centralized service
        var vehicleAssignment = issue.getVehicleAssignmentEntity();
        if (vehicleAssignment != null) {
            Optional<OrderEntity> orderOpt = orderEntityService.findVehicleAssignmentOrder(vehicleAssignment.getId());
            if (orderOpt.isPresent()) {
                OrderEntity order = orderOpt.get();
                
                // Trigger auto-update using centralized service
                // This will apply correct priority logic:
                // - COMPENSATION (highest priority if ANY package compensated)
                // - IN_TROUBLES (if ANY package has active issue)
                // - CANCELLED (if ALL packages cancelled)
                // - RETURNING/RETURNED (if ALL packages in return flow)
                // - DELIVERED (only if ALL packages delivered)
                orderDetailStatusService.triggerOrderStatusUpdate(order.getId());
                
                log.info("✅ Order status auto-updated after return delivery confirmation for Order: {}", order.getId());
            }
        }

        // Mark issue as RESOLVED
        issue.setStatus(IssueEnum.RESOLVED.name());
        issue.setResolvedAt(java.time.LocalDateTime.now());

        // Save issue
        issue = issueEntityService.save(issue);

        // 📧 Send notification to CUSTOMER about return completion (at least 1 package returned)
        try {
            List<OrderDetailEntity> returnedDetails = issue.getOrderDetails() != null
                    ? issue.getOrderDetails()
                    : java.util.Collections.emptyList();

            if (!returnedDetails.isEmpty()) {
                var order = returnedDetails.get(0).getOrderEntity();
                var customer = order.getSender();

                if (customer == null || customer.getUser() == null) {
                    log.warn("⚠️ Skip customer return completed notification - customer or user is null for order {}", 
                            order.getId());
                } else {
                    // Determine total packages of the order for context
                    List<OrderDetailEntity> allOrderDetails = orderDetailEntityService
                            .findOrderDetailEntitiesByOrderEntityId(order.getId());

                    int returnedCount = returnedDetails.size();
                    int totalPackageCount = allOrderDetails.size();

                    String pickupLocation = order.getPickupAddress() != null
                            ? order.getPickupAddress().getStreet()
                            : "điểm lấy hàng";

                    // Check if all packages of the order are now RETURNED
                    boolean allPackagesReturned = allOrderDetails.stream()
                            .allMatch(od -> OrderDetailStatusEnum.RETURNED.name().equals(od.getStatus()));

                    var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildReturnCompleted(
                            customer.getUser().getId(),
                            order.getOrderCode(),
                            returnedCount,
                            totalPackageCount,
                            pickupLocation,
                            returnedDetails,
                            order.getId(),
                            returnedDetails.stream().map(OrderDetailEntity::getId).collect(java.util.stream.Collectors.toList()),
                            vehicleAssignmentId,
                            allPackagesReturned
                    );

                    notificationService.createNotification(notificationRequest);
                    log.info("📧 Customer notification created for return completed ({} / {} packages)",
                            returnedCount, totalPackageCount);
                    
                    // Staff notification: Return completed (NO EMAIL)
                    try {
                        CreateNotificationRequest staffReturnTemplate = capstone_project.service.services.notification.NotificationBuilder.buildStaffReturnCompleted(
                            null, // Will be set for each staff user
                            order.getOrderCode(),
                            customer.getUser().getFullName(),
                            returnedCount,
                            totalPackageCount,
                            returnedDetails,
                            order.getId(),
                            returnedDetails.stream().map(OrderDetailEntity::getId).collect(java.util.stream.Collectors.toList())
                        );
                        sendStaffNotification(staffReturnTemplate);
                        log.info("📧 Staff notification created for return completed ({} / {} packages)",
                                returnedCount, totalPackageCount);
                    } catch (Exception e) {
                        log.error("❌ Failed to create staff notification for return completed", e);
                        // Don't fail the main flow if notification fails
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Failed to create customer notification for return completed", e);
            // Don't fail the main flow if notification fails
        }

        return getBasicIssue(issue.getId());
    }

    @Override
    @Transactional
    public capstone_project.dtos.response.order.transaction.TransactionResponse createReturnPaymentTransaction(UUID issueId) {

        // Get Issue
        IssueEntity issue = issueEntityService.findEntityById(issueId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage() + " Issue: " + issueId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Validate issue type
        if (!IssueCategoryEnum.ORDER_REJECTION.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            throw new IllegalStateException("Issue is not ORDER_REJECTION type");
        }
        
        // Validate issue status
        if (!IssueEnum.IN_PROGRESS.name().equals(issue.getStatus())) {
            throw new IllegalStateException("Issue must be IN_PROGRESS to create payment");
        }
        
        // Get final fee
        java.math.BigDecimal finalFee = issue.getAdjustedReturnFee() != null 
                ? issue.getAdjustedReturnFee() 
                : issue.getReturnShippingFee();
        
        if (finalFee == null || finalFee.compareTo(java.math.BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Return shipping fee not set or invalid");
        }
        
        // Get contract ID from order
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(
                issue.getVehicleAssignmentEntity()
        );
        if (orderDetails.isEmpty()) {
            throw new IllegalStateException("No order details found for vehicle assignment");
        }
        
        var order = orderDetails.get(0).getOrderEntity();
        var contract = contractEntityService.getContractByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("No contract found for order: " + order.getId()));

        // Create transaction using PayOS service
        capstone_project.dtos.response.order.transaction.TransactionResponse transactionResponse = 
                payOSTransactionService.createReturnShippingTransaction(
                        contract.getId(), 
                        finalFee, 
                        issue.getId()
                );

        // Transaction already has issueId set, no need to link back
        return transactionResponse;
    }
    
    // ===== REROUTE flow implementations =====
    
    @Override
    @Transactional
    public GetBasicIssueResponse reportRerouteIssue(
            capstone_project.dtos.request.issue.ReportRerouteRequest request,
            java.util.List<org.springframework.web.multipart.MultipartFile> files
    ) throws java.io.IOException {
        
        // Validate vehicle assignment exists
        VehicleAssignmentEntity vehicleAssignment = vehicleAssignmentEntityService.findEntityById(
                request.vehicleAssignmentId()
        ).orElseThrow(() -> new NotFoundException(
                "Vehicle assignment not found: " + request.vehicleAssignmentId(),
                ErrorEnum.NOT_FOUND.getErrorCode()
        ));
        
        // Validate issue type exists and is REROUTE category
        IssueTypeEntity issueType = issueTypeEntityService.findEntityById(request.issueTypeId())
                .orElseThrow(() -> new NotFoundException(
                        "Issue type not found: " + request.issueTypeId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        if (!IssueCategoryEnum.REROUTE.name().equals(issueType.getIssueCategory())) {
            throw new IllegalStateException("Issue type must be REROUTE category");
        }
        
        // Validate affected segment exists by finding it from active journey
        capstone_project.entity.order.order.JourneyHistoryEntity activeJourney = 
                journeyHistoryEntityService.findLatestActiveJourney(request.vehicleAssignmentId())
                .orElseThrow(() -> new NotFoundException(
                        "No active journey found for vehicle assignment: " + request.vehicleAssignmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        capstone_project.entity.order.order.JourneySegmentEntity affectedSegment = 
                activeJourney.getJourneySegments().stream()
                .filter(seg -> seg.getId().equals(request.affectedSegmentId()))
                .findFirst()
                .orElseThrow(() -> new NotFoundException(
                        "Journey segment not found in active journey: " + request.affectedSegmentId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Get current order detail status for tripStatusAtReport
        List<OrderDetailEntity> orderDetails = orderDetailEntityService
                .findByVehicleAssignmentEntity(vehicleAssignment);
        
        // Build trip status map
        java.util.Map<String, String> statusMap = new java.util.HashMap<>();
        for (OrderDetailEntity od : orderDetails) {
            statusMap.put(od.getId().toString(), od.getStatus());
        }
        
        String tripStatusJson;
        try {
            com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
                    new com.fasterxml.jackson.databind.ObjectMapper();
            tripStatusJson = objectMapper.writeValueAsString(statusMap);
        } catch (Exception e) {
            log.error("Failed to serialize trip status: {}", e.getMessage());
            tripStatusJson = "{}";
        }
        
        // Create issue entity
        IssueEntity issue = IssueEntity.builder()
                .description(request.description())
                .locationLatitude(request.locationLatitude())
                .locationLongitude(request.locationLongitude())
                .status(IssueEnum.OPEN.name())
                .reportedAt(java.time.LocalDateTime.now())
                .vehicleAssignmentEntity(vehicleAssignment)
                .issueTypeEntity(issueType)
                .tripStatusAtReport(tripStatusJson)
                .affectedSegment(affectedSegment) // Set affected segment
                .build();
        
        issue = issueEntityService.save(issue);
        
        // Upload optional images if provided
        if (files != null && !files.isEmpty()) {
            for (org.springframework.web.multipart.MultipartFile file : files) {
                var uploadResult = cloudinaryService.uploadFile(
                        file.getBytes(),
                        UUID.randomUUID().toString(),
                        "reroute_issue"
                );
                String imageUrl = uploadResult.get("secure_url").toString();
                
                IssueImageEntity imageEntity = IssueImageEntity.builder()
                        .imageUrl(imageUrl)
                        .description("REROUTE_ISSUE")
                        .issueEntity(issue)
                        .build();
                issueImageEntityService.save(imageEntity);
            }
        }
        
        log.info("✅ REROUTE issue reported: {} for segment {} at location ({}, {})",
                issue.getId(), affectedSegment.getId(),
                request.locationLatitude(), request.locationLongitude());
        
        // ✅ CRITICAL: Update all OrderDetails in VehicleAssignment to IN_TROUBLES
        try {
            orderDetailStatusService.updateOrderDetailStatusByAssignment(
                    vehicleAssignment.getId(),
                    OrderDetailStatusEnum.IN_TROUBLES
            );
            log.info("✅ Updated OrderDetails to IN_TROUBLES for vehicle assignment: {}", 
                    vehicleAssignment.getId());
        } catch (Exception e) {
            log.error("❌ Failed to update OrderDetails to IN_TROUBLES: {}", e.getMessage());
        }
        
        // 📢 Broadcast new reroute issue to all staff clients
        try {
            GetBasicIssueResponse issueResponse = issueMapper.toIssueBasicResponse(issue);
            issueWebSocketService.broadcastNewIssue(issueResponse);
        } catch (Exception e) {
            log.error("❌ Failed to broadcast new issue: {}", e.getMessage());
        }
        
        // 📧 Send persistent notification to STAFF about new reroute issue
        try {
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            if (!staffUsers.isEmpty()) {
                var driver = vehicleAssignment.getDriver1();
                var order = !orderDetails.isEmpty() ? orderDetails.get(0).getOrderEntity() : null;
                
                for (var staff : staffUsers) {
                    var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildNewIssueReported(
                        staff.getId(),
                        capstone_project.service.services.notification.NotificationBuilder.generateIssueCode(issue.getId()),
                        issueType.getIssueTypeName(),
                        order != null ? order.getOrderCode() : "N/A",
                        driver != null && driver.getUser() != null ? driver.getUser().getFullName() : "Tài xế",
                        driver != null && driver.getUser() != null ? driver.getUser().getPhoneNumber() : "N/A",
                        vehicleAssignment.getVehicleEntity() != null ? vehicleAssignment.getVehicleEntity().getLicensePlateNumber() : "N/A",
                        order != null ? order.getId() : null,
                        issue.getId(),
                        vehicleAssignment.getId()
                    );
                    
                    notificationService.createNotification(notificationRequest);
                }
                log.info("📧 Staff notifications created for new reroute issue");
            }
        } catch (Exception e) {
            log.error("❌ Failed to create staff notifications for reroute issue", e);
            // Don't fail the main flow if notification fails
        }
        
        // 📧 Send persistent notification to CUSTOMER about route delay
        try {
            if (!orderDetails.isEmpty()) {
                var order = orderDetails.get(0).getOrderEntity();
                var customer = order.getSender();
                
                var notificationRequest = capstone_project.service.services.notification.NotificationBuilder.buildNewIssueReported(
                    customer.getId(),
                    capstone_project.service.services.notification.NotificationBuilder.generateIssueCode(issue.getId()),
                    issueType.getIssueTypeName(),
                    order.getOrderCode(),
                    "Tài xế",
                    "N/A",
                    vehicleAssignment.getVehicleEntity() != null ? vehicleAssignment.getVehicleEntity().getLicensePlateNumber() : "N/A",
                    order.getId(),
                    issue.getId(),
                    vehicleAssignment.getId()
                );
                
                notificationService.createNotification(notificationRequest);
                log.info("📧 Customer notification created for route delay");
            }
        } catch (Exception e) {
            log.error("❌ Failed to create customer notification for route delay", e);
            // Don't fail the main flow if notification fails
        }
        
        return getBasicIssue(issue.getId());
    }
    
    @Override
    @Transactional
    public capstone_project.dtos.response.issue.RerouteDetailResponse processReroute(
            capstone_project.dtos.request.issue.ProcessRerouteRequest request
    ) {
        
        // Get Issue
        IssueEntity issue = issueEntityService.findEntityById(request.issueId())
                .orElseThrow(() -> new NotFoundException(
                        "Issue not found: " + request.issueId(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Validate issue type
        if (!IssueCategoryEnum.REROUTE.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            throw new IllegalStateException("Issue is not REROUTE type");
        }
        
        // Validate affected segment exists
        if (issue.getAffectedSegment() == null) {
            throw new IllegalStateException("Issue has no affected segment");
        }
        
        // Get current active journey to copy segments
        capstone_project.entity.order.order.JourneyHistoryEntity oldJourney = 
                journeyHistoryEntityService.findLatestActiveJourney(
                        issue.getVehicleAssignmentEntity().getId()
                ).orElse(null);
        
        if (oldJourney == null) {
            throw new IllegalStateException("No active journey found for vehicle assignment");
        }
        
        log.info("🔄 Processing reroute for issue {} - Affected segment: {} (order: {})",
                issue.getId(),
                issue.getAffectedSegment().getId(),
                issue.getAffectedSegment().getSegmentOrder());
        
        // Create new journey for rerouted path
        capstone_project.entity.order.order.JourneyHistoryEntity reroutedJourney = 
                capstone_project.entity.order.order.JourneyHistoryEntity.builder()
                .journeyName("Rerouted Journey for " + issue.getVehicleAssignmentEntity().getTrackingCode())
                .journeyType("REROUTE")
                .status("ACTIVE") // Immediately active (no payment needed)
                .reasonForReroute(issue.getDescription())
                .totalTollFee(request.totalTollFee())
                .totalTollCount(request.totalTollCount())
                .vehicleAssignment(issue.getVehicleAssignmentEntity())
                .build();
        
        // ✅ CRITICAL: Frontend sends COMPLETE journey with affected segment already replaced
        // Just use request.newRouteSegments directly - no need to copy old segments
        List<capstone_project.entity.order.order.JourneySegmentEntity> allSegments = 
                new java.util.ArrayList<>();
        
        log.info("📦 Building new journey from {} segments sent by frontend", 
                request.newRouteSegments().size());
        
        // Create segments from request (frontend already built complete journey)
        for (capstone_project.dtos.request.order.RouteSegmentInfo newSegInfo : 
                request.newRouteSegments()) {
            
            capstone_project.entity.order.order.JourneySegmentEntity newSegment = 
                    capstone_project.entity.order.order.JourneySegmentEntity.builder()
                    .segmentOrder(newSegInfo.segmentOrder()) // Use order from frontend
                    .startPointName(newSegInfo.startPointName())
                    .endPointName(newSegInfo.endPointName())
                    .startLatitude(newSegInfo.startLatitude())
                    .startLongitude(newSegInfo.startLongitude())
                    .endLatitude(newSegInfo.endLatitude())
                    .endLongitude(newSegInfo.endLongitude())
                    .distanceKilometers(newSegInfo.distanceKilometers())
                    .estimatedTollFee(newSegInfo.estimatedTollFee() != null ? 
                            newSegInfo.estimatedTollFee().longValue() : null)
                    .status("ACTIVE") // All segments in rerouted journey are ACTIVE
                    .pathCoordinatesJson(newSegInfo.pathCoordinatesJson())
                    .journeyHistory(reroutedJourney)
                    .build();
            
            // Convert toll details to JSON
            if (newSegInfo.tollDetails() != null && !newSegInfo.tollDetails().isEmpty()) {
                try {
                    com.fasterxml.jackson.databind.ObjectMapper objectMapper = 
                            new com.fasterxml.jackson.databind.ObjectMapper();
                    newSegment.setTollDetailsJson(
                            objectMapper.writeValueAsString(newSegInfo.tollDetails())
                    );
                } catch (Exception e) {
                    log.warn("Failed to serialize toll details: {}", e.getMessage());
                }
            }
            
            allSegments.add(newSegment);
            
            log.info("✅ Created segment {}: {} → {} ({}km)", 
                    newSegInfo.segmentOrder(),
                    newSegInfo.startPointName(),
                    newSegInfo.endPointName(),
                    newSegInfo.distanceKilometers());
        }
        
        reroutedJourney.setJourneySegments(allSegments);
        reroutedJourney = journeyHistoryEntityService.save(reroutedJourney);
        
        // Set old journey to CANCELLED
        // oldJourney.setStatus("CANCELLED");
        // journeyHistoryEntityService.save(oldJourney);
        
        // Link rerouted journey to issue
        issue.setReroutedJourney(reroutedJourney);
        issue.setStatus(IssueEnum.RESOLVED.name());
        issue.setResolvedAt(java.time.LocalDateTime.now());
        issue = issueEntityService.save(issue);
        
        log.info("✅ Reroute processed: Journey {} created with {} segments",
                reroutedJourney.getId(), allSegments.size());
        
        // ✅ CRITICAL: Restore OrderDetails status from tripStatusAtReport
        String tripStatusAtReport = issue.getTripStatusAtReport();
        List<OrderDetailEntity> orderDetails = orderDetailEntityService
                .findByVehicleAssignmentEntity(issue.getVehicleAssignmentEntity());
        
        if (tripStatusAtReport != null && !tripStatusAtReport.isEmpty()) {
            try {
                // Parse JSON map: {"uuid1":"STATUS1","uuid2":"STATUS2"}
                if (tripStatusAtReport.startsWith("{") && tripStatusAtReport.endsWith("}")) {
                    log.info("🔄 Restoring statuses from JSON (processReroute): {}", tripStatusAtReport);
                    
                    // Simple JSON parsing
                    String jsonContent = tripStatusAtReport.substring(1, tripStatusAtReport.length() - 1);
                    String[] entries = jsonContent.split(",");
                    
                    java.util.Map<String, String> statusMap = new java.util.HashMap<>();
                    for (String entry : entries) {
                        String[] parts = entry.split(":");
                        if (parts.length == 2) {
                            String id = parts[0].replace("\"", "").trim();
                            String status = parts[1].replace("\"", "").trim();
                            statusMap.put(id, status);
                        }
                    }
                    
                    // Restore each OrderDetail to its original status
                    for (OrderDetailEntity orderDetail : orderDetails) {
                        String savedStatus = statusMap.get(orderDetail.getId().toString());
                        if (savedStatus != null) {
                            try {
                                OrderDetailStatusEnum restoredStatusEnum = OrderDetailStatusEnum.valueOf(savedStatus);
                                updateOrderDetailStatusWithNotification(
                                    orderDetail,
                                    restoredStatusEnum,
                                    issue.getVehicleAssignmentEntity().getId()
                                );
                                log.info("✅ Restored OrderDetail {} to status {}", 
                                        orderDetail.getId(), savedStatus);
                            } catch (IllegalArgumentException e) {
                                log.warn("⚠️ Invalid status '{}' for OrderDetail {}", 
                                        savedStatus, orderDetail.getId());
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.error("❌ Failed to restore statuses (processReroute): {}", e.getMessage(), e);
            }
        } else {
            log.warn("⚠️ No tripStatusAtReport found for issue {}, cannot restore statuses", 
                    issue.getId());
        }
        
        // ✅ CRITICAL FIX: Trigger Order status aggregation after restoring OrderDetails
        // This ensures Order status is updated based on ALL OrderDetails after restoration
        // Handles case: All OrderDetails were IN_TROUBLES → Order IN_TROUBLES
        //               After reroute → Order status should reflect restored statuses
        if (!orderDetails.isEmpty()) {
            UUID orderId = orderDetails.get(0).getOrderEntity().getId();
            log.info("🔄 Triggering Order status update after OrderDetail restoration for Order {}", orderId);
            try {
                orderDetailStatusService.triggerOrderStatusUpdate(orderId);
                log.info("✅ Order status updated successfully after reroute");
            } catch (Exception e) {
                log.error("❌ Failed to update Order status after reroute: {}", e.getMessage(), e);
            }
        }
        
        // Send WebSocket notification to driver
        try {
            // Get Order ID through OrderDetail (VehicleAssignment → OrderDetail → Order)
            // orderDetails already fetched above for status restoration
            UUID orderId = orderDetails.isEmpty() ? null : orderDetails.get(0).getOrderEntity().getId();
            
            // ✅ CRITICAL: Use driver ID (not user ID) to match mobile app subscription
            var driver = issue.getVehicleAssignmentEntity().getDriver1();
            UUID driverId = driver.getId();
            UUID userId = driver.getUser() != null ? driver.getUser().getId() : null;
            
            log.info("📢 Sending reroute resolved notification:");
            log.info("   Driver ID: {} (for WebSocket subscription)", driverId);
            log.info("   User ID: {} (DO NOT USE for WebSocket)", userId);
            log.info("   Issue ID: {}", issue.getId());
            log.info("   Order ID: {}", orderId);
            
            issueWebSocketService.sendRerouteResolvedNotification(
                    driverId, // ✅ CORRECT: Driver ID
                    issue.getId(),
                    orderId
            );
        } catch (Exception e) {
            log.error("❌ Failed to send reroute notification to driver: {}", e.getMessage(), e);
        }
        
        // Broadcast issue status change
        try {
            GetBasicIssueResponse issueResponse = issueMapper.toIssueBasicResponse(issue);
            issueWebSocketService.broadcastIssueStatusChange(issueResponse);
        } catch (Exception e) {
            log.error("❌ Failed to broadcast issue status: {}", e.getMessage());
        }
        
        return getRerouteDetail(issue.getId());
    }
    
    @Override
    public capstone_project.dtos.response.issue.RerouteDetailResponse getRerouteDetail(UUID issueId) {
        
        // Get Issue with relations
        IssueEntity issue = issueEntityService.findByIdWithDetails(issueId)
                .orElseThrow(() -> new NotFoundException(
                        "Issue not found: " + issueId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Validate issue type
        if (!IssueCategoryEnum.REROUTE.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            throw new IllegalStateException("Issue is not REROUTE type");
        }
        
        // Get affected segment full info
        capstone_project.entity.order.order.JourneySegmentEntity affectedSegmentEntity = 
                issue.getAffectedSegment();
        
        if (affectedSegmentEntity == null) {
            throw new IllegalStateException("Issue has no affected segment");
        }
        
        // Map affected segment to response
        capstone_project.dtos.response.order.JourneySegmentResponse affectedSegment = 
                new capstone_project.dtos.response.order.JourneySegmentResponse(
                    affectedSegmentEntity.getId(),
                    affectedSegmentEntity.getSegmentOrder(),
                    affectedSegmentEntity.getStartPointName(),
                    affectedSegmentEntity.getEndPointName(),
                    affectedSegmentEntity.getStartLatitude(),
                    affectedSegmentEntity.getStartLongitude(),
                    affectedSegmentEntity.getEndLatitude(),
                    affectedSegmentEntity.getEndLongitude(),
                    affectedSegmentEntity.getDistanceKilometers(),
                    affectedSegmentEntity.getPathCoordinatesJson(),
                    affectedSegmentEntity.getTollDetailsJson(),
                    affectedSegmentEntity.getStatus(),
                    affectedSegmentEntity.getCreatedAt(),
                    affectedSegmentEntity.getModifiedAt()
                );
        
        // Map vehicle assignment with vehicle & driver details
        capstone_project.dtos.response.vehicle.VehicleAssignmentResponse vehicleAssignment = null;
        if (issue.getVehicleAssignmentEntity() != null) {
            vehicleAssignment = vehicleAssignmentMapper.toResponse(issue.getVehicleAssignmentEntity());
        }
        
        // Get latest ACTIVE journey with full segments
        capstone_project.dtos.response.order.JourneyHistoryResponse activeJourney = null;
        if (issue.getVehicleAssignmentEntity() != null) {
            // Query journey histories separately to get full segments
            var journeyHistories = journeyHistoryRepository.findByVehicleAssignment_Id(
                    issue.getVehicleAssignmentEntity().getId());
            
            // Find latest ACTIVE journey
            var activeJourneyEntity = journeyHistories.stream()
                    .filter(j -> "ACTIVE".equals(j.getStatus()))
                    .max((j1, j2) -> j1.getCreatedAt().compareTo(j2.getCreatedAt()))
                    .orElse(null);
            
            if (activeJourneyEntity != null) {
                // Fetch with segments
                var journeyWithSegments = journeyHistoryRepository.findByIdWithSegments(
                        activeJourneyEntity.getId()).orElse(null);
                if (journeyWithSegments != null) {
                    activeJourney = journeyHistoryMapper.toResponse(journeyWithSegments);
                }
            }
        }
        
        // Map rerouted journey if exists
        capstone_project.dtos.response.order.JourneyHistoryResponse reroutedJourney = null;
        if (issue.getReroutedJourney() != null) {
            reroutedJourney = journeyHistoryMapper.toResponse(issue.getReroutedJourney());
        }
        
        // Get issue images
        List<String> issueImages = issueImageEntityService.findByIssueEntity_Id(issueId)
                .stream()
                .map(capstone_project.entity.issue.IssueImageEntity::getImageUrl)
                .collect(java.util.stream.Collectors.toList());
        
        return new capstone_project.dtos.response.issue.RerouteDetailResponse(
                issue.getId(),
                issue.getStatus(),
                issue.getDescription(),
                issue.getReportedAt(),
                issue.getResolvedAt(),
                issue.getLocationLatitude(),
                issue.getLocationLongitude(),
                affectedSegment,
                vehicleAssignment,
                activeJourney,
                reroutedJourney,
                issueImages
        );
    }
    
    /**
     * Helper method to update order detail status and send WebSocket notification
     * Centralizes the logic to avoid code duplication
     * 
     * @param orderDetail The order detail entity to update
     * @param newStatus The new status to set
     * @param vehicleAssignmentId Vehicle assignment ID (can be null)
     */
    private void updateOrderDetailStatusWithNotification(
            OrderDetailEntity orderDetail,
            OrderDetailStatusEnum newStatus,
            UUID vehicleAssignmentId
    ) {
        String oldStatus = orderDetail.getStatus();
        orderDetail.setStatus(newStatus.name());
        orderDetailEntityService.save(orderDetail);

        // Send WebSocket notification
        try {
            OrderEntity order = orderDetail.getOrderEntity();
            if (order != null) {
                orderDetailStatusWebSocketService.sendOrderDetailStatusChange(
                        orderDetail.getId(),
                        orderDetail.getTrackingCode(),
                        order.getId(),
                        order.getOrderCode(),
                        vehicleAssignmentId,
                        oldStatus,
                        newStatus
                );
            }
        } catch (Exception e) {
            log.error("❌ Failed to send WebSocket notification for order detail {}: {}",
                    orderDetail.getTrackingCode(), e.getMessage());
        }
    }
    
    /**
     * Get suggested alternative routes for reroute issue using Vietmap Route V3 API
     * Returns multiple route options for staff to choose from
     * 
     * @param issueId The reroute issue ID
     * @return Vietmap Route V3 response with alternative routes
     */
    @Override
    @Transactional(readOnly = true)
    public capstone_project.dtos.response.vietmap.VietmapRouteV3Response getSuggestedRoutesForReroute(UUID issueId) {
        // 1️⃣ Get issue and validate
        IssueEntity issue = issueEntityService.findByIdWithDetails(issueId)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.ISSUE_NOT_FOUND));
        
        if (!IssueCategoryEnum.REROUTE.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            throw new BadRequestException("Issue is not a REROUTE issue", ErrorEnum.INVALID_REQUEST.getErrorCode());
        }
        
        if (issue.getAffectedSegment() == null) {
            throw new BadRequestException("Affected segment not found for this issue", ErrorEnum.INVALID_REQUEST.getErrorCode());
        }
        
        // 2️⃣ Get 3 points: start, issue location, end
        var affectedSegment = issue.getAffectedSegment();
        
        // Validate coordinates are not null
        if (affectedSegment.getStartLatitude() == null || affectedSegment.getStartLongitude() == null) {
            throw new BadRequestException("Tọa độ điểm bắt đầu không hợp lệ", ErrorEnum.INVALID_REQUEST.getErrorCode());
        }
        if (affectedSegment.getEndLatitude() == null || affectedSegment.getEndLongitude() == null) {
            throw new BadRequestException("Tọa độ điểm kết thúc không hợp lệ", ErrorEnum.INVALID_REQUEST.getErrorCode());
        }
        if (issue.getLocationLatitude() == null || issue.getLocationLongitude() == null) {
            throw new BadRequestException("Tọa độ vị trí sự cố không hợp lệ", ErrorEnum.INVALID_REQUEST.getErrorCode());
        }
        
        // Start point: beginning of affected segment
        String startPoint = String.format("%s,%s", 
                affectedSegment.getStartLatitude(), 
                affectedSegment.getStartLongitude());
        
        // Middle point: issue reported location
        String issuePoint = String.format("%s,%s",
                issue.getLocationLatitude(),
                issue.getLocationLongitude());
        
        // End point: end of affected segment
        String endPoint = String.format("%s,%s",
                affectedSegment.getEndLatitude(),
                affectedSegment.getEndLongitude());
        
        // Validate coordinate ranges (Vietnam approximate bounds)
        validateCoordinates(affectedSegment.getStartLatitude().doubleValue(), affectedSegment.getStartLongitude().doubleValue(), "điểm bắt đầu");
        validateCoordinates(affectedSegment.getEndLatitude().doubleValue(), affectedSegment.getEndLongitude().doubleValue(), "điểm kết thúc");
        validateCoordinates(issue.getLocationLatitude().doubleValue(), issue.getLocationLongitude().doubleValue(), "vị trí sự cố");
        
        // Validate start and end points are not identical (zero-distance edge case)
        if (affectedSegment.getStartLatitude().equals(affectedSegment.getEndLatitude()) && 
            affectedSegment.getStartLongitude().equals(affectedSegment.getEndLongitude())) {
            throw new BadRequestException("Điểm bắt đầu và điểm kết thúc không được trùng nhau", ErrorEnum.INVALID_REQUEST.getErrorCode());
        }
        
        // 3️⃣ Get vehicle capacity from vehicle type (ton → kg)
        var vehicle = issue.getVehicleAssignmentEntity().getVehicleEntity();
        var vehicleType = vehicle.getVehicleTypeEntity();
        
        // Weight limit in tons, convert to kg for API
        Integer capacityKg = vehicleType.getWeightLimitTon() != null
                ? vehicleType.getWeightLimitTon().multiply(new java.math.BigDecimal(1000)).intValue()
                : null;
        
        log.info("🚛 Getting suggested routes for reroute issue {}:", issue.getId());
        log.info("   Start: {} ({})", affectedSegment.getStartPointName(), startPoint);
        log.info("   Issue: Lat {} Lng {} ({})", issue.getLocationLatitude(), issue.getLocationLongitude(), issuePoint);
        log.info("   End: {} ({})", affectedSegment.getEndPointName(), endPoint);
        log.info("   Vehicle: {} - {} (capacity: {} kg)", 
                vehicle.getLicensePlateNumber(),
                vehicleType.getVehicleTypeName(),
                capacityKg);
        
        // 4️⃣ Build Vietmap Route V3 request
        // Let VietmapServiceImpl use defaultTime from properties
        capstone_project.dtos.request.vietmap.VietmapRouteV3Request request = 
                capstone_project.dtos.request.vietmap.VietmapRouteV3Request.builder()
                        .points(java.util.List.of(startPoint, issuePoint, endPoint))
                        .vehicle("truck") // Default vehicle type
                        .capacity(capacityKg) // Required for truck
                        // time: will use default from VietmapServiceImpl properties
                        // pointsEncoded: default true (encoded polyline)
                        .alternative(true) // Get alternative routes
                        .build();
        
        // 5️⃣ Call Vietmap Route V3 API
        try {
            capstone_project.dtos.response.vietmap.VietmapRouteV3Response response = 
                    vietmapService.routeV3(request);
            
            // Validate response has routes
            if (response.getPaths() == null || response.getPaths().isEmpty()) {
                log.warn("⚠️ No routes found for the requested points");
                throw new RuntimeException("Không tìm thấy lộ trình phù hợp cho các điểm đã chọn");
            }
            
            // Limit route count to prevent memory issues (max 10 routes)
            if (response.getPaths().size() > 10) {
                log.warn("⚠️ API returned {} routes, limiting to first 10", response.getPaths().size());
                response.setPaths(response.getPaths().subList(0, 10));
            }
            
            // Validate each route has required fields
            for (int i = 0; i < response.getPaths().size(); i++) {
                var path = response.getPaths().get(i);
                if (path.getPoints() == null || path.getPoints().toString().trim().isEmpty()) {
                    log.error("❌ Route {} has no points data", i);
                    throw new RuntimeException("Lộ trình " + (i + 1) + " thiếu dữ liệu tọa độ");
                }
                if (path.getDistance() == null || path.getDistance() <= 0) {
                    log.error("❌ Route {} has invalid distance: {}", i, path.getDistance());
                    throw new RuntimeException("Lộ trình " + (i + 1) + " có khoảng cách không hợp lệ");
                }
                if (path.getTime() == null || path.getTime() <= 0) {
                    log.error("❌ Route {} has invalid time: {}", i, path.getTime());
                    throw new RuntimeException("Lộ trình " + (i + 1) + " có thời gian không hợp lệ");
                }
                // Gracefully handle null/empty instructions array
                if (path.getInstructions() == null) {
                    log.warn("⚠️ Route {} has no instructions, setting empty array", i);
                    path.setInstructions(java.util.List.of());
                }
            }
            
            log.info("✅ Received {} suggested route(s) from Vietmap API", 
                    response.getPaths().size());
            
            // Log route summary for debugging
            for (int i = 0; i < response.getPaths().size(); i++) {
                var path = response.getPaths().get(i);
                double distanceKm = path.getDistance() / 1000.0;
                long timeMinutes = path.getTime() / 60000;
                log.info("   Route {}: {:.2f} km, {} minutes, points_encoded: {}", 
                        i + 1, distanceKm, timeMinutes, path.getPointsEncoded());
            }
            
            return response;
            
        } catch (RuntimeException e) {
            // Re-throw our custom exceptions
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to get suggested routes from Vietmap API: {}", e.getMessage());
            throw new RuntimeException("Không thể lấy gợi ý lộ trình: " + e.getMessage(), e);
        }
    }
    
    /**
     * Validate coordinate ranges (Vietnam approximate bounds)
     */
    private void validateCoordinates(Double latitude, Double longitude, String pointName) {
        // Vietnam approximate bounds: 8.0 to 24.0 latitude, 102.0 to 110.0 longitude
        if (latitude < 8.0 || latitude > 24.0) {
            throw new BadRequestException(
                    String.format("Tọa độ vĩ độ (%.6f) của %s không hợp lệ (phải trong khoảng [8.0, 24.0])", latitude, pointName), 
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }
        if (longitude < 102.0 || longitude > 110.0) {
            throw new BadRequestException(
                    String.format("Tọa độ kinh độ (%.6f) của %s không hợp lệ (phải trong khoảng [102.0, 110.0])", longitude, pointName), 
                    ErrorEnum.INVALID_REQUEST.getErrorCode());
        }
    }

    // ===== OFF_ROUTE_RUNAWAY implementation =====
    
    @Override
    public capstone_project.dtos.response.issue.OffRouteRunawayDetailResponse getOffRouteRunawayDetail(UUID issueId) {
        // Get issue with details
        IssueEntity issue = issueEntityService.findByIdWithDetails(issueId)
                .orElseThrow(() -> new NotFoundException("Issue not found: " + issueId, ErrorEnum.NOT_FOUND.getErrorCode()));
        
        // Validate issue category
        if (issue.getIssueTypeEntity() == null || 
            !IssueCategoryEnum.OFF_ROUTE_RUNAWAY.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            throw new BadRequestException("Issue is not an OFF_ROUTE_RUNAWAY type", ErrorEnum.INVALID_REQUEST.getErrorCode());
        }
        
        VehicleAssignmentEntity assignment = issue.getVehicleAssignmentEntity();
        if (assignment == null) {
            throw new NotFoundException("Vehicle assignment not found for issue", ErrorEnum.NOT_FOUND.getErrorCode());
        }
        
        // Get off-route event info
        capstone_project.dtos.response.issue.OffRouteRunawayDetailResponse.OffRouteEventInfo eventInfo = null;
        Optional<capstone_project.entity.offroute.OffRouteEventEntity> eventOpt = offRouteEventEntityService.findByIssueId(issueId);
        if (eventOpt.isPresent()) {
            capstone_project.entity.offroute.OffRouteEventEntity event = eventOpt.get();
            eventInfo = capstone_project.dtos.response.issue.OffRouteRunawayDetailResponse.OffRouteEventInfo.builder()
                    .eventId(event.getId())
                    .detectedAt(event.getOffRouteStartTime())
                    .offRouteDurationMinutes(event.getOffRouteDurationMinutes())
                    .distanceFromRouteMeters(event.getDistanceFromRouteMeters() != null ? java.math.BigDecimal.valueOf(event.getDistanceFromRouteMeters()) : null)
                    .warningStatus(event.getWarningStatus() != null ? event.getWarningStatus().name() : null)
                    .canContactDriver(event.getCanContactDriver())
                    .contactNotes(event.getContactNotes())
                    .contactedAt(event.getContactedAt())
                    .build();
        }
        
        // Get all packages in the trip
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(assignment);
        java.math.BigDecimal totalDeclaredValue = java.math.BigDecimal.ZERO;
        List<capstone_project.dtos.response.issue.OffRouteRunawayDetailResponse.PackageInfo> packages = new java.util.ArrayList<>();
        
        for (OrderDetailEntity od : orderDetails) {
            packages.add(capstone_project.dtos.response.issue.OffRouteRunawayDetailResponse.PackageInfo.builder()
                    .orderDetailId(od.getId())
                    .trackingCode(od.getTrackingCode())
                    .description(od.getDescription())
                    .weightBaseUnit(od.getWeightBaseUnit())
                    .unit(od.getUnit())
                    .declaredValue(od.getDeclaredValue())
                    .status(od.getStatus())
                    .build());
            
            if (od.getDeclaredValue() != null) {
                totalDeclaredValue = totalDeclaredValue.add(od.getDeclaredValue());
            }
        }
        
        // Get sender info 
        capstone_project.dtos.response.user.CustomerResponse senderResponse = null;
        if (!orderDetails.isEmpty() && orderDetails.get(0).getOrderEntity() != null) {
            OrderEntity order = orderDetails.get(0).getOrderEntity();
            
            // Get sender info
            if (order.getSender() != null) {
                capstone_project.entity.user.customer.CustomerEntity sender = order.getSender();
                senderResponse = capstone_project.dtos.response.user.CustomerResponse.builder()
                        .id(sender.getId().toString())
                        .companyName(sender.getCompanyName())
                        .representativeName(sender.getRepresentativeName())
                        .representativePhone(sender.getRepresentativePhone())
                        .businessAddress(sender.getBusinessAddress())
                        .build();
            }
        }
        
        // Get refund if exists
        capstone_project.dtos.response.refund.GetRefundResponse refundResponse = null;
        try {
            refundResponse = refundService.getRefundByIssueId(issueId);
        } catch (Exception e) {
            // No refund yet - that's fine
            log.debug("No refund found for issue {}: {}", issueId, e.getMessage());
        }

        // Resolve transport fee from contract similar to DAMAGE flow
        java.math.BigDecimal transportFee = java.math.BigDecimal.ZERO;
        java.math.BigDecimal legalLimit = java.math.BigDecimal.ZERO;
        try {
            if (!orderDetails.isEmpty() && orderDetails.get(0).getOrderEntity() != null) {
                OrderEntity orderForFee = orderDetails.get(0).getOrderEntity();
                var contractOpt = contractEntityService.getContractByOrderId(orderForFee.getId());
                if (contractOpt.isPresent()) {
                    var contract = contractOpt.get();
                    if (contract.getAdjustedValue() != null && contract.getAdjustedValue().compareTo(java.math.BigDecimal.ZERO) > 0) {
                        transportFee = contract.getAdjustedValue();
                    } else if (contract.getTotalValue() != null) {
                        transportFee = contract.getTotalValue();
                    }
                    legalLimit = transportFee.multiply(java.math.BigDecimal.TEN);
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not resolve transport fee for OFF_ROUTE_RUNAWAY issue {}: {}", issueId, e.getMessage());
        }

        String compensationPolicyNote = "Trường hợp lỗi cố ý của tài xế (chiếm đoạt/bỏ trốn): bồi thường 100% giá trị thực tế lô hàng cộng 100% cước phí vận chuyển, không áp dụng giới hạn 10× cước phí. " +
                "Để xác định đúng giá trị thực tế, khách hàng cần cung cấp hóa đơn/chứng từ hợp lệ của lô hàng.";

        // Suggested compensation: 100% giá trị lô hàng (totalDeclaredValue) + 100% cước phí vận chuyển
        java.math.BigDecimal suggestedCompensation = java.math.BigDecimal.ZERO;
        if (totalDeclaredValue != null) {
            suggestedCompensation = totalDeclaredValue;
        }
        if (transportFee != null) {
            suggestedCompensation = suggestedCompensation.add(transportFee);
        }
        
        return capstone_project.dtos.response.issue.OffRouteRunawayDetailResponse.builder()
                .issueId(issue.getId())
                .description(issue.getDescription())
                .status(issue.getStatus())
                .reportedAt(issue.getReportedAt())
                .resolvedAt(issue.getResolvedAt())
                .locationLatitude(issue.getLocationLatitude())
                .locationLongitude(issue.getLocationLongitude())
                .offRouteEventInfo(eventInfo)
                .vehicleAssignment(vehicleAssignmentMapper.toResponse(assignment))
                .sender(senderResponse)
                .packages(packages)
                .totalDeclaredValue(totalDeclaredValue)
                .transportFee(transportFee)
                .legalLimit(legalLimit)
                .compensationPolicyNote(compensationPolicyNote)
                .suggestedCompensation(suggestedCompensation)
                .refund(refundResponse)
                .assessment(null) // Assessment now fetched via CompensationService separately
                .build();
    }

    // ===== DAMAGE compensation flow methods =====
    // NOTE: These methods now delegate to CompensationService which uses IssueCompensationAssessmentEntity
    
    @Override
    @Transactional
    public GetBasicIssueResponse updateDamageCompensation(capstone_project.dtos.request.issue.UpdateDamageCompensationRequest request) {
        log.info("📊 Updating DAMAGE compensation for issue: {} (delegating to CompensationService)", request.issueId());
        
        // Convert legacy UpdateDamageCompensationRequest to unified CompensationAssessmentRequest
        capstone_project.dtos.request.issue.CompensationAssessmentRequest unifiedRequest = 
            capstone_project.dtos.request.issue.CompensationAssessmentRequest.builder()
                .issueId(request.issueId())
                .issueType("DAMAGE")
                .hasDocuments(request.damageHasDocuments())
                .documentValue(request.damageDeclaredValue())
                .estimatedMarketValue(request.damageEstimatedMarketValue())
                .assessmentRatePercent(request.damageAssessmentPercent())
                .finalCompensation(request.damageFinalCompensation())
                .adjustReason(request.damageAdjustReason())
                .handlerNotes(request.damageHandlerNote())
                .fraudDetected(request.fraudDetected())
                .fraudReason(request.fraudReason())
                .build();
        
        // Delegate to CompensationService
        compensationService.resolveCompensation(unifiedRequest);
        
        log.info("✅ DAMAGE compensation updated via CompensationService");
        
        // Return full issue response
        return getBasicIssue(request.issueId());
    }
    
    @Override
    public capstone_project.dtos.response.issue.DamageCompensationResponse getDamageCompensationDetail(UUID issueId) {
        log.info("📊 Getting DAMAGE compensation detail for issue: {} (delegating to CompensationService)", issueId);
        
        // Get compensation detail from CompensationService
        var compensationDetail = compensationService.getCompensationDetail(issueId);
        
        // Convert CompensationDetailResponse to legacy DamageCompensationResponse for backward compatibility
        return convertToDamageCompensationResponse(compensationDetail);
    }
    
    /**
     * Convert unified CompensationDetailResponse to legacy DamageCompensationResponse
     * for backward compatibility with existing frontend
     */
    private capstone_project.dtos.response.issue.DamageCompensationResponse convertToDamageCompensationResponse(
            capstone_project.dtos.response.issue.CompensationDetailResponse detail) {
        
        if (detail == null || detail.getAssessment() == null) {
            return null;
        }
        
        var assessment = detail.getAssessment();
        var breakdown = detail.getCompensationBreakdown();
        var orderContext = detail.getOrderContext();
        
        // Determine case and labels based on hasInsurance and hasDocuments
        Boolean hasInsurance = orderContext != null ? orderContext.getHasInsurance() : false;
        Boolean hasDocuments = assessment.getHasDocuments();
        
        String compensationCase = breakdown != null ? breakdown.getCompensationCase() : null;
        String caseLabel = null;
        String caseDescription = null;
        Boolean appliesLegalLimit = null;
        
        if (compensationCase != null) {
            try {
                var caseEnum = capstone_project.common.enums.DamageCompensationCaseEnum.valueOf(compensationCase);
                caseLabel = caseEnum.getLabel();
                caseDescription = caseEnum.getDescription();
                appliesLegalLimit = caseEnum.appliesLegalLimit();
            } catch (IllegalArgumentException e) {
                // Invalid case enum, ignore
            }
        }
        
        return new capstone_project.dtos.response.issue.DamageCompensationResponse(
            assessment.getAssessmentRate() != null ? 
                assessment.getAssessmentRate().multiply(java.math.BigDecimal.valueOf(100)) : null,
            hasInsurance,
            hasDocuments,
            assessment.getDocumentValue(),
            assessment.getEstimatedMarketValue(),
            orderContext != null ? orderContext.getTransportFee() : null,
            breakdown != null ? breakdown.getLegalLimit() : null,
            breakdown != null ? breakdown.getGoodsCompensation() : null,
            assessment.getCompensationByPolicy(),
            assessment.getFinalCompensation(),
            compensationCase,
            caseLabel,
            caseDescription,
            appliesLegalLimit,
            assessment.getAdjustReason(),
            assessment.getHandlerNotes(),
            detail.getIssueStatus(),
            null // statusLabel - can be derived from issueStatus if needed
        );
    }
    
    /**
     * Create issue notifications - broadcast to staff, notify customer if damage
     */
    private void createIssueNotifications(IssueEntity issue) {
        try {
            VehicleAssignmentEntity assignment = issue.getVehicleAssignmentEntity();
            if (assignment == null) {
                log.warn("Cannot create issue notifications - no vehicle assignment");
                return;
            }
            
            List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(assignment);
            if (orderDetails == null || orderDetails.isEmpty()) {
                log.warn("Cannot create issue notifications - no order details");
                return;
            }
            
            OrderEntity order = orderDetails.get(0).getOrderEntity();
            if (order == null) {
                log.warn("Cannot create issue notifications - no order");
                return;
            }
            
            String issueCode = "ISS-" + issue.getId().toString().substring(0, 8).toUpperCase();
            String issueTypeName = issue.getIssueTypeEntity() != null ? 
                issue.getIssueTypeEntity().getIssueTypeName() : "Sự cố";
            
            // Notification 1: Broadcast to ALL STAFF
            try {
                List<UserEntity> staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
                for (UserEntity staff : staffUsers) {
                    String driverName = assignment.getDriver1() != null && assignment.getDriver1().getUser() != null ? 
                        assignment.getDriver1().getUser().getFullName() : "N/A";
                    String driverPhone = assignment.getDriver1() != null && assignment.getDriver1().getUser() != null ? 
                        assignment.getDriver1().getUser().getPhoneNumber() : "N/A";
                    String vehiclePlate = assignment.getVehicleEntity() != null ? 
                        assignment.getVehicleEntity().getLicensePlateNumber() : "N/A";
                    
                    CreateNotificationRequest staffNotif = NotificationBuilder.buildNewIssueReported(
                        staff.getId(),
                        issueCode,
                        issueTypeName,
                        order.getOrderCode(),
                        driverName,
                        driverPhone,
                        vehiclePlate,
                        order.getId(),
                        issue.getId(),
                        assignment.getId()
                    );
                    notificationService.createNotification(staffNotif);
                }
                log.info("✅ Broadcast NEW_ISSUE_REPORTED to {} staff members", staffUsers.size());
            } catch (Exception e) {
                log.error("❌ Failed to broadcast issue notifications to staff: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌ Failed to create issue notifications: {}", e.getMessage());
        }
    }
    
    /**
     * Helper method to send staff notifications
     * Creates a notification for each staff user from a template
     */
    private void sendStaffNotification(CreateNotificationRequest template) {
        try {
            var staffUsers = userEntityService.getUserEntitiesByRoleRoleName("STAFF");
            if (!staffUsers.isEmpty()) {
                for (var staff : staffUsers) {
                    // Create a new notification request for each staff user
                    CreateNotificationRequest staffNotification = CreateNotificationRequest.builder()
                        .userId(staff.getId())
                        .recipientRole("STAFF")
                        .title(template.getTitle())
                        .description(template.getDescription())
                        .notificationType(template.getNotificationType())
                        .relatedOrderId(template.getRelatedOrderId())
                        .relatedIssueId(template.getRelatedIssueId())
                        .relatedVehicleAssignmentId(template.getRelatedVehicleAssignmentId())
                        .relatedContractId(template.getRelatedContractId())
                        .metadata(template.getMetadata())
                        .build();
                    
                    notificationService.createNotification(staffNotification);
                }
                log.info("📧 Staff notifications sent: {} staff users notified, type: {}", 
                    staffUsers.size(), template.getNotificationType());
            }
        } catch (Exception e) {
            log.error("❌ Failed to send staff notification: {}", e.getMessage());
            // Don't throw - Notification failure shouldn't break business logic
        }
    }

    
    private CustomerEntity getCustomerFromIssue(IssueEntity issue) {
        if (issue.getOrderDetails() != null && !issue.getOrderDetails().isEmpty()) {
            OrderDetailEntity orderDetail = issue.getOrderDetails().get(0);
            OrderEntity order = orderDetail.getOrderEntity();
            return order.getSender();
        }
        return null;
    }
}
