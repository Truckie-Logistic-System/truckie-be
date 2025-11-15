package capstone_project.service.auth;

import capstone_project.common.utils.JWTUtil;
import capstone_project.config.security.SecurityConfigurer;
import capstone_project.dtos.response.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

/**
 * The type Jwt request filter.
 */
@Component
@RequiredArgsConstructor
public class JwtRequestFilter extends OncePerRequestFilter {

    private final AuthUserService authUserService;
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        Logger log = LoggerFactory.getLogger(this.getClass());
        log.info("[JwtRequestFilter] ➡️ Request: {} {}", request.getMethod(), path);

        if (isPublicEndpoint(path)) {
            log.info("[JwtRequestFilter] Public endpoint - skipping token validation");
            filterChain.doFilter(request, response);
            return;
        }

        log.info("[JwtRequestFilter] Protected endpoint - validating token");

        final String authorizationHeader = request.getHeader("Authorization");

        String username = null;
        String jwt = null;

        try {
            // First try to get token from Authorization header
            if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
                jwt = authorizationHeader.substring(7);
                username = JWTUtil.extractUsername(jwt);
            }

            // If not found in header, try to get from cookies
            if (username == null && request.getCookies() != null) {
                Cookie accessTokenCookie = Arrays.stream(request.getCookies())
                        .filter(cookie -> ACCESS_TOKEN_COOKIE_NAME.equals(cookie.getName()))
                        .findFirst()
                        .orElse(null);

                if (accessTokenCookie != null) {
                    jwt = accessTokenCookie.getValue();
                    username = JWTUtil.extractUsername(jwt);
                }
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                log.info("[JwtRequestFilter] Username extracted: {}", username);
                final var user = authUserService.loadUserByUsername(username);
                log.info("[JwtRequestFilter] User loaded with authorities: {}", user.getAuthorities());

                if (JWTUtil.validateToken(jwt, user.getUsername())) {
                    UsernamePasswordAuthenticationToken authenticationToken =
                            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    log.info("[JwtRequestFilter] ✅ Authentication set successfully");
                } else {
                    log.warn("[JwtRequestFilter] ❌ Token validation failed for user: {}", username);
                }
            } else if (username == null) {
                log.warn("[JwtRequestFilter] ❌ No username extracted from token");
            } else {
                log.info("[JwtRequestFilter] Authentication already exists: {}", SecurityContextHolder.getContext().getAuthentication());
            }

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException ex) {
            handleErrorResponse(response, "Access token expired. Please refresh your token.", HttpStatus.UNAUTHORIZED.value());
        } catch (Exception ex) {
            handleErrorResponse(response, "Invalid or missing token", HttpStatus.UNAUTHORIZED.value());
        }
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
                .anyMatch(pattern -> new AntPathMatcher().match(pattern, path));
    }

}