package capstone_project.config.payment;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import vn.payos.PayOS;

@Configuration
@RequiredArgsConstructor
public class PayOSConfig {

    private final PayOSProperties properties;

    @Bean
    public PayOS payOS() {
        return new PayOS(properties.getClientId(),
                properties.getApiKey(),
                properties.getChecksumKey());
    }
}
