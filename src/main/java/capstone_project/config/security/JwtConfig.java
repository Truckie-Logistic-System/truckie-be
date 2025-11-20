package capstone_project.config.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * JWT Configuration properties
 * Centralized JWT settings for better security and maintainability
 */
@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtConfig {
    
    /**
     * Secret key for JWT signing (base64 encoded)
     * CRITICAL: Should be moved to environment variable in production
     */
    private String secret;
    
    /**
     * Access token expiration time in milliseconds
     * Default: 1 hour (3600000 ms)
     */
    private long accessTokenExpirationMs = 3600000;
    
    /**
     * Refresh token expiration time in milliseconds
     * Default: 30 days (2592000000 ms)
     */
    private long refreshTokenExpirationMs = 2592000000L;
    
    /**
     * Cache configuration
     */
    private Cache cache = new Cache();
    
    @Getter
    @Setter
    public static class Cache {
        /**
         * Enable/disable JWT token caching
         */
        private boolean enabled = true;
        
        /**
         * Cache TTL in seconds
         * Default: 1 hour (3600 seconds)
         */
        private int ttlSeconds = 3600;
    }
}
