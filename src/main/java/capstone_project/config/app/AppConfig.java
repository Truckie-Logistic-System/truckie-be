package capstone_project.config.app;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class AppConfig {

    private final CorsProperties corsProperties;

    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);

        List<String> allowed = corsProperties.getAllowedOrigins();
        if (allowed != null && !allowed.isEmpty()) {
            allowed.forEach(config::addAllowedOrigin);
        } else {
            // allow all origins if none configured (use with caution)
            config.addAllowedOriginPattern("*");
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Specific TrackAsia proxy endpoints and API
        source.registerCorsConfiguration("/api/trackasia/**", config);
        source.registerCorsConfiguration("/api/**", config);
        // Optional: keep a global fallback
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setName("corsFilter");
        return bean;
    }
}