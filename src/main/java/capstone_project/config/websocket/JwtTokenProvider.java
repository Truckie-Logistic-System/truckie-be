package capstone_project.config.websocket;

import capstone_project.common.utils.JWTUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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
            log.warn("JWT token validation failed: {}", e.getMessage());
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
}
