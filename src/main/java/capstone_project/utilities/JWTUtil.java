package capstone_project.utilities;

import capstone_project.entity.UsersEntity;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The type Jwt util.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JWTUtil {

    private static final String SECRET_KEY = "m9fP0TyWJ1tF3z2q8rB7tG6+KoW8I8sLK8JiwUEaUO8=";


    /**
     * Generate token string.
     *
     * @param usersEntity the users entity
     * @return the string
     */
    public static String generateToken(final UsersEntity usersEntity) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("username", usersEntity.getUsername() != null ? usersEntity.getUsername() : usersEntity.getEmail());
        claims.put("email", usersEntity.getEmail());
        claims.put("role", usersEntity.getRole().getRoleName());
        claims.put("status", usersEntity.getStatus());
        return createToken(claims,
                String.valueOf(usersEntity.getId()));
    }

    private static String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public static String generateRefreshToken(final UsersEntity usersEntity) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", String.valueOf(usersEntity.getId()));
        return createRefreshToken(claims,
                String.valueOf(usersEntity.getId()));
    }

    private static String createRefreshToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    private static SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
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
        System.out.println("Extracted username: " + username);
        System.out.println("Extracted email: " + email);
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
