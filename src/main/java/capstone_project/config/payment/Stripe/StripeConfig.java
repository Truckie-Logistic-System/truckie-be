package capstone_project.config.payment.Stripe;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class StripeConfig {

    @Value("${stripe.secret-key}")
    private String secretKey;

    @Value("${stripe.webhook-secret-key}")
    private String secretWebhookKey;

    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
    }
}