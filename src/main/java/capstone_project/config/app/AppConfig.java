package capstone_project.config.app;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AppConfig {

    private final CorsProperties corsProperties;

    /**
     * Custom CORS Filter that handles preflight OPTIONS requests properly
     * and sets CORS headers for all responses including error responses
     */
    @Bean
    public FilterRegistrationBean<Filter> corsFilter() {
        Filter corsFilter = new Filter() {
            @Override
            public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletResponse response = (HttpServletResponse) res;
                HttpServletRequest request = (HttpServletRequest) req;
                
                String origin = request.getHeader("Origin");
                
                // Check if origin is allowed
                if (origin != null && isOriginAllowed(origin)) {
                    response.setHeader("Access-Control-Allow-Origin", origin);
                    response.setHeader("Access-Control-Allow-Credentials", "true");
                } else if (origin == null) {
                    // No origin header (same-origin or non-browser request)
                    response.setHeader("Access-Control-Allow-Origin", "*");
                    response.setHeader("Access-Control-Allow-Credentials", "false");
                }
                
                response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD");
                response.setHeader("Access-Control-Allow-Headers", "Origin, Content-Type, Accept, Authorization, X-Requested-With, Cache-Control");
                response.setHeader("Access-Control-Expose-Headers", "Authorization, Content-Disposition, Set-Cookie");
                response.setHeader("Access-Control-Max-Age", "3600");
                
                // Handle preflight OPTIONS request immediately
                if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                    response.setStatus(HttpServletResponse.SC_OK);
                    return;
                }
                
                chain.doFilter(req, res);
            }
            
            private boolean isOriginAllowed(String origin) {
                // Get allowed origins from configuration
                List<String> allowed = corsProperties.getAllowedOrigins();
                
                // Default allowed patterns if not configured
                List<String> defaultPatterns = Arrays.asList(
                    "http://localhost",
                    "https://truckie.io.vn",
                    ".vercel.app",
                    ".azurewebsites.net",
                    ".azure.com",
                    "14.225.253.8"
                );
                
                // Check configured origins
                if (allowed != null && !allowed.isEmpty()) {
                    for (String pattern : allowed) {
                        if (matchesPattern(origin, pattern)) {
                            return true;
                        }
                    }
                }
                
                // Check default patterns
                for (String pattern : defaultPatterns) {
                    if (matchesPattern(origin, pattern)) {
                        return true;
                    }
                }
                
                log.warn("CORS: Origin not allowed: {}", origin);
                return false;
            }
            
            private boolean matchesPattern(String origin, String pattern) {
                if (pattern.equals("*")) {
                    return true;
                }
                // Handle wildcard patterns
                if (pattern.startsWith("*") || pattern.startsWith(".")) {
                    String suffix = pattern.startsWith("*") ? pattern.substring(1) : pattern;
                    return origin.endsWith(suffix);
                }
                // Handle exact match or prefix match
                return origin.equals(pattern) || origin.startsWith(pattern);
            }

            @Override
            public void init(FilterConfig filterConfig) {
                log.info("CORS Filter initialized with HIGHEST_PRECEDENCE");
            }

            @Override
            public void destroy() {}
        };

        FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(corsFilter);
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setName("corsFilter");
        bean.addUrlPatterns("/*");
        return bean;
    }
}