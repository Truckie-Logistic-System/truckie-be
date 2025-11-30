package capstone_project.service.auth;

import capstone_project.common.utils.JWTUtil;
import capstone_project.config.security.SecurityConfigurer;
import capstone_project.dtos.response.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * JWT Request Filter with caching and security improvements
 * - Token validation caching to reduce DB queries
 * - Blacklist checking for revoked tokens
 * - Better error handling and logging
 * - User status validation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtRequestFilter extends OncePerRequestFilter {

    private final AuthUserService authUserService;
    private final JwtCacheService jwtCacheService;
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";
    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip public endpoints
        if (isPublicEndpoint(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = null;
        String username = null;

        try {
            // Extract JWT token from header or cookie
            jwt = extractToken(request);
            
            if (jwt == null) {
                log.debug("[JwtRequestFilter] No token found in request to: {}", path);
                filterChain.doFilter(request, response);
                return;
            }

            // SECURITY: Check if token is blacklisted (revoked/logged out)
            if (jwtCacheService.isTokenBlacklisted(jwt)) {
                log.warn("[JwtRequestFilter] ❌ Blacklisted token attempted access");
                handleErrorResponse(response, "Token has been revoked", HttpStatus.UNAUTHORIZED.value());
                return;
            }

            // Extract username from token
            username = JWTUtil.extractUsername(jwt);
            
            if (username == null) {
                log.warn("[JwtRequestFilter] ❌ No username in token");
                handleErrorResponse(response, "Invalid token format", HttpStatus.UNAUTHORIZED.value());
                return;
            }

            // Skip if already authenticated in this request
            if (SecurityContextHolder.getContext().getAuthentication() != null) {
                filterChain.doFilter(request, response);
                return;
            }

            // Try to get user from cache first (performance optimization)
            UserDetails user = jwtCacheService.getCachedUserByToken(jwt);
            
            if (user == null) {
                // Cache miss - load from database
                log.debug("[JwtRequestFilter] Cache miss - loading user: {}", username);
                user = authUserService.loadUserByUsername(username);
            }

            // Validate token
            if (JWTUtil.validateToken(jwt, user.getUsername())) {
                
                // SECURITY: Validate user status from token claims
                String tokenStatus = JWTUtil.extractStatus(jwt);
                if ("INACTIVE".equals(tokenStatus) || "BANNED".equals(tokenStatus)) {
                    log.warn("[JwtRequestFilter] ❌ User account is {}: {}", tokenStatus, username);
                    handleErrorResponse(response, "Account is " + tokenStatus.toLowerCase(), HttpStatus.FORBIDDEN.value());
                    return;
                }
                
                // Create authentication token
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                
                // Set authentication in context
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                
                // Cache the validated token and user details
                jwtCacheService.cacheValidatedToken(jwt, user);
                
                log.debug("[JwtRequestFilter] ✅ Authenticated user: {} for {}", username, path);
            } else {
                log.warn("[JwtRequestFilter] ❌ Token validation failed for user: {}", username);
                handleErrorResponse(response, "Invalid token", HttpStatus.UNAUTHORIZED.value());
                return;
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
//            log.warn("[JwtRequestFilter] ❌ Expired token: {}", ex.getMessage());
            handleErrorResponse(response, "Access token expired. Please refresh your token.", HttpStatus.UNAUTHORIZED.value());
        } catch (JwtException ex) {
            log.warn("[JwtRequestFilter] ❌ JWT error: {}", ex.getMessage());
            handleErrorResponse(response, "Invalid token format", HttpStatus.UNAUTHORIZED.value());
        } catch (Exception ex) {
            log.error("[JwtRequestFilter] ❌ Unexpected error: {}", ex.getMessage(), ex);
            handleErrorResponse(response, "Authentication error", HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
    
    /**
     * Extract JWT token from Authorization header or cookies
     */
    private String extractToken(HttpServletRequest request) {
        // Try Authorization header first
        final String authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        // Fallback to cookies
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                    .findFirst()
                    .map(Cookie::getValue)
                    .orElse(null);
        }
        
        return null;
    }

    private void handleErrorResponse(HttpServletResponse response, String message, int status) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write(new ObjectMapper().writeValueAsString(
                ApiResponse.fail(message, status)
        ));
    }

    private boolean isPublicEndpoint(String path) {
        return Arrays.stream(SecurityConfigurer.PUBLIC_ENDPOINTS)
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

}