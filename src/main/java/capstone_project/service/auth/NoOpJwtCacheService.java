package capstone_project.service.auth;

import org.springframework.context.annotation.Primary;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

/**
 * No-op implementation of JWT cache service that doesn't use Redis.
 * All cache operations are no-ops since we're not using Redis anymore.
 */
@Service
@Primary
public class NoOpJwtCacheService implements JwtCacheService {
    
    @Override
    public void cacheValidatedToken(String token, UserDetails userDetails) {
        // No-op
    }

    @Override
    public UserDetails getCachedUserByToken(String token) {
        return null; // Always return null to force database lookup
    }

    @Override
    public UserDetails getCachedUserByUsername(String username) {
        return null; // Always return null to force database lookup
    }

    @Override
    public void invalidateToken(String token, String username) {
        // No-op
    }

    @Override
    public void blacklistToken(String token, long expirationSeconds) {
        // No-op - tokens cannot be blacklisted without Redis
    }

    @Override
    public boolean isTokenBlacklisted(String token) {
        return false; // No tokens are blacklisted without Redis
    }

    @Override
    public void clearUserCache(String username) {
        // No-op
    }
}
