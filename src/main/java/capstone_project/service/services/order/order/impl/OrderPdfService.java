package capstone_project.service.services.order.order.impl;

import capstone_project.dtos.response.order.contract.ContractPdfResponse;
import capstone_project.dtos.response.order.ListContractRuleAssignResult;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.order.ContractRuleService;
import capstone_project.service.services.order.order.ContractService;
import capstone_project.service.services.pdf.PdfGenerationService;
import capstone_project.service.services.user.DistanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderPdfService {

    private final ContractEntityService contractEntityService;
    private final ContractService contractService;
    private final ContractRuleService contractRuleAssignService;
    private final PdfGenerationService pdfGenerationService;
    private final CloudinaryService cloudinaryService;
    private final DistanceService distanceService;

    public ContractPdfResponse generateAndUploadContractPdf(UUID contractId) {
        try {
            ContractEntity contract = contractEntityService.findEntityById(contractId)
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

            OrderEntity order = contract.getOrderEntity();
            if (order == null) {
                throw new IllegalStateException("No order found for contract id: " + contractId);
            }

            ListContractRuleAssignResult assignResult =
                    contractRuleAssignService.getListAssignOrUnAssignContractRule(contractId);


            Map<UUID, Integer> vehicleCountMap = assignResult.vehicleAssignments().stream()
                    .collect(Collectors.groupingBy(ContractRuleAssignResponse::getVehicleRuleId, Collectors.summingInt(a -> 1)));


            BigDecimal distanceKm = distanceService.getDistanceInKilometers(order.getId());

//            BigDecimal distanceKm = BigDecimal.valueOf(101.00);

            byte[] pdfBytes = pdfGenerationService.generateContractPdf(
                    contract,
                    order,
                    assignResult,
                    distanceKm,
                    vehicleCountMap
            );

            String fileName = "contract_" + contract.getContractName() + "_" + System.currentTimeMillis();
            Map<String, Object> uploadResult = cloudinaryService.uploadFile(
                    pdfBytes,
                    fileName,
                    "contract_pdfs"
            );

            String pdfUrl = (String) uploadResult.get("secure_url");

            return ContractPdfResponse.builder()
                    .contractId(contractId.toString())
                    .pdfUrl(pdfUrl)
                    .message("Contract PDF generated and uploaded successfully")
                    .build();

        } catch (Exception e) {
            log.error("Error generating/uploading contract PDF: {}", e.getMessage(), e);
            return ContractPdfResponse.builder()
                    .contractId(contractId.toString())
                    .pdfUrl(null)
                    .message("Failed to generate/upload Contract PDF: " + e.getMessage())
                    .build();
        }
    }

}