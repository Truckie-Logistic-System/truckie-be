package capstone_project.service.services.order.order.impl;

import capstone_project.common.enums.ErrorEnum;
import capstone_project.common.exceptions.dto.NotFoundException;
import capstone_project.dtos.response.order.contract.ContractPdfResponse;
import capstone_project.dtos.response.order.contract.ContractRuleAssignResponse;
import capstone_project.dtos.response.order.contract.FullContractPDFResponse;
import capstone_project.dtos.response.order.contract.PriceCalculationResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.order.OrderEntity;
import capstone_project.entity.setting.ContractSettingEntity;
import capstone_project.entity.user.customer.CustomerEntity;
import capstone_project.repository.entityServices.order.contract.ContractEntityService;
import capstone_project.repository.entityServices.setting.ContractSettingEntityService;
import capstone_project.service.mapper.order.OrderMapper;
import capstone_project.service.mapper.setting.ContractSettingMapper;
import capstone_project.service.mapper.user.CustomerMapper;
import capstone_project.service.mapper.user.UserMapper;
import capstone_project.service.services.cloudinary.CloudinaryService;
import capstone_project.service.services.order.order.ContractRuleService;
import capstone_project.service.services.order.order.ContractService;
import capstone_project.service.services.pdf.PdfGenerationService;
import capstone_project.service.services.user.DistanceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
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
    private final ContractSettingEntityService contractSettingEntityService;

    private final CustomerMapper customerMapper;
    private final UserMapper userMapper;
    private final ContractSettingMapper contractSettingMapper;
    private final OrderMapper orderMapper;

    public ContractPdfResponse generateAndUploadContractPdf(UUID contractId) {
        try {
            ContractEntity contract = contractEntityService.findEntityById(contractId)
                    .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));

            OrderEntity order = contract.getOrderEntity();
            if (order == null) {
                throw new IllegalStateException("Contract has no associated order: " + contractId);
            }

            List<ContractRuleAssignResponse> assignResult = contractService.assignVehicles(order.getId());

            log.info("Assignments total: {}", assignResult.size());
            assignResult.forEach(a ->
                    log.info("Assignment => ruleId={}, ruleName={}, index={}, load={}",
                            a.getVehicleRuleId(), a.getVehicleRuleName(), a.getVehicleIndex(), a.getCurrentLoad())
            );

            Map<UUID, Integer> vehicleCountMap = assignResult.stream()
                    .collect(Collectors.groupingBy(
                            ContractRuleAssignResponse::getVehicleRuleId,
                            Collectors.summingInt(a -> 1)
                    ));

            log.info("VehicleCountMap: {}", vehicleCountMap);

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

    public FullContractPDFResponse getFullContractPdfData(UUID contractId) {
        ContractEntity contract = contractEntityService.findEntityById(contractId)
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        OrderEntity order = contract.getOrderEntity();
        if (order == null) {
            throw new NotFoundException(
                    "Order not found for the given contract",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

        CustomerEntity customer = order.getSender();
        if (customer == null) {
            throw new NotFoundException(
                    "Customer (sender) not found for the given order",
                    ErrorEnum.NOT_FOUND.getErrorCode()
            );
        }

//        UserEntity user = customer.getUser();
//        if (user == null) {
//            throw new NotFoundException(
//                    "User not found for the given customer",
//                    ErrorEnum.NOT_FOUND.getErrorCode()
//            );
//        }

        ContractSettingEntity setting = contractSettingEntityService.findFirstByOrderByCreatedAtAsc()
                .orElseThrow(() -> new NotFoundException(
                        ErrorEnum.NOT_FOUND.getMessage(),
                        ErrorEnum.NOT_FOUND.getErrorCode()
                ));

        List<ContractRuleAssignResponse> assignResult = contractService.assignVehicles(order.getId());

        Map<UUID, Integer> vehicleCountMap = assignResult.stream()
                .collect(Collectors.groupingBy(
                        ContractRuleAssignResponse::getVehicleRuleId,
                        Collectors.summingInt(a -> 1)
                ));

        BigDecimal distanceKm = distanceService.getDistanceInKilometers(order.getId());

        PriceCalculationResponse result =
                contractService.calculateTotalPrice(contract, distanceKm, vehicleCountMap);

        FullContractPDFResponse response = FullContractPDFResponse.builder()
                .contractId(contractId.toString())
//                .pdfUrl(contract.getPdfUrl())
                .message("Full contract PDF data retrieved successfully")
                .customerInfo(customerMapper.mapCustomerResponse(customer))
//                .senderInfo(userMapper.mapUserResponse(user))
                .orderInfo(orderMapper.toGetOrderResponse(order))
                .priceDetails(result)
                .assignResult(assignResult)
                .distanceKm(distanceKm)
                .contractSettings(contractSettingMapper.toContractSettingResponse(setting))
                .build();

        return response;
    }

}