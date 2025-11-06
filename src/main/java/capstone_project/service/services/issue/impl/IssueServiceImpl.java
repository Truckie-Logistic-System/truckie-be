package capstone_project.service.services.issue.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.IssueEnum;
import capstone_project.common.enums.IssueCategoryEnum;
import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
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

import java.util.List;
import java.util.UUID;

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


    @Override
    public GetBasicIssueResponse getBasicIssue(UUID issueId) {
        IssueEntity getIssue = issueEntityService.findEntityById(issueId).get();
        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(getIssue);
        
        // Populate vehicle assignment với nested objects
        if (getIssue.getVehicleAssignmentEntity() != null) {
            var vehicleAssignment = getIssue.getVehicleAssignmentEntity();
            var enrichedVA = mapVehicleAssignmentWithDetails(vehicleAssignment);
            
            // Create new response with enriched vehicle assignment
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
                response.newSealConfirmedAt()
            );
        }
        
        return response;
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
        return issueMapper.toIssueBasicResponses(issueEntityService.findAll());
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

        // Convert sang response
        GetBasicIssueResponse response = issueMapper.toIssueBasicResponse(saved);

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
        List<IssueEntity> allIssues = issueEntityService.findAll();
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

}
