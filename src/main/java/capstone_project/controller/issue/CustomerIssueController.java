package capstone_project.controller.issue;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.issue.OrderRejectionDetailResponse;
import capstone_project.service.services.issue.CustomerIssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for customer-specific issue operations
 * Handles ORDER_REJECTION issues from customer perspective
 */
@RestController
@RequestMapping("${issue-customer.api.base-path}")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("isAuthenticated()")
public class CustomerIssueController {
    
    private final CustomerIssueService customerIssueService;
    
    /**
     * Get all ORDER_REJECTION issues for current customer's orders
     */
    @GetMapping("/order-rejections")
    public ResponseEntity<ApiResponse<List<OrderRejectionDetailResponse>>> getCustomerOrderRejectionIssues() {

        List<OrderRejectionDetailResponse> issues = customerIssueService.getCustomerOrderRejectionIssues();

        return ResponseEntity.ok(ApiResponse.ok(
                issues,
                String.format("Tìm thấy %d sự cố trả hàng", issues.size())
        ));
    }
    
    /**
     * Get ORDER_REJECTION issues for a specific order
     */
    @GetMapping("/order/{orderId}/order-rejections")
    public ResponseEntity<ApiResponse<List<OrderRejectionDetailResponse>>> getOrderRejectionIssuesByOrder(
            @PathVariable UUID orderId) {

        List<OrderRejectionDetailResponse> issues = 
                customerIssueService.getOrderRejectionIssuesByOrder(orderId);

        return ResponseEntity.ok(ApiResponse.ok(
                issues,
                String.format("Tìm thấy %d sự cố trả hàng cho đơn hàng", issues.size())
        ));
    }
    
    /**
     * Create return payment transaction
     * Customer pays for return shipping fee
     */
    @PostMapping("/{issueId}/create-return-payment")
    public ResponseEntity<ApiResponse<capstone_project.dtos.response.order.transaction.TransactionResponse>> createReturnPayment(
            @PathVariable UUID issueId) {

        capstone_project.dtos.response.order.transaction.TransactionResponse result = 
                customerIssueService.createReturnPaymentTransaction(issueId);

        return ResponseEntity.ok(ApiResponse.ok(result, "Đã tạo giao dịch thanh toán trả hàng"));
    }
    
    /**
     * Reject return payment - customer doesn't want to pay
     * This will keep journey INACTIVE and items will be cancelled
     */
    @PostMapping("/{issueId}/reject-return-payment")
    public ResponseEntity<ApiResponse<Void>> rejectReturnPayment(@PathVariable UUID issueId) {

        customerIssueService.rejectReturnPayment(issueId);

        return ResponseEntity.ok(ApiResponse.ok(
                null,
                "Đã từ chối thanh toán. Các kiện hàng sẽ được hủy."
        ));
    }
}
