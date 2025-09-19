package capstone_project.service.services.order.transaction;

import capstone_project.dtos.response.order.transaction.GetTransactionStatusResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;

import java.util.List;
import java.util.UUID;

public interface TransactionService {

    TransactionResponse createTransaction(UUID contractId);

    TransactionResponse createDepositTransaction(UUID contractId);

    TransactionResponse getTransactionById(UUID transactionId);

    List<TransactionResponse> getTransactionsByContractId(UUID contractId);

    GetTransactionStatusResponse getTransactionStatus(UUID transactionId);

    void handleWebhook(String rawCallbackPayload);

    TransactionResponse syncTransaction(UUID transactionId);

    TransactionResponse refundTransaction(UUID transactionId, String reason);

}
