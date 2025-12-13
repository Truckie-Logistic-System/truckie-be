package capstone_project.service.services.order.transaction;

import capstone_project.dtos.response.order.transaction.StaffTransactionResponse;

import java.util.List;
import java.util.UUID;

public interface TransactionService {
    /**
     * Get all transactions for staff management
     * Includes minimal contract info for list view
     * Sorted by createdAt DESC (newest first)
     */
    List<StaffTransactionResponse> getAllTransactionsForStaff();

    /**
     * Get transaction detail with full information for staff
     * Includes complete contract information for banner
     */
    StaffTransactionResponse getTransactionDetailForStaff(UUID transactionId);
}
