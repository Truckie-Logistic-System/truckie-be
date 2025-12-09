package capstone_project.service.services.pdf;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.order.contract.PriceCalculationResponse;
import capstone_project.dtos.response.order.contract.PriceCalculationResponse.CalculationStep;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.setting.ContractSettingEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
import capstone_project.repository.entityServices.setting.ContractSettingEntityService;
import capstone_project.repository.entityServices.user.CustomerEntityService;
import capstone_project.service.services.order.order.ContractService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfGenerationService {

    private final TemplateEngine templateEngine;
    private final ContractService contractService;
    private final CustomerEntityService customerEntityService;
    private final UserEntityService userEntityService;
    private final ContractSettingEntityService contractSettingEntityService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public byte[] generateContractPdf(ContractEntity contract,
                                      OrderEntity order,
                                      List<ContractRuleAssignResponse> assignResult,
                                      BigDecimal distanceKm,
                                      Map<UUID, Integer> vehicleCountMap) {
        try {
            PriceCalculationResponse result =
                    contractService.calculateTotalPrice(contract, distanceKm, vehicleCountMap);

            Context context = new Context();

            if (contract.getCreatedAt() != null) {
                String formattedDate = contract.getCreatedAt().format(DATE_FORMATTER);
                context.setVariable("createdDateFormatted", formattedDate);
            }

            // Lấy thông tin khách hàng từ sender của order
            CustomerEntity senderCustomer = order.getSender();
            
            // Log để debug
            log.info("🔍 PDF Generation - Order ID: {}, Sender: {}", order.getId(), senderCustomer);
            
            if (senderCustomer != null) {
                log.info("🔍 Customer Info - CompanyName: {}, RepName: {}, Phone: {}, Address: {}", 
                    senderCustomer.getCompanyName(),
                    senderCustomer.getRepresentativeName(),
                    senderCustomer.getRepresentativePhone(),
                    senderCustomer.getBusinessAddress());
                    
                context.setVariable("customerName", senderCustomer.getCompanyName() != null ? senderCustomer.getCompanyName() : "N/A");
                context.setVariable("representativeName", senderCustomer.getRepresentativeName() != null ? senderCustomer.getRepresentativeName() : "N/A");
                context.setVariable("customerPhone", senderCustomer.getRepresentativePhone() != null ? senderCustomer.getRepresentativePhone() : "N/A");
                context.setVariable("businessLicenseNumber", senderCustomer.getBusinessLicenseNumber() != null ? senderCustomer.getBusinessLicenseNumber() : "N/A");
                context.setVariable("businessAddress", senderCustomer.getBusinessAddress() != null ? senderCustomer.getBusinessAddress() : "N/A");
                
                // Lấy email từ User entity nếu có
                if (senderCustomer.getUser() != null) {
                    UserEntity user = senderCustomer.getUser();
                    context.setVariable("senderEmail", user.getEmail() != null ? user.getEmail() : "N/A");
                    context.setVariable("fullName", user.getFullName() != null ? user.getFullName() : "N/A");
                    context.setVariable("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
                } else {
                    context.setVariable("senderEmail", "N/A");
                    context.setVariable("fullName", "N/A");
                    context.setVariable("phoneNumber", "N/A");
                }
            } else {
                log.warn("⚠️ PDF Generation - Sender customer is NULL for order: {}", order.getId());
                context.setVariable("customerName", "N/A");
                context.setVariable("representativeName", "N/A");
                context.setVariable("customerPhone", "N/A");
                context.setVariable("businessLicenseNumber", "N/A");
                context.setVariable("businessAddress", "N/A");
                context.setVariable("senderEmail", "N/A");
                context.setVariable("fullName", "N/A");
                context.setVariable("phoneNumber", "N/A");
            }

            context.setVariable("contract", contract);
            context.setVariable("order", order);
            context.setVariable("assignResult", assignResult);

            context.setVariable("totalPrice", result.getTotalPrice());
            context.setVariable("totalBeforeAdjustment", result.getTotalBeforeAdjustment());
            context.setVariable("categoryExtraFee", result.getCategoryExtraFee());
            context.setVariable("categoryMultiplier", result.getCategoryMultiplier());
            context.setVariable("promotionDiscount", result.getPromotionDiscount());
            context.setVariable("finalTotal", result.getFinalTotal());
            context.setVariable("calculationDetails", result.getSteps());
            
            // Group calculation details by vehicle type for PDF display
            Map<String, List<CalculationStep>> groupedSteps = result.getSteps().stream()
                    .collect(Collectors.groupingBy(CalculationStep::getSizeRuleName));
            context.setVariable("groupedCalculationDetails", groupedSteps);
            
            // Pre-calculate vehicle totals for each vehicle type
            Map<String, BigDecimal> vehicleTotals = new HashMap<>();
            for (Map.Entry<String, List<CalculationStep>> entry : groupedSteps.entrySet()) {
                BigDecimal total = entry.getValue().stream()
                        .map(CalculationStep::getSubtotal)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                vehicleTotals.put(entry.getKey(), total);
            }
            context.setVariable("vehicleTotals", vehicleTotals);
            
//            context.setVariable("summary", result.getSummary());
            context.setVariable("distanceKm", distanceKm);

            ContractSettingEntity setting = contractSettingEntityService.findFirstByOrderByCreatedAtAsc()
                    .orElseThrow(() -> new NotFoundException(
                            ErrorEnum.NOT_FOUND.getMessage(),
                            ErrorEnum.NOT_FOUND.getErrorCode()
                    ));

            context.setVariable("depositPercent", setting.getDepositPercent());
            context.setVariable("depositDeadlineHours", setting.getDepositDeadlineHours());
            context.setVariable("signingDeadlineHours", setting.getSigningDeadlineHours());
            context.setVariable("fullPaymentDaysBeforePickup", setting.getFullPaymentDaysBeforePickup());
            context.setVariable("insuranceRateNormal", setting.getInsuranceRateNormal());
            context.setVariable("insuranceRateFragile", setting.getInsuranceRateFragile());
            context.setVariable("vatRate", setting.getVatRate());

            String htmlContent = templateEngine.process("contract-pdf", context);


            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                System.out.println("🔧 DEBUG: PdfGenerationService.generateContractPdf() called at " + java.time.LocalDateTime.now());
                ITextRenderer renderer = new ITextRenderer();

                // Add Times New Roman font (serif - matches frontend)
                renderer.getFontResolver().addFont(
                        "src/main/resources/fonts/TimesNewRoman.ttf",
                        com.lowagie.text.pdf.BaseFont.IDENTITY_H,
                        com.lowagie.text.pdf.BaseFont.EMBEDDED
                );
                // Also add DejaVu Sans as fallback
                renderer.getFontResolver().addFont(
                        "src/main/resources/fonts/DejaVuSans.ttf",
                        com.lowagie.text.pdf.BaseFont.IDENTITY_H,
                        com.lowagie.text.pdf.BaseFont.EMBEDDED
                );

                // Set page size and margins for proper pagination
                renderer.setDocumentFromString(htmlContent);
                
                // Configure page settings
                renderer.getSharedContext().setPrint(true);
                renderer.getSharedContext().setInteractive(false);
                
                // Layout and create PDF with proper page breaks
                renderer.layout();
                renderer.createPDF(baos, true); // true enables multi-page support
                return baos.toByteArray();
            }


        } catch (Exception e) {
            throw new RuntimeException("Error generating Contract PDF", e);
        }
    }
}
