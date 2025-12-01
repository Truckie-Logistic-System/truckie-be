package capstone_project.config.rateLimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitConfig {

    /**
     * Create bucket for guest conversation creation: 3 conversations per hour
     */
    public Bucket createGuestConversationBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(3, Refill.intervally(3, Duration.ofHours(1))))
            .build();
    }

    /**
     * Create bucket for guest messages: 10 messages per 5 minutes
     */
    public Bucket createGuestMessageBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(10, Refill.intervally(10, Duration.ofMinutes(5))))
            .build();
    }

    /**
     * Create bucket for authenticated user messages: 20 messages per minute
     */
    public Bucket createAuthenticatedMessageBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(20, Refill.intervally(20, Duration.ofMinutes(1))))
            .build();
    }

    /**
     * Create bucket for IP violations: 5 violations per hour before temporary ban
     */
    public Bucket createIpViolationBucket() {
        return Bucket.builder()
            .addLimit(Bandwidth.classic(5, Refill.intervally(5, Duration.ofHours(1))))
            .build();
    }
}
