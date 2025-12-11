package capstone_project.service.services.order.transaction.impl;

import capstone_project.dtos.response.order.transaction.StaffTransactionResponse;
import capstone_project.entity.order.contract.ContractEntity;
import capstone_project.entity.order.transaction.TransactionEntity;
import capstone_project.repository.entityServices.order.transaction.TransactionEntityService;
import capstone_project.service.services.order.transaction.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TransactionServiceImpl implements TransactionService {

    @Autowired
    private TransactionEntityService transactionEntityService;

    @Override
    public List<StaffTransactionResponse> getAllTransactionsForStaff() {
        List<TransactionEntity> transactions = transactionEntityService.findAll();
        
        // Sort by createdAt DESC to show newest transactions first
        return transactions.stream()
                .sorted(Comparator.comparing(TransactionEntity::getCreatedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::mapToStaffTransactionResponse)
                .collect(Collectors.toList());
    }

    @Override
    public StaffTransactionResponse getTransactionDetailForStaff(UUID transactionId) {
        TransactionEntity transaction = transactionEntityService.findEntityById(transactionId)
                .orElseThrow(() -> new RuntimeException(
                        "Transaction not found: " + transactionId
                ));
        
        return mapToStaffTransactionResponseWithContract(transaction);
    }

    /**
     * Map TransactionEntity to StaffTransactionResponse (for list view - minimal contract info)
     */
    private StaffTransactionResponse mapToStaffTransactionResponse(TransactionEntity transaction) {
        ContractEntity contract = transaction.getContractEntity();
        
        return StaffTransactionResponse.builder()
                .id(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .amount(transaction.getAmount())
                .status(transaction.getStatus())
                .paymentProvider(transaction.getPaymentProvider())
                .currencyCode(transaction.getCurrencyCode())
                .gatewayOrderCode(transaction.getGatewayOrderCode())
                .paymentDate(transaction.getPaymentDate())
                .createdAt(transaction.getCreatedAt())
                .contract(contract != null ? StaffTransactionResponse.ContractInfo.builder()
                        .id(contract.getId())
                        .contractName(contract.getContractName())
                        .status(contract.getStatus())
                        .orderCode(contract.getOrderEntity() != null ? contract.getOrderEntity().getOrderCode() : null)
                        .build() : null)
                .build();
    }

    /**
     * Map TransactionEntity to StaffTransactionResponse with full contract info (for detail view)
     */
    private StaffTransactionResponse mapToStaffTransactionResponseWithContract(TransactionEntity transaction) {
        StaffTransactionResponse response = mapToStaffTransactionResponse(transaction);
        
        // If there's a contract, enrich with full details
        if (transaction.getContractEntity() != null) {
            ContractEntity contract = transaction.getContractEntity();
            
            StaffTransactionResponse.ContractInfo fullContractInfo = StaffTransactionResponse.ContractInfo.builder()
                    .id(contract.getId())
                    .contractName(contract.getContractName())
                    .status(contract.getStatus())
                    .orderCode(contract.getOrderEntity() != null ? contract.getOrderEntity().getOrderCode() : null)
                    .orderStatus(contract.getOrderEntity() != null ? contract.getOrderEntity().getStatus() : null)
                    .customerName(contract.getOrderEntity() != null && 
                                   contract.getOrderEntity().getSender() != null && 
                                   contract.getOrderEntity().getSender().getUser() != null ? 
                                   contract.getOrderEntity().getSender().getUser().getFullName() : null)
                    .adjustedValue(contract.getAdjustedValue())
                    .totalValue(contract.getTotalValue())
                    .attachFileUrl(contract.getAttachFileUrl())
                    .build();
            
            response.setContract(fullContractInfo);
        }
        
        return response;
    }
}
