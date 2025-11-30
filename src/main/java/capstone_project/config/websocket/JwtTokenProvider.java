package capstone_project.config.websocket;

import capstone_project.common.utils.JWTUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
@Slf4j
public class JwtTokenProvider {

    /**
     * Validate JWT token
     */
    public boolean validateToken(String token) {
        try {
            // Extract username first to check if token is parseable
            String username = JWTUtil.extractUsername(token);
            if (username == null || username.isEmpty()) {
                return false;
            }

            // Validate token with extracted username
            return JWTUtil.validateToken(token, username);
        } catch (Exception e) {
//            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract username from JWT token
     */
    public String getUsernameFromToken(String token) {
        try {
            return JWTUtil.extractUsername(token);
        } catch (Exception e) {
            log.warn("Failed to extract username from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract role from JWT token
     */
    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.warn("Failed to extract role from token: {}", e.getMessage());
            return null;
        }
    }

    private static SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode("m9fP0TyWJ1tF3z2q8rB7tG6+KoW8I8sLK8JiwUEaUO8=");
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
