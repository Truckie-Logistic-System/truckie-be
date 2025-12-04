package capstone_project.controller.order;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.RecipientOrderTrackingResponse;
import capstone_project.service.services.order.order.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public controller for recipient order tracking
 * No authentication required - allows recipients to track orders by order code
 */
@RestController
@RequestMapping("${recipient-tracking.api.base-path}")
@RequiredArgsConstructor
@Slf4j
public class RecipientOrderTrackingController {

    private final OrderService orderService;

    /**
     * Get order tracking information for recipient by order code
     * This endpoint is public and does not require authentication
     * Returns order information without sensitive contract/transaction data
     * 
     * @param orderCode the order code to search
     * @return order tracking response for recipient
     */
    @GetMapping("/{orderCode}")
    public ResponseEntity<ApiResponse<RecipientOrderTrackingResponse>> getOrderByCode(
            @PathVariable String orderCode) {
        log.info("📦 Recipient tracking request for order code: {}", orderCode);
        final var result = orderService.getOrderForRecipientByOrderCode(orderCode);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
