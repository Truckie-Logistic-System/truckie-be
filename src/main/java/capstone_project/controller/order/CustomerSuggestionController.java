package capstone_project.controller.order;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.ReceiverDetailResponse;
import capstone_project.dtos.response.order.ReceiverSuggestionResponse;
import capstone_project.service.services.order.suggestion.CustomerSuggestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("${order.api.base-path}/suggestions")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class CustomerSuggestionController {

    private final CustomerSuggestionService customerSuggestionService;

    /**
     * API endpoint to get recent receivers for the current customer
     *
     * @param limit maximum number of suggestions to return (default: 5)
     * @return list of receiver suggestions
     */
    @GetMapping("/recent-receivers")
    public ResponseEntity<ApiResponse<List<ReceiverSuggestionResponse>>> getRecentReceivers(
            @RequestParam(required = false, defaultValue = "5") int limit) {
        final var result = customerSuggestionService.getRecentReceivers(limit);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * API endpoint to get detailed information about a specific receiver
     *
     * @param orderId ID of the order containing receiver information
     * @return detailed receiver information with addresses
     */
    @GetMapping("/receiver-details/{orderId}")
    public ResponseEntity<ApiResponse<ReceiverDetailResponse>> getReceiverDetails(
            @PathVariable UUID orderId) {
        final var result = customerSuggestionService.getReceiverDetails(orderId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
