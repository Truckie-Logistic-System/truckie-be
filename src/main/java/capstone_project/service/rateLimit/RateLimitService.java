package capstone_project.service.rateLimit;

import capstone_project.config.rateLimit.RateLimitConfig;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RateLimitConfig rateLimitConfig;
    
    // Cache buckets to avoid recreating them for each request
    private final ConcurrentHashMap<String, Bucket> guestConversationBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> guestMessageBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> authenticatedMessageBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> ipViolationBuckets = new ConcurrentHashMap<>();

    /**
     * Check if IP is allowed to create guest conversation
     */
    public boolean canCreateGuestConversation(String clientIp) {
        String key = "guest-conversation-" + clientIp;
        Bucket bucket = guestConversationBuckets.computeIfAbsent(key, 
            k -> rateLimitConfig.createGuestConversationBucket());
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (!probe.isConsumed()) {
            log.warn("Rate limit exceeded for guest conversation creation from IP: {}", clientIp);
            recordViolation(clientIp);
            return false;
        }
        
        log.debug("Guest conversation creation allowed for IP: {}, remaining tokens: {}", 
                 clientIp, probe.getRemainingTokens());
        return true;
    }

    /**
     * Check if conversation is allowed to send message (guest)
     */
    public boolean canSendGuestMessage(String conversationId, String clientIp) {
        String key = "guest-message-" + conversationId;
        Bucket bucket = guestMessageBuckets.computeIfAbsent(key, 
            k -> rateLimitConfig.createGuestMessageBucket());
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (!probe.isConsumed()) {
            log.warn("Rate limit exceeded for guest message from conversation: {}, IP: {}", 
                    conversationId, clientIp);
            recordViolation(clientIp);
            return false;
        }
        
        return true;
    }

    /**
     * Check if conversation is allowed to send message (authenticated user)
     */
    public boolean canSendAuthenticatedMessage(String conversationId, String userId) {
        String key = "auth-message-" + conversationId + "-" + userId;
        Bucket bucket = authenticatedMessageBuckets.computeIfAbsent(key, 
            k -> rateLimitConfig.createAuthenticatedMessageBucket());
        
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        
        if (!probe.isConsumed()) {
            log.warn("Rate limit exceeded for authenticated message from conversation: {}, user: {}", 
                    conversationId, userId);
            return false;
        }
        
        return true;
    }

    /**
     * Record IP violation for potential banning
     */
    private void recordViolation(String clientIp) {
        String key = "violation-" + clientIp;
        Bucket bucket = ipViolationBuckets.computeIfAbsent(key, 
            k -> rateLimitConfig.createIpViolationBucket());
        bucket.tryConsume(1);
        
        // Check if IP should be temporarily banned
        if (bucket.getAvailableTokens() <= 0) {
            log.warn("IP {} has exceeded violation limit and should be temporarily banned", clientIp);
        }
    }

    /**
     * Check if IP is temporarily banned due to violations
     */
    public boolean isIpBanned(String clientIp) {
        String key = "violation-" + clientIp;
        Bucket bucket = ipViolationBuckets.computeIfAbsent(key, 
            k -> rateLimitConfig.createIpViolationBucket());
        return bucket.getAvailableTokens() <= 0;
    }

    /**
     * Get client IP from request
     */
    public String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
