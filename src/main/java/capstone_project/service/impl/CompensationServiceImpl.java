package capstone_project.service.impl;

import capstone_project.dtos.request.issue.CompensationAssessmentRequest;
import capstone_project.dtos.response.issue.CompensationAssessmentListResponse;
import capstone_project.dtos.response.issue.CompensationDetailResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.issue.IssueCompensationAssessmentEntity;
import capstone_project.entity.issue.IssueEntity;
import capstone_project.entity.order.order.OrderDetailEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.order.order.RefundEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.IssueCompensationAssessmentRepository;
import capstone_project.repository.repositories.issue.IssueRepository;
import capstone_project.repository.repositories.refund.RefundRepository;
import capstone_project.repository.repositories.auth.UserRepository;
import capstone_project.repository.repositories.user.CustomerRepository;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.order.order.OrderDetailEntityService;
import capstone_project.service.CompensationService;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.common.utils.UserContextUtils;
import static capstone_project.common.enums.UserStatusEnum.BANNED;
import static capstone_project.common.enums.IssueEnum.CLOSED_FRAUD;
import static capstone_project.common.enums.IssueEnum.RESOLVED;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompensationServiceImpl implements CompensationService {

    private final IssueRepository issueRepository;
    private final IssueCompensationAssessmentRepository assessmentRepository;
    private final RefundRepository refundRepository;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final OrderDetailEntityService orderDetailEntityService;
    private final CloudinaryService cloudinaryService;
    private final ContractEntityService contractEntityService;
    private final UserContextUtils userContextUtils;
    
    private static final BigDecimal TEN = BigDecimal.valueOf(10);
    
    @Override
    public List<CompensationAssessmentListResponse> getAllCompensationAssessments() {
        List<IssueCompensationAssessmentEntity> assessments = assessmentRepository.findAll();
        
        // Sort by createdAt DESC
        assessments.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        
        return assessments.stream()
                .map(this::mapToListResponse)
                .collect(Collectors.toList());
    }
    
    private CompensationAssessmentListResponse mapToListResponse(IssueCompensationAssessmentEntity assessment) {
        IssueEntity issue = assessment.getIssue();
        UserEntity createdBy = assessment.getCreatedBy();
        
        return CompensationAssessmentListResponse.builder()
                .id(assessment.getId())
                .issueType(assessment.getIssueType())
                .hasDocuments(assessment.getHasDocuments())
                .documentValue(assessment.getDocumentValue())
                .estimatedMarketValue(assessment.getEstimatedMarketValue())
                .assessmentRate(assessment.getAssessmentRate())
                .compensationByPolicy(assessment.getCompensationByPolicy())
                .finalCompensation(assessment.getFinalCompensation())
                .fraudDetected(assessment.getFraudDetected())
                .fraudReason(assessment.getFraudReason())
                .createdAt(assessment.getCreatedAt())
                .updatedAt(assessment.getUpdatedAt())
                .issue(issue != null ? CompensationAssessmentListResponse.IssueInfo.builder()
                        .id(issue.getId())
                        .issueTypeName(issue.getIssueTypeEntity() != null ? 
                                issue.getIssueTypeEntity().getIssueTypeName() : null)
                        .issueCategory(issue.getIssueTypeEntity() != null ? 
                                issue.getIssueTypeEntity().getIssueCategory() : null)
                        .status(issue.getStatus())
                        .description(issue.getDescription())
                        .reportedAt(issue.getReportedAt())
                        .resolvedAt(issue.getResolvedAt())
                        .build() : null)
                .createdByStaff(createdBy != null ? CompensationAssessmentListResponse.StaffInfo.builder()
                        .id(createdBy.getId())
                        .fullName(createdBy.getFullName())
                        .email(createdBy.getEmail())
                        .build() : null)
                .build();
    }
    
    @Override
    public CompensationDetailResponse getCompensationDetail(UUID issueId) {
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with ID: " + issueId));
        
        OrderDetailEntity orderDetail = resolvePrimaryOrderDetail(issue);
        OrderEntity order = orderDetail.getOrderEntity();
        
        IssueCompensationAssessmentEntity assessment = assessmentRepository
                .findByIssue(issue)
                .orElse(null);
        
        RefundEntity refund = refundRepository.findByIssueEntityId(issueId).orElse(null);
        
        return buildCompensationDetailResponse(issue, orderDetail, order, assessment, refund);
    }

    @Override
    @Transactional
    public CompensationDetailResponse resolveCompensation(CompensationAssessmentRequest request) {
        UUID issueId = request.getIssueId();
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with ID: " + issueId));
        
        OrderDetailEntity orderDetail = resolvePrimaryOrderDetail(issue);
        OrderEntity order = orderDetail.getOrderEntity();
        
        // Create or update assessment
        IssueCompensationAssessmentEntity assessment = createOrUpdateAssessment(issue, request);
        
        // Create or update refund if needed
        RefundEntity refund = null;
        if (request.getRefund() != null && Boolean.TRUE.equals(request.getRefund().getCreateOrUpdate())) {
            refund = createOrUpdateRefund(issue, assessment, request.getRefund());
        }
        
        // Update issue status based on fraud detection
        if (Boolean.TRUE.equals(request.getFraudDetected())) {
            issue.setStatus(CLOSED_FRAUD.name());
            issue.setResolvedAt(LocalDateTime.now());
            
            // Ban customer account when fraud is detected
            banCustomerAccount(issue);
            
            log.info("Issue {} marked as {} and customer account banned", issueId, CLOSED_FRAUD.name());
        } else {
            issue.setStatus(RESOLVED.name());
            issue.setResolvedAt(LocalDateTime.now());
            log.info("Issue {} resolved with assessment and refund", issueId);
        }
        
        issueRepository.save(issue);
        
        return buildCompensationDetailResponse(issue, orderDetail, order, assessment, refund);
    }

    @Override
    public CompensationDetailResponse uploadDocumentImages(UUID issueId, String[] imageUrls) {
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with ID: " + issueId));
        
        IssueCompensationAssessmentEntity assessment = assessmentRepository
                .findByIssue(issue)
                .orElseThrow(() -> new RuntimeException("Assessment not found for issue: " + issueId));
        
        // Update document images
        String currentImages = assessment.getDocumentImages();
        Set<String> imageSet = new HashSet<>();
        
        // Add existing images
        if (currentImages != null && !currentImages.isEmpty()) {
            imageSet.addAll(Arrays.asList(currentImages.split(",")));
        }
        
        // Add new images
        if (imageUrls != null) {
            imageSet.addAll(Arrays.asList(imageUrls));
        }
        
        // Update assessment
        assessment.setDocumentImages(String.join(",", imageSet));
        assessment.setUpdatedAt(LocalDateTime.now());
        assessmentRepository.save(assessment);
        
        // Return updated detail
        OrderDetailEntity orderDetail = resolvePrimaryOrderDetail(issue);
        OrderEntity order = orderDetail.getOrderEntity();
        RefundEntity refund = refundRepository.findByIssueEntityId(issueId).orElse(null);
        
        return buildCompensationDetailResponse(issue, orderDetail, order, assessment, refund);
    }
    
    /**
     * Create or update compensation assessment
     */
    private IssueCompensationAssessmentEntity createOrUpdateAssessment(
            IssueEntity issue, 
            CompensationAssessmentRequest request) {
        
        UserEntity currentUser = userContextUtils.getCurrentUser();
        
        IssueCompensationAssessmentEntity assessment = assessmentRepository
                .findByIssue(issue)
                .orElse(IssueCompensationAssessmentEntity.builder()
                        .issue(issue)
                        .issueType(request.getIssueType())
                        .createdAt(LocalDateTime.now())
                        .createdBy(currentUser)
                        .build());
        
        // Set assessment fields
        assessment.setHasDocuments(request.getHasDocuments());
        assessment.setDocumentValue(request.getDocumentValue());
        assessment.setEstimatedMarketValue(request.getEstimatedMarketValue());
        
        // Handle assessment rate (from percent if provided)
        if (request.getAssessmentRatePercent() != null) {
            assessment.setAssessmentRateFromPercent(request.getAssessmentRatePercent());
        } else if (request.getAssessmentRate() != null) {
            assessment.setAssessmentRate(request.getAssessmentRate());
        }
        
        // Calculate compensation by policy
        OrderDetailEntity orderDetail = resolvePrimaryOrderDetail(issue);
        OrderEntity order = orderDetail.getOrderEntity();
        
        CompensationDetailResponse.CompensationBreakdown breakdown = 
                calculateCompensationBreakdown(orderDetail, order, assessment);
        
        // Set compensation values
        assessment.setCompensationByPolicy(breakdown.getTotalCompensation());
        
        // Use provided final compensation or default to policy calculation
        if (request.getFinalCompensation() != null) {
            assessment.setFinalCompensation(request.getFinalCompensation());
        } else {
            assessment.setFinalCompensation(breakdown.getTotalCompensation());
        }
        
        // Set notes
        assessment.setStaffNotes(request.getStaffNotes());
        assessment.setAdjustReason(request.getAdjustReason());
        assessment.setHandlerNotes(request.getHandlerNotes());
        
        // Set fraud detection
        assessment.setFraudDetected(request.getFraudDetected());
        assessment.setFraudReason(request.getFraudReason());
        
        // Set document images (uploaded via Cloudinary)
        if (request.getDocumentImages() != null && !request.getDocumentImages().isEmpty()) {
            assessment.setDocumentImages(String.join(",", request.getDocumentImages()));
        }
        
        // Update timestamp
        assessment.setUpdatedAt(LocalDateTime.now());
        
        return assessmentRepository.save(assessment);
    }
    
    /**
     * Create or update refund
     */
    private RefundEntity createOrUpdateRefund(
            IssueEntity issue,
            IssueCompensationAssessmentEntity assessment,
            CompensationAssessmentRequest.RefundRequest refundRequest) {
        
        UserEntity currentUser = userContextUtils.getCurrentUser();
        
        RefundEntity refund = refundRepository.findByIssueEntityId(issue.getId())
                .orElse(RefundEntity.builder()
                        .issueEntity(issue)
                        .sourceType("ISSUE_" + assessment.getIssueType())
                        .sourceId(assessment.getId())
                        .processedByStaff(currentUser)
                        .build());
        
        refund.setRefundAmount(refundRequest.getRefundAmount());
        refund.setBankName(refundRequest.getBankName());
        refund.setAccountNumber(refundRequest.getAccountNumber());
        refund.setAccountHolderName(refundRequest.getAccountHolderName());
        refund.setTransactionCode(refundRequest.getTransactionCode());
        refund.setBankTransferImage(refundRequest.getBankTransferImage());
        refund.setNotes(refundRequest.getNotes());
        refund.setRefundDate(LocalDateTime.now());
        refund.setProcessedByStaff(currentUser);
        
        return refundRepository.save(refund);
    }
    
    /**
     * Ban customer account when fraud is detected
     */
    private void banCustomerAccount(IssueEntity issue) {
        try {
            // Get customer from order
            OrderDetailEntity orderDetail = resolvePrimaryOrderDetail(issue);
            OrderEntity order = orderDetail.getOrderEntity();
            CustomerEntity customer = order.getSender();
            
            if (customer != null) {
                // Set customer status to BANNED (using enum)
                customer.setStatus(BANNED.name());
                customerRepository.save(customer);
                
                // Also ban the associated user account
                UserEntity user = customer.getUser();
                if (user != null) {
                    user.setStatus(BANNED.name());
                    userRepository.save(user);
                }
                
                log.info("Customer {} and associated user {} banned (status set to {}) due to fraud in issue {}", 
                    customer.getId(), user != null ? user.getId() : "null", BANNED.name(), issue.getId());
            } else {
                log.warn("Cannot ban customer account - customer not found for issue {}", issue.getId());
            }
        } catch (Exception e) {
            log.error("Failed to ban customer account for issue {}: {}", issue.getId(), e.getMessage());
            // Don't throw exception - fraud detection should still succeed even if ban fails
        }
    }
    
    /**
     * Build compensation detail response
     */
    private CompensationDetailResponse buildCompensationDetailResponse(
            IssueEntity issue,
            OrderDetailEntity orderDetail,
            OrderEntity order,
            IssueCompensationAssessmentEntity assessment,
            RefundEntity refund) {
        
        // Calculate weight for Pro-rata
        BigDecimal packageWeight = orderDetail.getWeightTons() != null ? orderDetail.getWeightTons() : BigDecimal.ONE;
        BigDecimal totalWeight = BigDecimal.ZERO;
        if (order.getOrderDetailEntities() != null) {
            for (OrderDetailEntity od : order.getOrderDetailEntities()) {
                if (od.getWeightTons() != null) {
                    totalWeight = totalWeight.add(od.getWeightTons());
                }
            }
        }
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            totalWeight = packageWeight; // Fallback: single package = total
        }
        
        // Build order context
        CompensationDetailResponse.OrderContextInfo orderContext = CompensationDetailResponse.OrderContextInfo.builder()
                .orderId(order.getId())
                .orderCode(order.getOrderCode())
                .orderDetailId(orderDetail.getId())
                .trackingCode(orderDetail.getTrackingCode())
                .packageDescription(orderDetail.getDescription())
                .orderDetailStatus(orderDetail.getStatus())
                .declaredValue(orderDetail.getDeclaredValue())
                .hasInsurance(order.getHasInsurance())
                .category(order.getCategory() != null ? order.getCategory().getCategoryName().name() : "NORMAL")
                .categoryDescription(getCategoryDescription(order.getCategory() != null ? order.getCategory().getCategoryName().name() : "NORMAL"))
                .transportFee(resolveTransportFee(order))
                .customerName(order.getSender() != null ? order.getSender().getRepresentativeName() : null)
                .customerPhone(order.getSender() != null ? order.getSender().getRepresentativePhone() : null)
                .weight(packageWeight)
                .totalWeight(totalWeight)
                .build();
        
        // Build policy info
        BigDecimal transportFee = orderContext.getTransportFee();
        CompensationDetailResponse.PolicyInfo policyInfo = CompensationDetailResponse.PolicyInfo.builder()
                .maxCompensationWithoutDocs(transportFee.multiply(TEN).setScale(2, RoundingMode.HALF_UP))
                .build();
        
        // Build assessment info
        CompensationDetailResponse.AssessmentInfo assessmentInfo = null;
        if (assessment != null) {
            List<String> documentImages = new ArrayList<>();
            if (assessment.getDocumentImages() != null && !assessment.getDocumentImages().isEmpty()) {
                documentImages = Arrays.asList(assessment.getDocumentImages().split(","))
                    .stream()
                    .map(url -> url.replace("http://", "https://"))
                    .collect(Collectors.toList());
            }
            
            assessmentInfo = CompensationDetailResponse.AssessmentInfo.builder()
                    .assessmentId(assessment.getId())
                    .hasDocuments(assessment.getHasDocuments())
                    .documentValue(assessment.getDocumentValue())
                    .estimatedMarketValue(assessment.getEstimatedMarketValue())
                    .documentImages(documentImages)
                    .assessmentRate(assessment.getAssessmentRate())
                    .assessmentRatePercent(assessment.getAssessmentRate().multiply(new BigDecimal("100")).setScale(2) + "%")
                    .compensationByPolicy(assessment.getCompensationByPolicy())
                    .finalCompensation(assessment.getFinalCompensation())
                    .staffNotes(assessment.getStaffNotes())
                    .adjustReason(assessment.getAdjustReason())
                    .handlerNotes(assessment.getHandlerNotes())
                    .fraudDetected(assessment.getFraudDetected())
                    .fraudReason(assessment.getFraudReason())
                    .createdAt(assessment.getCreatedAt())
                    .updatedAt(assessment.getUpdatedAt())
                    .createdByName(assessment.getCreatedBy() != null ? assessment.getCreatedBy().getFullName() : null)
                    .build();
        }
        
        // Build refund info
        CompensationDetailResponse.RefundInfo refundInfo = null;
        if (refund != null) {
            refundInfo = CompensationDetailResponse.RefundInfo.builder()
                    .refundId(refund.getId())
                    .refundAmount(refund.getRefundAmount())
                    .bankName(refund.getBankName())
                    .accountNumber(refund.getAccountNumber())
                    .accountHolderName(refund.getAccountHolderName())
                    .transactionCode(refund.getTransactionCode())
                    .bankTransferImage(refund.getBankTransferImage() != null 
                        ? refund.getBankTransferImage().replace("http://", "https://") 
                        : null)
                    .refundDate(refund.getRefundDate())
                    .notes(refund.getNotes())
                    .processedByStaffName(refund.getProcessedByStaff() != null 
                            ? refund.getProcessedByStaff().getFullName() 
                            : null)
                    .build();
        }
        
        // Build compensation breakdown
        CompensationDetailResponse.CompensationBreakdown compensationBreakdown = null;
        if (assessment != null) {
            compensationBreakdown = calculateCompensationBreakdown(orderDetail, order, assessment);
        }
        
        // Build final response
        return CompensationDetailResponse.builder()
                .issueId(issue.getId())
                .issueType(issue.getIssueTypeEntity() != null ? issue.getIssueTypeEntity().getIssueTypeName() : null)
                .issueStatus(issue.getStatus())
                .description(issue.getDescription())
                .evidenceImages(issue.getIssueImages() != null && !issue.getIssueImages().isEmpty()
                        ? issue.getIssueImages().stream()
                                .map(img -> img.getImageUrl().replace("http://", "https://"))
                                .collect(Collectors.toList())
                        : Collections.emptyList())
                .reportedAt(issue.getReportedAt())
                .reportedBy(issue.getStaff() != null ? issue.getStaff().getFullName() : null)
                .orderContext(orderContext)
                .policyInfo(policyInfo)
                .assessment(assessmentInfo)
                .refundInfo(refundInfo)
                .compensationBreakdown(compensationBreakdown)
                .build();
    }
    
    /**
     * Calculate compensation breakdown based on issue type and assessment
     */
    private CompensationDetailResponse.CompensationBreakdown calculateCompensationBreakdown(
            OrderDetailEntity orderDetail,
            OrderEntity order,
            IssueCompensationAssessmentEntity assessment) {
        
        if (assessment == null) {
            return CompensationDetailResponse.CompensationBreakdown.builder()
                    .goodsCompensation(BigDecimal.ZERO)
                    .freightRefund(BigDecimal.ZERO)
                    .totalCompensation(BigDecimal.ZERO)
                    .legalLimit(BigDecimal.ZERO)
                    .compensationCase("PENDING")
                    .explanation("Chưa có thông tin thẩm định")
                    .build();
        }
        
        if (assessment.isDamageAssessment()) {
            return calculateDamageCompensationBreakdown(orderDetail, order, assessment);
        } else if (assessment.isOffRouteAssessment()) {
            return calculateOffRouteCompensationBreakdown(orderDetail, order, assessment);
        }
        
        // Default case
        return CompensationDetailResponse.CompensationBreakdown.builder()
                .goodsCompensation(BigDecimal.ZERO)
                .freightRefund(BigDecimal.ZERO)
                .totalCompensation(BigDecimal.ZERO)
                .legalLimit(BigDecimal.ZERO)
                .compensationCase("UNKNOWN")
                .explanation("Không xác định được loại vấn đề")
                .build();
    }
    
    /**
     * Calculate compensation breakdown for DAMAGE issues
     * 
     * Formula:
     * 1. C_hư = C_total × (W_kiện / W_total) × T_hư
     * 2. V_lỗ = V_thực_tế × T_hư  
     * 3. B_hàng = min(V_lỗ, 10 × C_hư) nếu không bảo hiểm/không chứng từ
     *          = min(V_lỗ, V_khai_báo) nếu có bảo hiểm + chứng từ
     * 4. B_tổng = B_hàng + C_hư
     */
    private CompensationDetailResponse.CompensationBreakdown calculateDamageCompensationBreakdown(
            OrderDetailEntity orderDetail,
            OrderEntity order,
            IssueCompensationAssessmentEntity assessment) {
        
        Boolean hasInsurance = order.getHasInsurance() != null ? order.getHasInsurance() : false;
        Boolean hasDocuments = assessment.getHasDocuments() != null ? assessment.getHasDocuments() : false;
        BigDecimal damageRate = assessment.getAssessmentRate() != null ? assessment.getAssessmentRate() : BigDecimal.ZERO;
        BigDecimal declaredValue = orderDetail.getDeclaredValue() != null ? orderDetail.getDeclaredValue() : BigDecimal.ZERO;
        BigDecimal transportFee = resolveTransportFee(order);
        
        // Debug logging for calculation tracing
        log.info("📊 DAMAGE CALC INPUT: hasInsurance={}, hasDocuments={}, damageRate={}, declaredValue={}, transportFee={}, estimatedMarketValue={}, documentValue={}",
            hasInsurance, hasDocuments, damageRate, declaredValue, transportFee,
            assessment.getEstimatedMarketValue(), assessment.getDocumentValue());
        
        // Calculate weight for Pro-rata
        BigDecimal packageWeight = orderDetail.getWeightTons() != null ? orderDetail.getWeightTons() : BigDecimal.ONE;
        BigDecimal totalWeight = BigDecimal.ZERO;
        if (order.getOrderDetailEntities() != null) {
            for (OrderDetailEntity od : order.getOrderDetailEntities()) {
                if (od.getWeightTons() != null) {
                    totalWeight = totalWeight.add(od.getWeightTons());
                }
            }
        }
        if (totalWeight.compareTo(BigDecimal.ZERO) <= 0) {
            totalWeight = packageWeight; // Fallback: single package = total
        }
        
        // Xác định V_thực_tế với logic chi tiết theo chính sách
        BigDecimal actualValue;
        String documentComparisonNote = "";
        
        if (hasDocuments && assessment.getDocumentValue() != null && assessment.getDocumentValue().compareTo(BigDecimal.ZERO) > 0) {
            // Có chứng từ: So sánh với giá trị khai báo
            BigDecimal docValue = assessment.getDocumentValue();
            int comparison = docValue.compareTo(declaredValue);
            
            if (comparison < 0) {
                // Chứng từ THẤP HƠN khai báo → Đền theo chứng từ (thực tế)
                actualValue = docValue;
                documentComparisonNote = String.format("CT < KB (%s < %s): Đền theo CT", 
                        formatCurrency(docValue), formatCurrency(declaredValue));
            } else if (comparison == 0) {
                // Chứng từ BẰNG khai báo → Khớp hoàn toàn
                actualValue = docValue;
                documentComparisonNote = String.format("CT = KB (%s): Khớp hoàn toàn", 
                        formatCurrency(docValue));
            } else {
                // Chứng từ CAO HƠN khai báo → Đền tối đa theo khai báo (Bảo hiểm dưới giá trị)
                actualValue = declaredValue;
                documentComparisonNote = String.format("CT > KB (%s > %s): Đền tối đa theo KB (Under-insured)", 
                        formatCurrency(docValue), formatCurrency(declaredValue));
            }
        } else {
            // Không có chứng từ hợp lệ: dùng estimated hoặc declared
            if (assessment.getEstimatedMarketValue() != null && assessment.getEstimatedMarketValue().compareTo(BigDecimal.ZERO) > 0) {
                actualValue = assessment.getEstimatedMarketValue();
                documentComparisonNote = "Không CT: Dùng giá trị ước tính";
            } else {
                actualValue = declaredValue;
                documentComparisonNote = "Không CT: Dùng giá trị khai báo";
            }
        }
        
        // Bước 1: Tính C_hư = C_total × (W_kiện / W_total) × T_hư
        BigDecimal weightRatio = totalWeight.compareTo(BigDecimal.ZERO) > 0 
                ? packageWeight.divide(totalWeight, 4, RoundingMode.HALF_UP) 
                : BigDecimal.ONE;
        BigDecimal freightRefund = transportFee.multiply(weightRatio).multiply(damageRate).setScale(2, RoundingMode.HALF_UP);
        
        // Bước 2: Tính V_lỗ = V_thực_tế × T_hư
        BigDecimal valueLoss = actualValue.multiply(damageRate).setScale(2, RoundingMode.HALF_UP);
        
        // Debug: Log actualValue and valueLoss
        log.info("📊 DAMAGE CALC STEP: actualValue={}, valueLoss={} (actualValue × damageRate), freightRefund={}", 
            actualValue, valueLoss, freightRefund);
        
        // Bước 3: Tính B_hàng với giới hạn
        // Legal limit = 10 × C_total (tổng cước vận chuyển), KHÔNG phải 10 × C_hư
        BigDecimal legalLimit = transportFee.multiply(TEN).setScale(2, RoundingMode.HALF_UP); // 10 × C_total
        BigDecimal goodsCompensation;
        String compensationCase;
        String explanation;
        
        if (hasInsurance && hasDocuments) {
            // Case 1: Có BH + Có CT → không giới hạn, tối đa = V_khai_báo
            goodsCompensation = valueLoss.compareTo(declaredValue) < 0 ? valueLoss : declaredValue;
            compensationCase = "CASE1_HAS_INS_HAS_DOC";
            explanation = String.format("Có BH + Có CT: %s. Bồi thường theo giá trị thực tế, không giới hạn 10×.", 
                    documentComparisonNote);
        } else if (hasInsurance && !hasDocuments) {
            // Case 2: Có BH + Không CT → BH vô hiệu, áp dụng 10 × C_hư
            goodsCompensation = valueLoss.compareTo(legalLimit) < 0 ? valueLoss : legalLimit;
            compensationCase = "CASE2_HAS_INS_NO_DOC";
            explanation = String.format("Có BH nhưng KHÔNG có CT: Bảo hiểm vô hiệu, áp dụng giới hạn 10× cước (%s)", 
                    formatCurrency(legalLimit));
        } else if (!hasInsurance && hasDocuments) {
            // Case 3: Không BH + Có CT → áp dụng 10 × C_hư
            goodsCompensation = valueLoss.compareTo(legalLimit) < 0 ? valueLoss : legalLimit;
            compensationCase = "CASE3_NO_INS_HAS_DOC";
            explanation = String.format("Không BH + Có CT: %s. Áp dụng giới hạn 10× cước (%s)", 
                    documentComparisonNote, formatCurrency(legalLimit));
        } else {
            // Case 4: Không BH + Không CT → áp dụng 10 × C_hư
            goodsCompensation = valueLoss.compareTo(legalLimit) < 0 ? valueLoss : legalLimit;
            compensationCase = "CASE4_NO_INS_NO_DOC";
            explanation = String.format("Không BH + Không CT: Dùng giá trị ước tính/khai báo. Áp dụng giới hạn 10× cước (%s)", 
                    formatCurrency(legalLimit));
        }
        
        goodsCompensation = goodsCompensation.setScale(2, RoundingMode.HALF_UP);
        
        // Bước 4: B_tổng = B_hàng + C_hư
        BigDecimal totalCompensation = goodsCompensation.add(freightRefund).setScale(2, RoundingMode.HALF_UP);
        
        // Fix: When insurance is invalid (no documents), cap TOTAL compensation at legal limit
        if (hasInsurance && !hasDocuments) {
            BigDecimal legalLimitForTotal = transportFee.multiply(TEN).setScale(2, RoundingMode.HALF_UP);
            if (totalCompensation.compareTo(legalLimitForTotal) > 0) {
                totalCompensation = legalLimitForTotal;
                log.info("🔧 Applied legal limit cap to total compensation: {} -> {}", 
                    goodsCompensation.add(freightRefund), totalCompensation);
            }
        }
        
        log.info("📊 DAMAGE CALC RESULT: goodsCompensation={}, freightRefund={}, totalCompensation={}, legalLimit={}, case={}",
            goodsCompensation, freightRefund, totalCompensation, legalLimit, compensationCase);
        
        return CompensationDetailResponse.CompensationBreakdown.builder()
                .goodsCompensation(goodsCompensation)
                .freightRefund(freightRefund)
                .totalCompensation(totalCompensation)
                .legalLimit(legalLimit)
                .compensationCase(compensationCase)
                .explanation(explanation)
                .build();
    }
    
    /**
     * Calculate compensation breakdown for OFF_ROUTE issues
     * 
     * For OFF_ROUTE:
     * - Always 100% loss (T_hư = 1.0)
     * - No legal limit (10× C_hư) since it's intentional fault
     * - B_hàng = 100% actual value
     * - C_hư = 100% transport fee
     */
    private CompensationDetailResponse.CompensationBreakdown calculateOffRouteCompensationBreakdown(
            OrderDetailEntity orderDetail,
            OrderEntity order,
            IssueCompensationAssessmentEntity assessment) {
        
        Boolean hasDocuments = assessment.getHasDocuments() != null ? assessment.getHasDocuments() : false;
        BigDecimal declaredValue = orderDetail.getDeclaredValue() != null ? orderDetail.getDeclaredValue() : BigDecimal.ZERO;
        BigDecimal transportFee = resolveTransportFee(order);
        
        // Xác định V_thực_tế (100% giá trị hàng) với logic chi tiết theo chính sách
        BigDecimal actualValue;
        String valueExplanation;
        
        if (hasDocuments && assessment.getDocumentValue() != null && assessment.getDocumentValue().compareTo(BigDecimal.ZERO) > 0) {
            // OFF_ROUTE + Có chứng từ: So sánh với giá trị khai báo
            BigDecimal docValue = assessment.getDocumentValue();
            int comparison = docValue.compareTo(declaredValue);
            
            if (comparison < 0) {
                // Chứng từ THẤP HƠN khai báo → Đền theo chứng từ (thực tế)
                actualValue = docValue;
                valueExplanation = String.format("CT < KB (%s < %s): Đền 100%% theo CT", 
                        formatCurrency(docValue), formatCurrency(declaredValue));
            } else if (comparison == 0) {
                // Chứng từ BẰNG khai báo → Khớp hoàn toàn
                actualValue = docValue;
                valueExplanation = String.format("CT = KB (%s): Đền 100%% theo KB (khớp)", 
                        formatCurrency(docValue));
            } else {
                // Chứng từ CAO HƠN khai báo → Đền tối đa theo khai báo
                actualValue = declaredValue;
                valueExplanation = String.format("CT > KB (%s > %s): Đền 100%% theo KB (khách under-declare)", 
                        formatCurrency(docValue), formatCurrency(declaredValue));
            }
        } else {
            // OFF_ROUTE + Không chứng từ: dùng estimated hoặc declared
            if (assessment.getEstimatedMarketValue() != null && assessment.getEstimatedMarketValue().compareTo(BigDecimal.ZERO) > 0) {
                actualValue = assessment.getEstimatedMarketValue();
                valueExplanation = String.format("Không CT: Đền 100%% theo giá trị ước tính (%s)", 
                        formatCurrency(actualValue));
            } else {
                actualValue = declaredValue;
                valueExplanation = String.format("Không CT: Đền 100%% theo giá trị khai báo (%s)", 
                        formatCurrency(actualValue));
            }
        }
        
        // OFF_ROUTE = full loss
        BigDecimal goodsCompensation = actualValue.setScale(2, RoundingMode.HALF_UP);
        BigDecimal freightRefund = transportFee.setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalCompensation = goodsCompensation.add(freightRefund).setScale(2, RoundingMode.HALF_UP);
        
        String explanation = String.format("OFF_ROUTE - Lỗi cố ý: %s. KHÔNG áp dụng giới hạn 10× cước", valueExplanation);
        
        return CompensationDetailResponse.CompensationBreakdown.builder()
                .goodsCompensation(goodsCompensation)
                .freightRefund(freightRefund)
                .totalCompensation(totalCompensation)
                .legalLimit(BigDecimal.ZERO) // No legal limit for OFF_ROUTE
                .compensationCase("OFF_ROUTE")
                .explanation(explanation)
                .build();
    }
    
    /**
     * Resolve transport fee from order
     */
    private BigDecimal resolveTransportFee(OrderEntity order) {
        if (order == null || order.getId() == null) {
            return BigDecimal.ZERO;
        }
        try {
            var contractOpt = contractEntityService.getContractByOrderId(order.getId());
            if (contractOpt.isPresent()) {
                var contract = contractOpt.get();
                if (contract.getAdjustedValue() != null && contract.getAdjustedValue().compareTo(BigDecimal.ZERO) > 0) {
                    log.info("📦 Using adjustedValue as transport fee for order {}: {}", order.getId(), contract.getAdjustedValue());
                    return contract.getAdjustedValue();
                } else if (contract.getTotalValue() != null) {
                    log.info("📦 Using totalValue as transport fee for order {}: {}", order.getId(), contract.getTotalValue());
                    return contract.getTotalValue();
                }
            }
        } catch (Exception e) {
            log.warn("⚠️ Could not resolve transport fee from contract for order {}: {}", order.getId(), e.getMessage());
        }
        return BigDecimal.ZERO;
    }
    
    /**
     * Get category description
     */
    private String getCategoryDescription(String category) {
        if ("NORMAL".equals(category)) {
            return "Hàng thông thường";
        } else if ("FRAGILE".equals(category)) {
            return "Hàng dễ vỡ";
        } else if ("VALUABLE".equals(category)) {
            return "Hàng giá trị cao";
        } else if ("PERISHABLE".equals(category)) {
            return "Hàng dễ hỏng";
        }
        return category;
    }
    
    /**
     * Calculate compensation preview without saving
     * Used for realtime UI updates when staff changes assessment fields
     */
    @Override
    public CompensationDetailResponse.CompensationBreakdown calculatePreview(
            UUID issueId, 
            CompensationAssessmentRequest previewRequest) {
        
        log.info("🔍 Calculating preview for issue {}", issueId);
        
        IssueEntity issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with ID: " + issueId));
        
        OrderDetailEntity orderDetail = resolvePrimaryOrderDetail(issue);
        OrderEntity order = orderDetail.getOrderEntity();
        
        // Get issue category (enum name like "DAMAGE", "OFF_ROUTE") for routing logic
        String issueCategory = issue.getIssueTypeEntity() != null ? 
                issue.getIssueTypeEntity().getIssueCategory() : null;
        
        // Create temporary assessment entity from request for calculation
        IssueCompensationAssessmentEntity tempAssessment = IssueCompensationAssessmentEntity.builder()
                .issue(issue)
                .issueType(previewRequest.getIssueType())
                .hasDocuments(previewRequest.getHasDocuments())
                .documentValue(previewRequest.getDocumentValue())
                .estimatedMarketValue(previewRequest.getEstimatedMarketValue())
                .assessmentRate(previewRequest.getAssessmentRate())
                .build();
        
        // Route to correct calculation based on issue category (enum)
        if ("DAMAGE".equals(issueCategory)) {
            return calculateDamageCompensationBreakdown(orderDetail, order, tempAssessment);
        } else if ("OFF_ROUTE".equals(issueCategory)
                || "RUNAWAY".equals(issueCategory)
                || "OFF_ROUTE_RUNAWAY".equals(issueCategory)) {
            // OFF_ROUTE_RUNAWAY dùng cùng công thức với OFF_ROUTE: 100% giá trị hàng + 100% cước
            return calculateOffRouteCompensationBreakdown(orderDetail, order, tempAssessment);
        } else {
            throw new RuntimeException("Unsupported issue category for preview: " + issueCategory +
                    " (type: " + (issue.getIssueTypeEntity() != null ? issue.getIssueTypeEntity().getIssueTypeName() : "null") + ")");
        }
    }

    private OrderDetailEntity resolvePrimaryOrderDetail(IssueEntity issue) {
        List<OrderDetailEntity> issueOrderDetails = issue.getOrderDetails();
        if (issueOrderDetails != null && !issueOrderDetails.isEmpty()) {
            return issueOrderDetails.get(0);
        }

        if (issue.getVehicleAssignmentEntity() != null) {
            List<OrderDetailEntity> byAssignment = orderDetailEntityService.findByVehicleAssignmentEntity(issue.getVehicleAssignmentEntity());
            if (byAssignment != null && !byAssignment.isEmpty()) {
                return byAssignment.get(0);
            }
        }

        throw new RuntimeException("No order details found for issue: " + issue.getId());
    }
    
    /**
     * Format currency for logging/explanation
     */
    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "0 đ";
        }
        return String.format("%,.0f đ", value);
    }
}
