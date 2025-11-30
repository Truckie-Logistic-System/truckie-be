package capstone_project.service.auth;

import capstone_project.config.security.JwtConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * JWT Token Cache Service using Redis
 * Reduces database queries by caching validated JWT tokens and user details
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtConfig jwtConfig;
    
    private static final String TOKEN_CACHE_PREFIX = "jwt:token:";
    private static final String USER_CACHE_PREFIX = "jwt:user:";
    private static final String BLACKLIST_PREFIX = "jwt:blacklist:";

    /**
     * Cache validated token with user details
     */
    public void cacheValidatedToken(String token, UserDetails userDetails) {
        if (!jwtConfig.getCache().isEnabled()) {
            return;
        }
        
        try {
            String tokenKey = TOKEN_CACHE_PREFIX + token;
            String userKey = USER_CACHE_PREFIX + userDetails.getUsername();
            
            // Convert to serializable DTO
            CachedUserDetails cachedUser = CachedUserDetails.from(userDetails);
            
            redisTemplate.opsForValue().set(
                tokenKey, 
                cachedUser, 
                jwtConfig.getCache().getTtlSeconds(), 
                TimeUnit.SECONDS
            );
            
            redisTemplate.opsForValue().set(
                userKey, 
                cachedUser, 
                jwtConfig.getCache().getTtlSeconds(), 
                TimeUnit.SECONDS
            );
            
            log.debug("[JwtCache] Cached token for user: {}", userDetails.getUsername());
        } catch (Exception e) {
            log.warn("[JwtCache] Failed to cache token: {}", e.getMessage());
        }
    }

    /**
     * Get cached user details by token
     */
    public UserDetails getCachedUserByToken(String token) {
        if (!jwtConfig.getCache().isEnabled()) {
            return null;
        }
        
        try {
            String key = TOKEN_CACHE_PREFIX + token;
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached instanceof CachedUserDetails) {
                log.debug("[JwtCache] ✅ Cache hit for token");
                return ((CachedUserDetails) cached).toUserDetails();
            }
        } catch (Exception e) {
            log.warn("[JwtCache] Failed to get cached token: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Get cached user details by username
     */
    public UserDetails getCachedUserByUsername(String username) {
        if (!jwtConfig.getCache().isEnabled()) {
            return null;
        }
        
        try {
            String key = USER_CACHE_PREFIX + username;
            Object cached = redisTemplate.opsForValue().get(key);
            
            if (cached instanceof CachedUserDetails) {
                log.debug("[JwtCache] ✅ Cache hit for user: {}", username);
                return ((CachedUserDetails) cached).toUserDetails();
            }
        } catch (Exception e) {
            log.warn("[JwtCache] Failed to get cached user: {}", e.getMessage());
        }
        
        return null;
    }

    /**
     * Invalidate token cache (on logout or token revocation)
     */
    public void invalidateToken(String token, String username) {
        try {
            String tokenKey = TOKEN_CACHE_PREFIX + token;
            String userKey = USER_CACHE_PREFIX + username;
            
            redisTemplate.delete(tokenKey);
            redisTemplate.delete(userKey);
            
            log.debug("[JwtCache] Invalidated cache for user: {}", username);
        } catch (Exception e) {
            log.warn("[JwtCache] Failed to invalidate cache: {}", e.getMessage());
        }
    }

    /**
     * Add token to blacklist (for revoked/logged out tokens)
     */
    public void blacklistToken(String token, long expirationSeconds) {
        try {
            String key = BLACKLIST_PREFIX + token;
            redisTemplate.opsForValue().set(key, "REVOKED", expirationSeconds, TimeUnit.SECONDS);
            log.debug("[JwtCache] Token blacklisted");
        } catch (Exception e) {
            log.warn("[JwtCache] Failed to blacklist token: {}", e.getMessage());
        }
    }

    /**
     * Check if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            String key = BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.warn("[JwtCache] Failed to check blacklist: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Clear all user-related caches (on password change, role change, etc.)
     */
    public void clearUserCache(String username) {
        try {
            String userKey = USER_CACHE_PREFIX + username;
            redisTemplate.delete(userKey);
            log.debug("[JwtCache] Cleared user cache: {}", username);
        } catch (Exception e) {
            log.warn("[JwtCache] Failed to clear user cache: {}", e.getMessage());
        }
    }
}
