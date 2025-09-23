package capstone_project.controller.order;

import capstone_project.dtos.response.common.ApiResponse;
import capstone_project.service.services.order.transaction.stripe.StripeTransactionService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("${stripe-transaction.api.base-path}")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Slf4j
public class StripeTransactionController {

    private final StripeTransactionService stripeTransactionService;

    @PostMapping("/{contractId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> createTransaction(@PathVariable UUID contractId) {
        try {
            PaymentIntent intent = stripeTransactionService.createPaymentIntent(contractId);

            Map<String, String> data = new HashMap<>();
            data.put("clientSecret", intent.getClientSecret());
            data.put("paymentIntentId", intent.getId());

            log.info("Stripe PaymentIntent created successfully for contract {} - ID: {}", contractId, intent.getId());

            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/{contractId}/deposit")
    public ResponseEntity<ApiResponse<Map<String, String>>> createDepositTransaction(@PathVariable UUID contractId) {
        try {
            PaymentIntent intent = stripeTransactionService.createDepositPaymentIntent(contractId);

            Map<String, String> data = new HashMap<>();
            data.put("clientSecret", intent.getClientSecret());
            data.put("paymentIntentId", intent.getId());

            log.info("Stripe PaymentIntent created successfully for contract {} - ID: {}", contractId, intent.getId());

            return ResponseEntity.ok(ApiResponse.ok(data));
        } catch (StripeException e) {
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handleStripeWebhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        stripeTransactionService.handleStripeWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
