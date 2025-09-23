package capstone_project.service.services.order.transaction.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;

import java.util.UUID;

public interface StripeTransactionService {
    PaymentIntent createPaymentIntent(UUID contractId) throws StripeException;

    PaymentIntent createDepositPaymentIntent(UUID contractId) throws StripeException;

    void handleStripeWebhook(String payload, String sigHeader);
}
