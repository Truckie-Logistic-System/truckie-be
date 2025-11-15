package capstone_project.service.services.issue;

import capstone_project.dtos.response.issue.OrderRejectionDetailResponse;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for customer-specific issue operations
 * Handles ORDER_REJECTION issues from customer perspective
 */
public interface CustomerIssueService {
    
    /**
     * Get all ORDER_REJECTION issues for current customer's orders
     * @return List of order rejection issues
     */
    List<OrderRejectionDetailResponse> getCustomerOrderRejectionIssues();
    
    /**
     * Get ORDER_REJECTION issues for a specific order
     * @param orderId Order ID
     * @return List of order rejection issues for the order
     */
    List<OrderRejectionDetailResponse> getOrderRejectionIssuesByOrder(UUID orderId);
    
    /**
     * Create return payment transaction for customer
     * @param issueId Issue ID
     * @return Transaction response with PayOS checkout URL
     */
    capstone_project.dtos.response.order.transaction.TransactionResponse createReturnPaymentTransaction(UUID issueId);
    
    /**
     * Reject return payment - customer doesn't want to pay
     * This will keep journey INACTIVE and items will be cancelled
     * @param issueId Issue ID
     */
    void rejectReturnPayment(UUID issueId);
}
