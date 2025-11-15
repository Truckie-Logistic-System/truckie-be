package capstone_project.service.services.refund.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.enums.IssueEnum;
import capstone_project.common.enums.OrderDetailStatusEnum;
import capstone_project.common.enums.OrderStatusEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.common.utils.UserContextUtils;
import capstone_project.dtos.request.refund.ProcessRefundRequest;
import capstone_project.dtos.response.refund.GetRefundResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.RefundEntity;
import capstone_project.repository.entityServices.issue.IssueEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.repository.entityServices.order.order.OrderEntityService;
import capstone_project.repository.entityServices.refund.RefundEntityService;
import capstone_project.service.mapper.issue.IssueMapper;
import capstone_project.service.mapper.refund.RefundMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.refund.RefundService;
import capstone_project.service.services.websocket.IssueWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {
    private final RefundEntityService refundEntityService;
    private final IssueEntityService issueEntityService;
    private final OrderDetailEntityService orderDetailEntityService;
    private final OrderEntityService orderEntityService;
    private final CloudinaryService cloudinaryService;
    private final RefundMapper refundMapper;
    private final IssueMapper issueMapper;
    private final IssueWebSocketService issueWebSocketService;
    private final UserContextUtils userContextUtils;

    @Override
    @Transactional
    public GetRefundResponse processRefund(ProcessRefundRequest request, MultipartFile bankTransferImage) {
        log.info("Processing refund for issue: {}", request.issueId());

        // Validate issue exists and is OPEN or IN_PROGRESS
        IssueEntity issue = issueEntityService.findEntityById(request.issueId())
                .orElseThrow(() -> new NotFoundException(ErrorEnum.ISSUE_NOT_FOUND));

        if (!IssueEnum.OPEN.name().equals(issue.getStatus()) && 
            !IssueEnum.IN_PROGRESS.name().equals(issue.getStatus())) {
            throw new IllegalStateException("Issue must be OPEN or IN_PROGRESS to process refund");
        }

        // Get current staff from UserContextUtils
        UserEntity staff = userContextUtils.getCurrentUser();

        // Upload bank transfer image to Cloudinary
        String bankTransferImageUrl = null;
        if (bankTransferImage != null && !bankTransferImage.isEmpty()) {
            try {
                log.info("📤 Uploading refund proof image to Cloudinary...");
                var uploadResult = cloudinaryService.uploadFile(bankTransferImage.getBytes(), 
                        "refund_" + System.currentTimeMillis(), 
                        "refund_proofs");
                bankTransferImageUrl = uploadResult.get("secure_url").toString();
                log.info("✅ Refund proof image uploaded: {}", bankTransferImageUrl);
            } catch (IOException e) {
                log.error("❌ Error uploading refund image to Cloudinary: {}", e.getMessage());
                throw new RuntimeException("Failed to upload refund image", e);
            }
        } else {
            log.warn("⚠️ No bank transfer image provided for refund");
        }

        // Create refund entity
        RefundEntity refund = RefundEntity.builder()
                .refundAmount(request.refundAmount())
                .bankTransferImage(bankTransferImageUrl)
                .bankName(request.bankName())
                .accountNumber(request.accountNumber())
                .accountHolderName(request.accountHolderName())
                .transactionCode(request.transactionCode())
                .refundDate(LocalDateTime.now())
                .notes(request.notes())
                .issueEntity(issue)
                .processedByStaff(staff)
                .build();

        // Save refund
        RefundEntity savedRefund = refundEntityService.save(refund);
        log.info("✅ Refund saved with ID: {}", savedRefund.getId());

        // Update order details in this vehicle assignment
        if (issue.getVehicleAssignmentEntity() != null) {
            var allOrderDetails = orderDetailEntityService.findByVehicleAssignmentEntity(
                    issue.getVehicleAssignmentEntity());
            
            for (OrderDetailEntity od : allOrderDetails) {
                // Kiện bị hư (linked to issue) → COMPENSATION
                if (od.getIssueEntity() != null && od.getIssueEntity().getId().equals(issue.getId())) {
                    od.setStatus(OrderDetailStatusEnum.COMPENSATION.name());
                    orderDetailEntityService.save(od);
                    log.info("✅ Order detail {} status updated to COMPENSATION", od.getId());
                }
                // Kiện còn lại (không bị hư và đang IN_TROUBLES) → DELIVERED
                else if (OrderDetailStatusEnum.IN_TROUBLES.name().equals(od.getStatus())) {
                    od.setStatus(OrderDetailStatusEnum.DELIVERED.name());
                    orderDetailEntityService.save(od);
                    log.info("✅ Order detail {} status updated to DELIVERED", od.getId());
                }
            }
            log.info("✅ Updated order details: damaged → COMPENSATION, others → DELIVERED");
            
            // Check if ALL order details in the affected orders are COMPENSATION
            // If so, update order status to COMPENSATION
            var affectedOrderIds = allOrderDetails.stream()
                    .map(od -> od.getOrderEntity().getId())
                    .distinct()
                    .toList();
            
            for (java.util.UUID orderId : affectedOrderIds) {
                var orderDetailsInOrder = orderDetailEntityService.findAll().stream()
                        .filter(od -> od.getOrderEntity() != null && 
                                      od.getOrderEntity().getId().equals(orderId))
                        .toList();
                
                boolean allCompensated = orderDetailsInOrder.stream()
                        .allMatch(od -> OrderDetailStatusEnum.COMPENSATION.name().equals(od.getStatus()));
                
                if (allCompensated) {
                    var order = orderEntityService.findEntityById(orderId)
                            .orElseThrow(() -> new NotFoundException(ErrorEnum.NOT_FOUND));
                    order.setStatus(OrderStatusEnum.COMPENSATION.name());
                    orderEntityService.save(order);
                    log.info("✅ Order {} status updated to COMPENSATION (all order details compensated)", orderId);
                }
            }
        }

        // Update issue status to RESOLVED
        issue.setStatus(IssueEnum.RESOLVED.name());
        issue.setResolvedAt(LocalDateTime.now());
        issue.setStaff(staff);
        IssueEntity updatedIssue = issueEntityService.save(issue);
        log.info("✅ Issue {} status updated to RESOLVED", issue.getId());

        // NOTE: Driver already continued trip after reporting damage
        // No notification needed as driver doesn't need to wait
        log.info("ℹ️ Driver already continued trip, refund processed successfully");

        log.info("Refund processed successfully for issue: {}", request.issueId());
        return refundMapper.toRefundResponse(refund);
    }

    @Override
    public GetRefundResponse getRefundByIssueId(UUID issueId) {
        RefundEntity refund = refundEntityService.findByIssueId(issueId)
                .orElseThrow(() -> new NotFoundException(ErrorEnum.REFUND_NOT_FOUND));
        return refundMapper.toRefundResponse(refund);
    }
}
