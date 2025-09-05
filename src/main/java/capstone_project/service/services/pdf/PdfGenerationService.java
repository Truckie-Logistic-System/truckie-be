package capstone_project.service.services.pdf;

import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.dtos.response.order.contract.PriceCalculationResponse;
import capstone_project.entity.auth.UserEntity;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.entityServices.auth.UserEntityService;
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
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfGenerationService {

    private final TemplateEngine templateEngine;
    private final ContractService contractService;
    private final CustomerEntityService customerEntityService;
    private final UserEntityService userEntityService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");

    public byte[] generateContractPdf(ContractEntity contract,
                                      OrderEntity order,
                                      ListContractRuleAssignResult assignResult,
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

            Optional<CustomerEntity> customerOpt = customerEntityService.findByUserId(order.getSender().getId());
            if (customerOpt.isPresent()) {
                CustomerEntity customer = customerOpt.get();
                context.setVariable("customerName", customer.getCompanyName() != null ? customer.getCompanyName() : "N/A");
                context.setVariable("representativeName", customer.getRepresentativeName() != null ? customer.getRepresentativeName() : "N/A");
                context.setVariable("customerPhone", customer.getRepresentativePhone() != null ? customer.getRepresentativePhone() : "N/A");
                context.setVariable("businessLicenseNumber", customer.getBusinessLicenseNumber() != null ? customer.getBusinessLicenseNumber() : "N/A");
                context.setVariable("businessAddress", customer.getBusinessAddress() != null ? customer.getBusinessAddress() : "N/A");
            } else {
                context.setVariable("customerName", "N/A");
                context.setVariable("representativeName", "N/A");
                context.setVariable("customerPhone", "N/A");
                context.setVariable("businessLicenseNumber", "N/A");
                context.setVariable("businessAddress", "N/A");
            }

            Optional<UserEntity> userEntityOptional = userEntityService.getUserById(order.getSender().getUser().getId());
            if (userEntityOptional.isPresent()) {
                UserEntity user = userEntityOptional.get();
                context.setVariable("fullName", user.getFullName() != null ? user.getFullName() : "N/A");
                context.setVariable("senderEmail", user.getEmail() != null ? user.getEmail() : "N/A");
                context.setVariable("phoneNumber", user.getPhoneNumber() != null ? user.getPhoneNumber() : "N/A");
            } else {
                context.setVariable("fullName", "N/A");
                context.setVariable("senderEmail", "N/A");
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
            context.setVariable("summary", result.getSummary());
            context.setVariable("distanceKm", distanceKm);

            String htmlContent = templateEngine.process("contract-pdf", context);


            try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                ITextRenderer renderer = new ITextRenderer();

                renderer.getFontResolver().addFont(
                        "src/main/resources/fonts/DejaVuSans.ttf",
                        com.lowagie.text.pdf.BaseFont.IDENTITY_H,
                        com.lowagie.text.pdf.BaseFont.EMBEDDED
                );

                renderer.setDocumentFromString(htmlContent);
                renderer.layout();
                renderer.createPDF(baos);
                return baos.toByteArray();
            }


        } catch (Exception e) {
            throw new RuntimeException("Error generating Contract PDF", e);
        }
    }
}
