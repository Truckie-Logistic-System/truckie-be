package capstone_project.controller.order;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.dtos.response.order.transaction.GetTransactionStatusResponse;
import capstone_project.dtos.response.order.transaction.TransactionResponse;
import capstone_project.service.services.order.transaction.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${transaction.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/{contractId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> createTransaction(@PathVariable UUID contractId) {
        final var result = transactionService.createTransaction(contractId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionById(@PathVariable UUID transactionId) {
        final var result = transactionService.getTransactionById(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{contractId}/list")
    public ResponseEntity<ApiResponse<List<TransactionResponse>>> getTransactionsByContractId(@PathVariable UUID contractId) {
        final var result = transactionService.getTransactionsByContractId(contractId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{transactionId}/status")
    public ResponseEntity<ApiResponse<GetTransactionStatusResponse>> getTransactionStatusById(@PathVariable UUID transactionId) {
        final var result = transactionService.getTransactionStatus(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/webhook")
    @PreAuthorize("permitAll()")
    public ResponseEntity<Void> handleWebhook(@RequestBody String rawCallbackPayload) {
        transactionService.handleWebhook(rawCallbackPayload);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{transactionId}/sync")
    public ResponseEntity<ApiResponse<TransactionResponse>> syncTransaction(@PathVariable UUID transactionId) {
        final var result = transactionService.syncTransaction(transactionId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/{transactionId}/refund")
    public ResponseEntity<ApiResponse<TransactionResponse>> refundTransaction(@PathVariable UUID transactionId,
                                                                                @RequestParam String reason) {
            final var result = transactionService.refundTransaction(transactionId, reason);
            return ResponseEntity.ok(ApiResponse.ok(result));
        }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam Map<String, String> params) {
        log.info("Return callback received: {}", params);
        return ResponseEntity.ok("Payment return callback received");
    }

    @GetMapping("/cancel")
    public ResponseEntity<String> cancel(@RequestParam Map<String, String> params) {
        log.info("Cancel callback received: {}", params);
        return ResponseEntity.ok("Payment cancel callback received");
    }
}
