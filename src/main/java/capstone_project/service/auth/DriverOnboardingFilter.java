package capstone_project.service.auth;

import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.common.enums.UserStatusEnum;
import capstone_project.common.utils.JWTUtil;
import capstone_project.dtos.response.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Filter to restrict INACTIVE drivers from accessing non-onboarding endpoints.
 * INACTIVE drivers can only access:
 * - Login/auth endpoints
 * - Driver onboarding endpoints
 * - Refresh token endpoint
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after JwtRequestFilter
public class DriverOnboardingFilter extends OncePerRequestFilter {

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();
    private static final String ACCESS_TOKEN_COOKIE_NAME = "accessToken";

    /**
     * Endpoints that INACTIVE drivers are allowed to access
     * STRICT: Only authentication and onboarding endpoints
     */
    private static final List<String> ALLOWED_INACTIVE_DRIVER_ENDPOINTS = Arrays.asList(
            "/api/v1/drivers/onboarding/**",
            "/swagger-ui/**",
            "/v3/api-docs/**"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Check if this endpoint is allowed for inactive drivers
        if (isAllowedForInactiveDriver(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract token to check user status and role
        String jwt = extractToken(request);
        
        if (jwt == null) {
            // No token - let other filters handle it
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String status = JWTUtil.extractStatus(jwt);
            String role = JWTUtil.extractRole(jwt);

            // Check if this is an INACTIVE DRIVER trying to access restricted endpoint
            if (UserStatusEnum.INACTIVE.name().equals(status) 
                    && RoleTypeEnum.DRIVER.name().equals(role)) {
                
                log.warn("[DriverOnboardingFilter] INACTIVE driver attempting to access restricted endpoint: {}", path);
                
                handleErrorResponse(response, 
                        "Vui lòng hoàn tất đăng ký tài khoản trước khi sử dụng ứng dụng. " +
                        "Bạn cần đổi mật khẩu và chụp ảnh khuôn mặt để kích hoạt tài khoản.",
                        HttpStatus.FORBIDDEN.value());
                return;
            }

        } catch (Exception e) {
            // Token parsing error - let JwtRequestFilter handle it
            log.debug("[DriverOnboardingFilter] Could not parse token: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedForInactiveDriver(String path) {
        return ALLOWED_INACTIVE_DRIVER_ENDPOINTS.stream()
                .anyMatch(pattern -> PATH_MATCHER.match(pattern, path));
    }

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
        response.setContentType("application/json;charset=UTF-8");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(new ObjectMapper().writeValueAsString(
                ApiResponse.fail(message, status)
        ));
    }
}
