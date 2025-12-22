package capstone_project.service.auth;

import org.springframework.security.core.userdetails.UserDetails;

/**
 * Interface for JWT token caching operations.
 * Implementations can provide different caching strategies (e.g., Redis, in-memory, no-op).
 */
public interface JwtCacheService {
    
    /**
     * Cache validated token with user details
     */
    void cacheValidatedToken(String token, UserDetails userDetails);
    
    /**
     * Get cached user details by token
     */
    UserDetails getCachedUserByToken(String token);
    
    /**
     * Get cached user details by username
     */
    UserDetails getCachedUserByUsername(String username);
    
    /**
     * Invalidate token cache (on logout or token revocation)
     */
    void invalidateToken(String token, String username);
    
    /**
     * Add token to blacklist (for revoked/logged out tokens)
     */
    void blacklistToken(String token, long expirationSeconds);
    
    /**
     * Check if token is blacklisted
     */
    boolean isTokenBlacklisted(String token);
    
    /**
     * Clear all user-related caches (on password change, role change, etc.)
     */
    void clearUserCache(String username);
}
