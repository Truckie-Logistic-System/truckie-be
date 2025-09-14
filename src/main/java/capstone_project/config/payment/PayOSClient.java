//package capstone_project.config.payment;
//
//import capstone_project.common.enums.ErrorEnum;
//import capstone_project.common.exceptions.dto.NotFoundException;
//import capstone_project.dtos.request.order.transaction.PayOSCreatePaymentRequest;
//import capstone_project.dtos.response.order.transaction.PayOSCreatePaymentResponse;
//import capstone_project.dtos.response.order.transaction.TransactionResponse;
//import capstone_project.entity.order.contract.ContractEntity;
//import capstone_project.entity.order.transaction.TransactionEntity;
//import capstone_project.repository.entityServices.order.contract.ContractEntityService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//import org.springframework.web.client.RestTemplate;
//
//import java.util.UUID;
//
//@Component
//@RequiredArgsConstructor
//public class PayOSClient {
//
//    private final ContractEntityService contractEntityService;
//    private final RestTemplate restTemplate;
//    private final PayOSConfig config;
//
//    @Override
//    public TransactionResponse createPayment(UUID contractId) {
//        ContractEntity contractEntity = contractEntityService.findEntityById(contractId)
//                .orElseThrow(() -> new NotFoundException(
//                        ErrorEnum.NOT_FOUND.getMessage(),
//                        ErrorEnum.NOT_FOUND.getErrorCode()
//                ));
//
//        // Gọi PayOS API
//        PayOSCreatePaymentRequest request = new PayOSCreatePaymentRequest(
//                contractEntity.getOrderEntity().getOrderCode(),
//                contractEntity.getTotalValue(),
//                "VND",
//                "Payment for contract " + contractEntity.getContractName(),
//                config.returnUrl(),
//                config.getReturnUrl()
//        );
//
//        PayOSCreatePaymentResponse payOSResponse = payOSClient.createPayment(request);
//
//        // Map sang TransactionEntity
//        TransactionEntity transaction = TransactionEntity.builder()
//                .paymentProvider("PayOS")
//                .amount(payOSResponse.getAmount())
//                .currencyCode(payOSResponse.getCurrencyCode())
//                .status(payOSResponse.getStatus())
//                .gatewayResponse(payOSResponse.getRawResponse()) // JSON string
//                .paymentDate(payOSResponse.getPaymentDate())
//                .contractEntity(contractEntity)
//                .build();
//
//        transactionRepository.save(transaction);
//
//        // Trả TransactionResponse cho FE
//        return new TransactionResponse(
//                payOSResponse.getTransactionId(),
//                "PayOS",
//                contractEntity.getOrderEntity().getOrderCode(),
//                payOSResponse.getAmount(),
//                payOSResponse.getCurrencyCode(),
//                payOSResponse.getMessage(),
//                payOSResponse.getStatus(),
//                payOSResponse.getPaymentDate(),
//                contractEntity.getId().toString()
//        );
//    }
//
//
//}
