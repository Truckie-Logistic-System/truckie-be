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
import capstone_project.service.services.websocket.IssueWebSocketService;
import capstone_project.dtos.request.issue.*;
import capstone_project.dtos.response.issue.GetBasicIssueResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.order.order.OrderEntityService;

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
    
    // Return payment deadline configuration
    @org.springframework.beans.factory.annotation.Value("${issue.return-payment.deadline-minutes:30}")
    private int returnPaymentDeadlineMinutes;


    @Override
    public GetBasicIssueResponse getBasicIssue(UUID issueId) {
        // Use findByIdWithDetails to eagerly fetch all related entities
        IssueEntity getIssue = issueEntityService.findByIdWithDetails(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found"));

        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(getIssue);

        // Fetch issue images
        List<String> issueImages = issueImageEntityService.findByIssueEntity_Id(issueId)
                .stream()
                .map(capstone_project.entity.issue.IssueImageEntity::getImageUrl)
                .collect(java.util.stream.Collectors.toList());
        
        log.info("📸 Fetched {} issue images for issue {}", issueImages.size(), issueId);
        if (!issueImages.isEmpty()) {
            log.info("   - Image URLs: {}", issueImages);
        }
        
        // Populate vehicle assignment với nested objects (now with user info already loaded)
        if (getIssue.getVehicleAssignmentEntity() != null) {
            var vehicleAssignment = getIssue.getVehicleAssignmentEntity();
            var enrichedVA = mapVehicleAssignmentWithDetails(vehicleAssignment);
            
            // Create new response with enriched vehicle assignment, issue images, and order detail
            return new GetBasicIssueResponse(
                response.id(),
                response.description(),
                response.locationLatitude(),
                response.locationLongitude(),
                response.status(),
                response.issueCategory(),
                response.reportedAt(),
                response.resolvedAt(),
                enrichedVA,
                response.staff(),
                response.issueTypeEntity(),
                response.oldSeal(),
                response.newSeal(),
                response.sealRemovalImage(),
                response.newSealAttachedImage(),
                response.newSealConfirmedAt(),
                issueImages,
                response.orderDetail(),
                response.sender(),
                response.paymentDeadline(),
                response.calculatedFee(),
                response.adjustedFee(),
                response.finalFee(),
                response.affectedOrderDetails(),
                response.returnTransaction()
            );
        }
        
        // Return with issue images and order detail even if no vehicle assignment
        return new GetBasicIssueResponse(
            response.id(),
            response.description(),
            response.locationLatitude(),
            response.locationLongitude(),
            response.status(),
            response.issueCategory(),
            response.reportedAt(),
            response.resolvedAt(),
            response.vehicleAssignmentEntity(),
            response.staff(),
            response.issueTypeEntity(),
            response.oldSeal(),
            response.newSeal(),
            response.sealRemovalImage(),
            response.newSealAttachedImage(),
            response.newSealConfirmedAt(),
            issueImages,
            response.orderDetail(),
            response.sender(),
            response.paymentDeadline(),
            response.calculatedFee(),
            response.adjustedFee(),
            response.finalFee(),
            response.affectedOrderDetails(),
            response.returnTransaction()
        );
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
                    vehicle.getVehicleTypeEntity().getVehicleTypeName()
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
            driver2
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

        // Lưu trạng thái hiện tại của order details (để restore sau khi resolve)
        // Format: "STATUS1,STATUS2,STATUS3" theo thứ tự order details
        String tripStatusAtReport = orderDetails.stream()
                .map(OrderDetailEntity::getStatus)
                .reduce((s1, s2) -> s1 + "," + s2)
                .orElse("");
        
        log.info("💾 Saving trip status at report: {}", tripStatusAtReport);
        
        // Update tất cả order details thành IN_TROUBLES
        orderDetails.forEach(orderDetail -> {
            String oldStatus = orderDetail.getStatus();
            orderDetail.setStatus(OrderDetailStatusEnum.IN_TROUBLES.name());
            orderDetailEntityService.save(orderDetail);
            log.info("🚨 Updated order detail {} from {} to IN_TROUBLES", 
                     orderDetail.getId(), oldStatus);
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

        // Convert sang response
        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(saved);
        
        // 📢 Broadcast new issue to all staff clients via WebSocket
        log.info("🚨 New issue created, broadcasting to staff: {}", response.id());
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
        log.info("📊 Issue status changed to IN_PROGRESS, broadcasting: {}", response.id());
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

        // Lấy vehicle assignment
        var vehicleAssignment = issue.getVehicleAssignmentEntity();
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(vehicleAssignment);

        // Parse tripStatusAtReport để restore statuses
        String tripStatusAtReport = issue.getTripStatusAtReport();
        
        if (tripStatusAtReport != null && !tripStatusAtReport.isEmpty()) {
            String[] savedStatuses = tripStatusAtReport.split(",");
            
            // Restore status cho từng order detail
            for (int i = 0; i < Math.min(orderDetails.size(), savedStatuses.length); i++) {
                OrderDetailEntity orderDetail = orderDetails.get(i);
                String restoredStatus = savedStatuses[i].trim();
                
                log.info("✅ Restoring order detail {} from IN_TROUBLES to {}", 
                         orderDetail.getId(), restoredStatus);
                
                orderDetail.setStatus(restoredStatus);
                orderDetailEntityService.save(orderDetail);
            }
        } else {
            log.warn("⚠️ No tripStatusAtReport found for issue {}, cannot restore statuses", issueId);
        }

        // Update issue status to RESOLVED
        issue.setStatus(IssueEnum.RESOLVED.name());
        issue.setResolvedAt(java.time.LocalDateTime.now());
        
        IssueEntity updated = issueEntityService.save(issue);

        // Convert sang response
        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(updated);
        
        // 📢 Broadcast status change (IN_PROGRESS -> RESOLVED)
        log.info("✅ Issue resolved, broadcasting: {}", response.id());
        issueWebSocketService.broadcastIssueStatusChange(response);
        
        // 📲 Send notification to driver if this is a DAMAGE issue
        if (issue.getIssueTypeEntity() != null && 
            IssueCategoryEnum.DAMAGE.name().equals(issue.getIssueTypeEntity().getIssueCategory())) {
            
            if (issue.getVehicleAssignmentEntity() != null) {
                var driver1 = issue.getVehicleAssignmentEntity().getDriver1();
                if (driver1 != null && driver1.getUser() != null) {
                    String staffName = issue.getStaff() != null ? 
                                      issue.getStaff().getFullName() : "Nhân viên";
                    
                    log.info("📦 Sending damage resolved notification to driver: {}", driver1.getUser().getId());
                    issueWebSocketService.sendDamageResolvedNotification(
                        driver1.getUser().getId().toString(),
                        response,
                        staffName
                    );
                }
            }
        }
        
        return response;
    }

    @Override
    @Transactional
    public GetBasicIssueResponse updateIssueStatus(UUID issueId, String status) {
        log.info("🔄 Updating issue status: issueId={}, newStatus={}", issueId, status);

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
            log.info("✅ Issue marked as RESOLVED at {}", issue.getResolvedAt());
        }

        IssueEntity updated = issueEntityService.save(issue);
        GetBasicIssueResponse response = getBasicIssue(updated.getId());

        // Broadcast status change
        log.info("📢 Issue status changed from {} to {}, broadcasting", oldStatus, status);
        issueWebSocketService.broadcastIssueStatusChange(response);

        return response;
    }

    @Override
    @Transactional
    public GetBasicIssueResponse reportSealIssue(ReportSealIssueRequest request) {
        log.info("🔓 Driver reporting seal removal issue for seal: {}", request.sealId());

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
        log.info("🔍 DEBUG: IssueType ID: {}, Name: {}, Category: {}", 
                issueType.getId(),
                issueType.getIssueTypeName(),
                issueType.getIssueCategory());

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

        // Lưu trạng thái order details (giống như createIssue)
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(vehicleAssignment);
        String tripStatusAtReport = orderDetails.stream()
                .map(OrderDetailEntity::getStatus)
                .reduce((s1, s2) -> s1 + "," + s2)
                .orElse("");

        log.info("💾 Saving trip status at report: {}", tripStatusAtReport);

        // Update tất cả order details thành IN_TROUBLES
        orderDetails.forEach(orderDetail -> {
            String oldStatus = orderDetail.getStatus();
            orderDetail.setStatus(OrderDetailStatusEnum.IN_TROUBLES.name());
            orderDetailEntityService.save(orderDetail);
            log.info("🚨 Updated order detail {} from {} to IN_TROUBLES", 
                     orderDetail.getId(), oldStatus);
        });

        // Upload seal removal image to Cloudinary
        String sealRemovalImageUrl;
        try {
            log.info("📤 Uploading seal removal image to Cloudinary...");
            String fileName = "seal_removal_" + oldSeal.getId() + "_" + System.currentTimeMillis();
            var uploadResult = cloudinaryService.uploadFile(
                    request.sealRemovalImage().getBytes(),
                    fileName,
                    "issues/seal-removal"
            );
            sealRemovalImageUrl = (String) uploadResult.get("secure_url");
            log.info("✅ Seal removal image uploaded: {}", sealRemovalImageUrl);
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
        log.info("🔓 Old seal {} marked as REMOVED due to issue report", oldSeal.getId());

        // Lưu issue
        IssueEntity saved = issueEntityService.save(issue);
        log.info("✅ Seal removal issue saved with ID: {}", saved.getId());

        // Fetch full issue with all nested objects (vehicle, drivers, images)
        GetBasicIssueResponse response = getBasicIssue(saved.getId());

        // 📢 Broadcast seal issue to staff
        log.info("🔓 Seal removal issue created, broadcasting to staff: {}", response.id());
        issueWebSocketService.broadcastNewIssue(response);

        return response;
    }

    @Override
    @Transactional
    public GetBasicIssueResponse assignNewSeal(AssignNewSealRequest request) {
        log.info("🔐 Staff assigning new seal {} to issue {}", request.newSealId(), request.issueId());

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
        log.info("🔍 Seal status validation - Seal ID: {}, Status: '{}', Expected: '{}'", 
                newSeal.getId(), 
                newSeal.getStatus(), 
                SealEnum.ACTIVE.name());

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
        log.info("🔐 New seal assigned, broadcasting: {}", response.id());
        issueWebSocketService.broadcastIssueStatusChange(response);

        // 📢 Send notification to driver
        if (issue.getVehicleAssignmentEntity() != null) {
            var driver1 = issue.getVehicleAssignmentEntity().getDriver1();
            if (driver1 != null) {
                log.info("📲 Sending seal assignment notification to driver: {}", driver1.getId());
                issueWebSocketService.sendSealAssignmentNotification(
                    driver1.getId().toString(),
                    response,
                    staff.getFullName(),
                    newSeal.getSealCode(),
                    issue.getOldSeal().getSealCode()
                );
            }
        }

        return response;
    }

    @Override
    @Transactional
    public GetBasicIssueResponse confirmNewSeal(ConfirmNewSealRequest request) {
        log.info("✅ Driver confirming new seal attachment for issue {}", request.issueId());

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
        log.info("🔓 Old seal {} marked as REMOVED", oldSeal.getId());

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
            log.info("📸 Seal image uploaded to Cloudinary: {}", imageUrl);
        } catch (Exception e) {
            log.error("❌ Failed to upload seal image to Cloudinary: {}", e.getMessage(), e);
            // Fallback: store base64 if upload fails
            newSeal.setSealAttachedImage(request.newSealAttachedImage());
        }
        
        newSeal.setSealDate(java.time.LocalDateTime.now());
        sealEntityService.save(newSeal);
        log.info("🔐 New seal {} marked as IN_USE", newSeal.getId());

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
            log.info("📸 Issue seal image uploaded to Cloudinary: {}", imageUrl);
        } catch (Exception e) {
            log.error("❌ Failed to upload issue seal image to Cloudinary: {}", e.getMessage(), e);
            // Fallback: store base64 if upload fails
            issue.setNewSealAttachedImage(request.newSealAttachedImage());
        }
        
        issue.setNewSealConfirmedAt(java.time.LocalDateTime.now());
        issue.setStatus(IssueEnum.RESOLVED.name());
        issue.setResolvedAt(java.time.LocalDateTime.now());

        // Restore order detail statuses
        var vehicleAssignment = issue.getVehicleAssignmentEntity();
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(vehicleAssignment);
        String tripStatusAtReport = issue.getTripStatusAtReport();

        if (tripStatusAtReport != null && !tripStatusAtReport.isEmpty()) {
            String[] savedStatuses = tripStatusAtReport.split(",");
            for (int i = 0; i < Math.min(orderDetails.size(), savedStatuses.length); i++) {
                OrderDetailEntity orderDetail = orderDetails.get(i);
                String restoredStatus = savedStatuses[i].trim();
                orderDetail.setStatus(restoredStatus);
                orderDetailEntityService.save(orderDetail);
                log.info("✅ Restored order detail {} to {}", orderDetail.getId(), restoredStatus);
            }
        }

        IssueEntity updated = issueEntityService.save(issue);

        // Convert sang response
        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(updated);

        // 📢 Broadcast resolution
        log.info("✅ Seal replacement completed, broadcasting: {}", response.id());
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
                
                log.info("📲 Sending seal confirmation message to staff: {}", staffId);
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

        return response;
    }

    @Override
    public capstone_project.dtos.response.order.seal.GetSealResponse getInUseSealByVehicleAssignment(UUID vehicleAssignmentId) {
        log.info("Getting IN_USE seal for vehicle assignment: {}", vehicleAssignmentId);
        
        // Tìm vehicle assignment
        var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy vehicle assignment với ID: " + vehicleAssignmentId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Lấy seal đang IN_USE
        SealEntity inUseSeal = sealEntityService.findByVehicleAssignment(vehicleAssignment, SealEnum.IN_USE.name());
        
        if (inUseSeal == null) {
            log.warn("Không tìm thấy seal IN_USE cho vehicle assignment: {}", vehicleAssignmentId);
            throw new NotFoundException(
                    "Không tìm thấy seal đang được sử dụng cho vehicle assignment này",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }
        
        // Convert to response
        return sealMapper.toGetSealResponse(inUseSeal);
    }

    @Override
    public List<capstone_project.dtos.response.order.seal.GetSealResponse> getActiveSealsByVehicleAssignment(UUID vehicleAssignmentId) {
        log.info("Getting ACTIVE seals for vehicle assignment: {}", vehicleAssignmentId);
        
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
        
        log.info("Found {} ACTIVE seals for vehicle assignment: {}", activeSeals.size(), vehicleAssignmentId);
        
        // Convert to response
        return activeSeals.stream()
                .map(sealMapper::toGetSealResponse)
                .toList();
    }

    @Override
    public List<GetBasicIssueResponse> getPendingSealReplacementsByVehicleAssignment(UUID vehicleAssignmentId) {
        log.info("Getting pending seal replacements for vehicle assignment: {}", vehicleAssignmentId);
        
        // Tìm vehicle assignment
        var vehicleAssignment = vehicleAssignmentEntityService.findEntityById(vehicleAssignmentId)
                .orElseThrow(() -> new NotFoundException(
                        "Không tìm thấy vehicle assignment với ID: " + vehicleAssignmentId,
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));
        
        // Lấy tất cả issues rồi filter theo vehicle assignment và các điều kiện khác
        List<IssueEntity> allIssues = issueEntityService.findAllSortedByReportedAtDesc();
        log.info("🔍 DEBUG: Total issues found: {}", allIssues.size());
        
        // Debug: Print all issues info
        for (IssueEntity issue : allIssues) {
            log.info("🔍 DEBUG: Issue {} - Status: {}, Category: {}, NewSeal: {}, ConfirmedAt: {}", 
                    issue.getId(),
                    issue.getStatus(),
                    issue.getIssueTypeEntity() != null ? issue.getIssueTypeEntity().getIssueCategory() : "NULL",
                    issue.getNewSeal() != null ? issue.getNewSeal().getSealCode() : "NULL",
                    issue.getNewSealConfirmedAt());
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
                    log.info("🔍 FILTER VA: Issue {} matches VA: {}", issue.getId(), matchesVA);
                    return matchesVA;
                })
                .filter(issue -> {
                    boolean matchesStatus = IssueEnum.IN_PROGRESS.name().equals(issue.getStatus());
                    log.info("🔍 FILTER STATUS: Issue {} status {} matches IN_PROGRESS: {}", 
                            issue.getId(), issue.getStatus(), matchesStatus);
                    return matchesStatus;
                })
                .filter(issue -> {
                    String category = issue.getIssueTypeEntity() != null ? 
                            issue.getIssueTypeEntity().getIssueCategory() : "NULL";
                    boolean matchesCategory = IssueCategoryEnum.SEAL_REPLACEMENT.name().equals(category);
                    log.info("🔍 FILTER CATEGORY: Issue {} category {} matches SEAL_REPLACEMENT: {}", 
                            issue.getId(), category, matchesCategory);
                    return matchesCategory;
                })
                .filter(issue -> {
                    boolean hasNewSeal = issue.getNewSeal() != null;
                    log.info("🔍 FILTER NEW SEAL: Issue {} has new seal: {}", 
                            issue.getId(), hasNewSeal);
                    return hasNewSeal;
                })
                .filter(issue -> {
                    boolean notConfirmed = issue.getNewSealConfirmedAt() == null;
                    log.info("🔍 FILTER CONFIRMED: Issue {} newSealConfirmedAt {} is null: {}", 
                            issue.getId(), issue.getNewSealConfirmedAt(), notConfirmed);
                    return notConfirmed;
                })
                .toList();
        
        log.info("Found {} pending seal replacement(s) for vehicle assignment: {}", 
                pendingReplacements.size(), vehicleAssignmentId);
        
        // Convert to response
        return pendingReplacements.stream()
                .map(issueMapper::toIssueBasicResponse)
                .toList();
    }

    @Override
    @Transactional
    public GetBasicIssueResponse reportDamageIssue(ReportDamageIssueRequest request) {
        log.info("📦 Driver reporting damaged goods issue for {} order detail(s)", 
                request.orderDetailIds() != null ? request.orderDetailIds().size() : 0);

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

        // Lưu trạng thái hiện tại của vehicle assignment (lấy từ order detail đầu tiên)
        String tripStatusAtReport = null;
        if (request.orderDetailIds() != null && !request.orderDetailIds().isEmpty()) {
            var firstOrderDetail = orderDetailEntityService.findByTrackingCode(
                    request.orderDetailIds().get(0)
            );
            tripStatusAtReport = firstOrderDetail.map(OrderDetailEntity::getStatus).orElse(null);
        }
        log.info("💾 Saving trip status at report: {}", tripStatusAtReport);

        // Tạo Issue trước
        log.info("📍 Creating damage issue with location: lat={}, lng={}", 
                request.locationLatitude(), request.locationLongitude());
        
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
        log.info("✅ Issue created with ID: {}", saved.getId());

        // Update tất cả order details bị hư hại
        List<OrderDetailEntity> selectedOrderDetails = new java.util.ArrayList<>();
        if (request.orderDetailIds() != null && !request.orderDetailIds().isEmpty()) {
            for (String trackingCode : request.orderDetailIds()) {
                try {
                    OrderDetailEntity orderDetail = orderDetailEntityService.findByTrackingCode(trackingCode)
                            .orElseThrow(() -> new NotFoundException(
                                    ErrorEnum.ORDER_DETAIL_NOT_FOUND.getMessage() + trackingCode,
                                    ErrorEnum.ORDER_DETAIL_NOT_FOUND.getErrorCode()
                            ));

                    // Link order detail to issue and mark as IN_TROUBLES
                    // Driver can continue trip, staff will update to COMPENSATION when processing refund
                    String oldStatus = orderDetail.getStatus();
                    orderDetail.setIssueEntity(saved);
                    orderDetail.setStatus(OrderDetailStatusEnum.IN_TROUBLES.name());
                    orderDetailEntityService.save(orderDetail);
                    
                    selectedOrderDetails.add(orderDetail);
                    
                    log.info("🚨 Updated order detail {} from {} to IN_TROUBLES (damaged goods) and linked to issue {}", 
                             orderDetail.getId(), oldStatus, saved.getId());
                } catch (Exception e) {
                    log.error("❌ Error updating order detail {}: {}", trackingCode, e.getMessage());
                    throw new RuntimeException("Failed to update order detail: " + trackingCode, e);
                }
            }
            
            // Set orderDetails to issue for bidirectional relationship
            saved.setOrderDetails(selectedOrderDetails);
            issueEntityService.save(saved);
            
            log.info("✅ Linked {} order details to DAMAGE issue {}", 
                     selectedOrderDetails.size(), saved.getId());
            
            // Update remaining order details in this vehicle assignment to DELIVERED
            var affectedOrderDetailIds = selectedOrderDetails.stream()
                    .map(OrderDetailEntity::getId)
                    .collect(java.util.stream.Collectors.toSet());
            
            var allOrderDetails = orderDetailEntityService.findByVehicleAssignmentId(request.vehicleAssignmentId());
            var remainingOrderDetails = allOrderDetails.stream()
                    .filter(od -> !affectedOrderDetailIds.contains(od.getId()))
                    .filter(od -> "ONGOING_DELIVERED".equals(od.getStatus()) || "ON_DELIVERED".equals(od.getStatus()))
                    .collect(java.util.stream.Collectors.toList());
            
            if (!remainingOrderDetails.isEmpty()) {
                remainingOrderDetails.forEach(orderDetail -> {
                    orderDetail.setStatus(OrderDetailStatusEnum.DELIVERED.name());
                    orderDetailEntityService.save(orderDetail);
                    log.info("📦 OrderDetail {} (not damaged) status updated to DELIVERED", 
                             orderDetail.getTrackingCode());
                });
                log.info("✅ Updated {} remaining order details to DELIVERED status", 
                         remainingOrderDetails.size());
            }
        }

        // Upload damage images to Cloudinary and save to issue_images table
        if (request.damageImages() != null && !request.damageImages().isEmpty()) {
            for (MultipartFile imageFile : request.damageImages()) {
                try {
                    log.info("📤 Uploading damage image to Cloudinary...");
                    // Don't add .jpg extension - Cloudinary will add it based on the file
                    String imageUrl = cloudinaryService.uploadFile(imageFile.getBytes(), 
                            "damage_" + System.currentTimeMillis(), 
                            "damage_reports").get("secure_url").toString();
                    
                    // Check for double .jpg extension
                    if (imageUrl.contains(".jpg.jpg")) {
                        log.warn("⚠️ Double .jpg extension detected in URL: {}", imageUrl);
                    } else {
                        log.info("✅ Damage image uploaded (no double extension): {}", imageUrl);
                    }

                    // Save to issue_images table
                    capstone_project.entity.issue.IssueImageEntity issueImage = 
                        capstone_project.entity.issue.IssueImageEntity.builder()
                            .imageUrl(imageUrl)
                            .description("Ảnh hàng hóa bị hư hại")
                            .issueEntity(saved)
                            .build();
                    
                    issueImageEntityService.save(issueImage);
                    log.info("✅ Damage image saved to database");
                    
                } catch (Exception e) {
                    log.error("❌ Error uploading damage image: {}", e.getMessage());
                    throw new RuntimeException("Failed to upload damage image", e);
                }
            }
        }

        // Fetch full issue with all nested objects (vehicle, drivers, images)
        GetBasicIssueResponse response = getBasicIssue(saved.getId());

        // 📢 Broadcast damage issue to staff
        log.info("📦 Damage issue created, broadcasting to staff: {}", response.id());
        issueWebSocketService.broadcastNewIssue(response);

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
        
        log.info("🚨 Driver reporting traffic penalty violation");
        log.info("   - Vehicle Assignment ID: {}", vehicleAssignmentId);
        log.info("   - Issue Type ID: {}", issueTypeId);
        log.info("   - Violation Type: {}", violationType);

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
        log.info("📍 Creating penalty issue with location: lat={}, lng={}", locationLatitude, locationLongitude);
        
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
        log.info("✅ Penalty issue created with ID: {}", saved.getId());

        // Upload penalty violation record image to Cloudinary
        if (violationImage != null && !violationImage.isEmpty()) {
            try {
                log.info("📤 Uploading traffic violation record image to Cloudinary...");
                String imageUrl = cloudinaryService.uploadFile(
                        violationImage.getBytes(), 
                        "penalty_" + saved.getId() + "_" + System.currentTimeMillis(), 
                        "penalties/traffic-violations"
                ).get("secure_url").toString();
                
                log.info("✅ Penalty image uploaded: {}", imageUrl);

                // Save image URL to issue_images table
                IssueImageEntity imageEntity = IssueImageEntity.builder()
                        .imageUrl(imageUrl)
                        .description("Biên bản vi phạm giao thông")
                        .issueEntity(saved)
                        .build();
                
                issueImageEntityService.save(imageEntity);
                log.info("✅ Penalty image saved to database");
                
            } catch (Exception e) {
                log.error("❌ Error uploading penalty image: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to upload penalty violation image", e);
            }
        }

        // Fetch full issue with all nested objects
        GetBasicIssueResponse response = getBasicIssue(saved.getId());

        // Broadcast penalty issue to staff
        log.info("🚨 Penalty issue created, broadcasting to staff: {}", response.id());
        issueWebSocketService.broadcastNewIssue(response);

        return response;
    }

    // ===== ORDER_REJECTION flow implementations =====

    @Override
    @Transactional
    public GetBasicIssueResponse reportOrderRejection(capstone_project.dtos.request.issue.ReportOrderRejectionRequest request) {
        log.info("🚫 Driver reporting order rejection for vehicle assignment: {} with {} package(s)", 
                 request.vehicleAssignmentId(), request.orderDetailIds().size());

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

        log.info("📋 Auto-selected issue type: {} ({})", issueType.getIssueTypeName(), issueType.getId());

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

        // Save trip status at report (all order details in vehicle assignment)
        List<OrderDetailEntity> allOrderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(vehicleAssignment);
        String tripStatusAtReport = allOrderDetails.stream()
                .map(OrderDetailEntity::getStatus)
                .reduce((s1, s2) -> s1 + "," + s2)
                .orElse("");
        
        log.info("💾 Saving trip status at report: {}", tripStatusAtReport);
        
        // Update ONLY selected order details to IN_TROUBLES
        selectedOrderDetails.forEach(orderDetail -> {
            String oldStatus = orderDetail.getStatus();
            orderDetail.setStatus(OrderDetailStatusEnum.IN_TROUBLES.name());
            orderDetailEntityService.save(orderDetail);
            log.info("🚨 Updated order detail {} ({}) from {} to IN_TROUBLES", 
                     orderDetail.getId(), 
                     orderDetail.getOrderEntity().getOrderCode(),
                     oldStatus);
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
        log.info("✅ ORDER_REJECTION issue created with ID: {} for {} package(s)", 
                 saved.getId(), selectedOrderDetails.size());

        // Link selected order details to this issue (bidirectional)
        selectedOrderDetails.forEach(orderDetail -> {
            orderDetail.setIssueEntity(saved);
            orderDetailEntityService.save(orderDetail);
        });
        
        // Set orderDetails to issue for bidirectional relationship
        saved.setOrderDetails(selectedOrderDetails);
        issueEntityService.save(saved);
        
        log.info("✅ Linked {} order details to ORDER_REJECTION issue {}", 
                 selectedOrderDetails.size(), saved.getId());

        // Fetch full issue with all nested objects
        GetBasicIssueResponse response = getBasicIssue(saved.getId());

        // Broadcast to staff
        log.info("🚨 ORDER_REJECTION issue created, broadcasting to staff: {}", response.id());
        issueWebSocketService.broadcastNewIssue(response);

        return response;
    }

    @Override
    public capstone_project.dtos.response.issue.ReturnShippingFeeResponse calculateReturnShippingFee(UUID issueId) {
        log.info("💰 Calculating return shipping fee for issue: {}", issueId);
        
        // Use the overloaded method with null distance to use default calculation
        return calculateReturnShippingFee(issueId, null);
    }
    
    @Override
    public capstone_project.dtos.response.issue.ReturnShippingFeeResponse calculateReturnShippingFee(UUID issueId, java.math.BigDecimal actualDistanceKm) {
        log.info("💰 Calculating return shipping fee for issue: {} with custom distance: {}", issueId, actualDistanceKm);

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

        log.info("📦 Calculating fee for {} selected package(s) to return", selectedOrderDetails.size());

        // Get order from first order detail (all should belong to same order)
        var order = selectedOrderDetails.get(0).getOrderEntity();

        // Get vehicle type from issue's vehicle assignment
        String vehicleType = null;
        if (issue.getVehicleAssignmentEntity() != null 
            && issue.getVehicleAssignmentEntity().getVehicleEntity() != null 
            && issue.getVehicleAssignmentEntity().getVehicleEntity().getVehicleTypeEntity() != null) {
            vehicleType = issue.getVehicleAssignmentEntity().getVehicleEntity().getVehicleTypeEntity().getVehicleTypeName();
            log.info("🚗 Found vehicle type from assignment: {}", vehicleType);
        } else {
            log.warn("⚠️ No vehicle type found in assignment, using default");
            vehicleType = "car"; // Default fallback
        }

        // Use actual distance from client if provided, otherwise calculate from addresses
        java.math.BigDecimal distanceKm;
        if (actualDistanceKm != null && actualDistanceKm.compareTo(java.math.BigDecimal.ZERO) > 0) {
            distanceKm = actualDistanceKm;
            log.info("📏 Using client-provided distance: {} km", distanceKm);
        } else {
            // Calculate distance from delivery address back to pickup address (return route) with vehicle type
            distanceKm = contractService.calculateDistanceKm(
                    order.getDeliveryAddress(), 
                    order.getPickupAddress(),
                    vehicleType
            );
            log.info("📏 Calculated return distance from addresses with vehicle type {}: {} km", vehicleType, distanceKm);
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

        log.info("🚗 Vehicle count map for return (sizeRuleId -> count): {}", vehicleCountMap);

        // Calculate total weight of selected packages for logging
        double totalWeight = selectedOrderDetails.stream()
                .mapToDouble(od -> od.getWeightBaseUnit() != null ? od.getWeightBaseUnit().doubleValue() : 0.0)
                .sum();
        log.info("⚖️ Total weight of packages to return: {} kg", totalWeight);

        // Calculate return shipping fee using contract pricing logic
        PriceCalculationResponse priceResponse =
                contractService.calculateTotalPrice(contract, distanceKm, vehicleCountMap);

        log.info("💵 Calculated return shipping fee: {} VND for {} package(s)", 
                 priceResponse.getTotalPrice(), selectedOrderDetails.size());

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
        log.info("⚙️ Staff processing ORDER_REJECTION issue: {}", request.issueId());

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
            log.info("💵 Staff adjusted return fee to: {}", request.adjustedReturnFee());
        }

        // Set return shipping fee (use calculated fee)
        issue.setReturnShippingFee(feeResponse.calculatedFee());

        // Get final fee for transaction
        java.math.BigDecimal finalFee = issue.getAdjustedReturnFee() != null 
                ? issue.getAdjustedReturnFee() 
                : issue.getReturnShippingFee();

        log.info("💰 Return shipping fee calculated: {} VND (Staff will create payment link separately)", finalFee);

        // Get order and contract for reference (needed for journey creation)
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(
                issue.getVehicleAssignmentEntity()
        );
        var order = orderDetails.get(0).getOrderEntity();
        var contract = contractEntityService.getContractByOrderId(order.getId())
                .orElseThrow(() -> new IllegalStateException("No contract found for order: " + order.getId()));

        // NOTE: Payment link will be created separately via POST /api/payos-transaction/{contractId}/return-shipping
        // This avoids nested transaction issues and follows the same pattern as deposit/full payment
        log.info("💳 Contract ID for payment: {} - Frontend will call payment endpoint separately", contract.getId());

        // ========== MERGE OLD JOURNEY + RETURN SEGMENTS ==========
        // Step 1: Lấy journey history cũ để copy các segments đã đi
        List<capstone_project.entity.order.order.JourneyHistoryEntity> oldJourneys = 
                journeyHistoryEntityService.findByVehicleAssignmentId(
                        issue.getVehicleAssignmentEntity().getId()
                );
        
        log.info("📜 Found {} existing journeys for vehicle assignment {}", 
                oldJourneys.size(), issue.getVehicleAssignmentEntity().getId());
        
        // Get the latest ACTIVE or COMPLETED journey (should have segments already traveled)
        capstone_project.entity.order.order.JourneyHistoryEntity oldJourney = oldJourneys.stream()
                .filter(j -> "ACTIVE".equals(j.getStatus()) || "COMPLETED".equals(j.getStatus()))
                .findFirst()
                .orElse(oldJourneys.isEmpty() ? null : oldJourneys.get(0));
        
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
            
            log.info("📍 Copying {} segments from old journey, excluding last segment", 
                    oldSegments.size());
            
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
                        .distanceMeters(oldSeg.getDistanceMeters())
                        .status(oldSeg.getStatus()) // Keep original status (COMPLETED/ACTIVE)
                        .estimatedTollFee(oldSeg.getEstimatedTollFee())
                        .pathCoordinatesJson(oldSeg.getPathCoordinatesJson())
                        .tollDetailsJson(oldSeg.getTollDetailsJson())
                        .journeyHistory(returnJourney)
                        .build();
                
                allSegments.add(copiedSegment);
            }
            
            log.info("✅ Copied {} old segments (excluding last delivery→carrier)", 
                    allSegments.size());
        } else {
            log.warn("⚠️ No old journey found, creating return journey with only return segments");
        }
        
        // Step 4: Add return segments (delivery → pickup → carrier)
        log.info("🔄 Adding {} return segments", request.routeSegments().size());
        
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
                    .distanceMeters(segmentInfo.distanceMeters())
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
        
        log.info("📊 Total segments in new return journey: {} (old: {}, return: {})", 
                allSegments.size(), 
                allSegments.size() - request.routeSegments().size(),
                request.routeSegments().size());

        returnJourney.setJourneySegments(allSegments);
        returnJourney = journeyHistoryEntityService.save(returnJourney);
        log.info("🛣️ Created INACTIVE return journey: {}", returnJourney.getId());

        issue.setReturnJourney(returnJourney);

        // Set payment deadline from configuration (driver cannot wait too long)
        issue.setPaymentDeadline(java.time.LocalDateTime.now().plusMinutes(returnPaymentDeadlineMinutes));
        log.info("⏰ Payment deadline set to {} minutes from now ({})", 
                returnPaymentDeadlineMinutes, issue.getPaymentDeadline());

        // Update issue status to IN_PROGRESS
        issue.setStatus(IssueEnum.IN_PROGRESS.name());

        // Save issue
        issue = issueEntityService.save(issue);
        log.info("✅ ORDER_REJECTION issue processed, status: IN_PROGRESS");

        // Broadcast WebSocket notification for issue status change
        // This ensures customer order detail page receives update and refetches
        try {
            GetBasicIssueResponse issueResponse = issueMapper.toIssueBasicResponse(issue);
            issueWebSocketService.broadcastIssueStatusChange(issueResponse);
            log.info("📢 Broadcasted issue status change via WebSocket for issue: {}", issue.getId());
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
        log.info("💳 Contract ID: {} - Customer will create payment via frontend when clicking 'Pay' button", 
                contract.getId());

        // Return detail response
        return getOrderRejectionDetail(issue.getId());
    }

    @Override
    public capstone_project.dtos.response.issue.OrderRejectionDetailResponse getOrderRejectionDetail(UUID issueId) {
        log.info("📋 Getting ORDER_REJECTION detail for issue: {}", issueId);

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

        // Get order details
        List<OrderDetailEntity> orderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(
                issue.getVehicleAssignmentEntity()
        );

        // Map to response
        List<capstone_project.dtos.response.issue.OrderDetailForIssueResponse> affectedOrderDetails = 
                orderDetails.stream()
                .map(od -> new capstone_project.dtos.response.issue.OrderDetailForIssueResponse(
                        od.getTrackingCode(),
                        od.getDescription(),
                        od.getWeightBaseUnit(),
                        od.getUnit()
                ))
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
            // Calculate total distance from segments
            Double totalDistance = journey.getJourneySegments() != null
                    ? journey.getJourneySegments().stream()
                            .mapToInt(seg -> seg.getDistanceMeters() != null ? seg.getDistanceMeters() : 0)
                            .sum() / 1000.0 // Convert meters to km
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
            capstone_project.dtos.request.issue.ConfirmReturnDeliveryRequest request
    ) {
        log.info("📦 Driver confirming return delivery for issue: {}", request.issueId());

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

        // Validate issue is in IN_PROGRESS status
        if (!IssueEnum.IN_PROGRESS.name().equals(issue.getStatus())) {
            throw new IllegalStateException("Issue must be IN_PROGRESS to confirm return delivery");
        }

        // Create IssueImageEntity for each return delivery image
        if (request.returnDeliveryImages() != null && !request.returnDeliveryImages().isEmpty()) {
            for (String imageUrl : request.returnDeliveryImages()) {
                IssueImageEntity imageEntity = IssueImageEntity.builder()
                        .imageUrl(imageUrl)
                        .description("RETURN_DELIVERY")
                        .issueEntity(issue)
                        .build();
                issueImageEntityService.save(imageEntity);
                log.info("📸 Saved return delivery image for issue: {}", issue.getId());
            }
        }

        // Update ONLY selected order details to RETURNED status
        List<OrderDetailEntity> selectedOrderDetails = issue.getOrderDetails() != null 
                ? issue.getOrderDetails() 
                : java.util.Collections.emptyList();
        
        if (selectedOrderDetails.isEmpty()) {
            throw new IllegalStateException("No order details found in issue for return");
        }
        
        selectedOrderDetails.forEach(orderDetail -> {
            orderDetail.setStatus(OrderDetailStatusEnum.RETURNED.name());
            orderDetailEntityService.save(orderDetail);
            log.info("✅ Updated order detail {} ({}) to RETURNED", 
                     orderDetail.getId(), 
                     orderDetail.getTrackingCode());
        });

        // Update Order status based on ALL OrderDetails using priority logic
        // CRITICAL: Multi-trip support - check all order details across all trips
        // Priority: DELIVERED > IN_TROUBLES > COMPENSATION > RETURNING > RETURNED
        var vehicleAssignment = issue.getVehicleAssignmentEntity();
        if (vehicleAssignment != null) {
            Optional<OrderEntity> orderOpt = orderEntityService.findVehicleAssignmentOrder(vehicleAssignment.getId());
            if (orderOpt.isPresent()) {
                OrderEntity order = orderOpt.get();
                String oldStatus = order.getStatus();
                
                // Get ALL orderDetails of this Order (across all trips)
                var allDetailsInOrder = orderDetailEntityService.findOrderDetailEntitiesByOrderEntityId(order.getId());
                
                long deliveredCount = allDetailsInOrder.stream()
                        .filter(od -> "DELIVERED".equals(od.getStatus())).count();
                long successfulCount = allDetailsInOrder.stream()
                        .filter(od -> "SUCCESSFUL".equals(od.getStatus())).count();
                long inTroublesCount = allDetailsInOrder.stream()
                        .filter(od -> "IN_TROUBLES".equals(od.getStatus())).count();
                long compensationCount = allDetailsInOrder.stream()
                        .filter(od -> "COMPENSATION".equals(od.getStatus())).count();
                long returningCount = allDetailsInOrder.stream()
                        .filter(od -> "RETURNING".equals(od.getStatus())).count();
                long returnedCount = allDetailsInOrder.stream()
                        .filter(od -> "RETURNED".equals(od.getStatus())).count();
                
                String newStatus;
                String reason;
                
                // Apply priority logic
                if (deliveredCount > 0 || successfulCount > 0) {
                    // Has delivered packages → SUCCESSFUL
                    newStatus = capstone_project.common.enums.OrderStatusEnum.SUCCESSFUL.name();
                    reason = String.format("%d delivered/successful, %d returned", 
                            deliveredCount + successfulCount, returnedCount);
                } else if (inTroublesCount > 0) {
                    // No delivered, has troubles → IN_TROUBLES
                    newStatus = capstone_project.common.enums.OrderStatusEnum.IN_TROUBLES.name();
                    reason = String.format("%d in troubles, %d returned", inTroublesCount, returnedCount);
                } else if (compensationCount > 0) {
                    // No delivered, no troubles, has compensation → COMPENSATION
                    newStatus = capstone_project.common.enums.OrderStatusEnum.COMPENSATION.name();
                    reason = String.format("%d compensated, %d returned", compensationCount, returnedCount);
                } else if (returningCount > 0) {
                    // Still has packages returning
                    newStatus = capstone_project.common.enums.OrderStatusEnum.RETURNING.name();
                    reason = String.format("%d returning, %d returned", returningCount, returnedCount);
                } else if (returnedCount == allDetailsInOrder.size()) {
                    // All returned → RETURNED
                    newStatus = capstone_project.common.enums.OrderStatusEnum.RETURNED.name();
                    reason = String.format("All %d packages returned", returnedCount);
                } else {
                    // Fallback
                    newStatus = oldStatus;
                    reason = "No status change needed";
                }
                
                if (!newStatus.equals(oldStatus)) {
                    order.setStatus(newStatus);
                    orderEntityService.save(order);
                    log.info("✅ Order {} status updated from {} to {} (driver confirmed return delivery, multi-trip: {})", 
                             order.getId(), oldStatus, newStatus, reason);
                } else {
                    log.info("ℹ️ Order {} status unchanged: {} ({})", order.getId(), oldStatus, reason);
                }
            }
        }

        // Mark issue as RESOLVED
        issue.setStatus(IssueEnum.RESOLVED.name());
        issue.setResolvedAt(java.time.LocalDateTime.now());

        // Save issue
        issue = issueEntityService.save(issue);
        log.info("✅ ORDER_REJECTION issue resolved, goods returned to pickup");

        return getBasicIssue(issue.getId());
    }

    @Override
    @Transactional
    public capstone_project.dtos.response.order.transaction.TransactionResponse createReturnPaymentTransaction(UUID issueId) {
        log.info("💳 Customer creating return shipping payment transaction for issue: {}", issueId);
        
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
        
        log.info("💳 Creating return shipping payment for contract {} with amount {}", 
                contract.getId(), finalFee);
        
        // Create transaction using PayOS service
        capstone_project.dtos.response.order.transaction.TransactionResponse transactionResponse = 
                payOSTransactionService.createReturnShippingTransaction(
                        contract.getId(), 
                        finalFee, 
                        issue.getId()
                );
        
        log.info("✅ Created return shipping transaction: {} for issue: {}", transactionResponse.id(), issue.getId());
        
        // Transaction already has issueId set, no need to link back
        return transactionResponse;
    }

}
