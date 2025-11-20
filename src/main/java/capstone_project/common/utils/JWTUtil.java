package capstone_project.common.utils;

import capstone_project.config.security.JwtConfig;
import capstone_project.entity.auth.UserEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

/**
 * JWT Utility class for token generation, validation and parsing
 * Refactored to use Spring configuration instead of hardcoded values
 */
@Component
public class JWTUtil {

    private static JwtConfig jwtConfig;
    
    @Autowired
    public void setJwtConfig(JwtConfig config) {
        JWTUtil.jwtConfig = config;
    }


    /**
     * Generate token string.
     *
     * @param userEntity the users entity
     * @return the string
     */
    public static String generateToken(final UserEntity userEntity) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", userEntity.getUsername() != null ? userEntity.getUsername() : userEntity.getEmail());
        claims.put("email", userEntity.getEmail());
        claims.put("role", userEntity.getRole().getRoleName());
        claims.put("status", userEntity.getStatus());
        return createToken(claims,
                String.valueOf(userEntity.getId()));
    }

    private static String createToken(Map<String, Object> claims, String subject) {
        long expirationMs = jwtConfig != null ? jwtConfig.getAccessTokenExpirationMs() : 3600000;
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public static String generateRefreshToken(final UserEntity userEntity) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", String.valueOf(userEntity.getId()));
        // CRITICAL: Add UUID to ensure token uniqueness even if generated at same millisecond
        claims.put("jti", UUID.randomUUID().toString());
        return createRefreshToken(claims,
                String.valueOf(userEntity.getId()));
    }

    private static String createRefreshToken(Map<String, Object> claims, String subject) {
        long expirationMs = jwtConfig != null ? jwtConfig.getRefreshTokenExpirationMs() : 2592000000L;
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private static SecretKey getSigningKey() {
        String secret = jwtConfig != null ? jwtConfig.getSecret() : "m9fP0TyWJ1tF3z2q8rB7tG6+KoW8I8sLK8JiwUEaUO8=";
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
    
    /**
     * Extract user ID from token
     */
    public static String extractUserId(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extract role from token
     */
    public static String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }
    
    /**
     * Extract status from token
     */
    public static String extractStatus(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("status", String.class);
    }
    
    /**
     * Get token expiration time in seconds
     */
    public static long getExpirationSeconds(String token) {
        Date expiration = extractExpiration(token);
        Date now = new Date();
        return (expiration.getTime() - now.getTime()) / 1000;
    }

    /**
     * Extract username string.
     *
     * @param token the token
     * @return the string
     */
    public static String extractUsername(String token) {
        Claims claims = extractAllClaims(token);
        String username = claims.get("username", String.class);
        String email = claims.get("email", String.class);
//        System.out.println("Extracted username: " + username);
//        System.out.println("Extracted email: " + email);
        return username != null ? username : email;
    }

    /**
     * Extract expiration date.
     *
     * @param token the token
     * @return the date
     */
    public static Date extractExpiration(String token) {
        return extractClaim(token,
                Claims::getExpiration);
    }

    /**
     * Extract claim t.
     *
     * @param <T>            the type parameter
     * @param token          the token
     * @param claimsResolver the claims resolver
     * @return the t
     */
    public static <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private static Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Validate token boolean.
     *
     * @param token    the token
     * @param username the username
     * @return the boolean
     */
    public static boolean validateToken(String token, String username) {
        final String extractedUsername = extractUsername(token);
        return (extractedUsername.equals(username) && !isTokenExpired(token));
    }
}
