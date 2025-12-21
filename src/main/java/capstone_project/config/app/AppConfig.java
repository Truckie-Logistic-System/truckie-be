package capstone_project.config.app;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
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
            // IMPORTANT: Cannot use wildcard "*" with allowCredentials=true
            // Use specific origins instead or configure in properties
            config.addAllowedOrigin("http://localhost:3000");
            config.addAllowedOrigin("http://localhost:3001");
            config.addAllowedOrigin("http://localhost:5173");
            config.addAllowedOrigin("http://localhost:5174");
            config.addAllowedOrigin("https://truckie.io.vn");
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        // IMPORTANT: Set SameSite=Lax for cookie handling during page refresh
        config.setExposedHeaders(Arrays.asList("Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setName("corsFilter");
        return bean;
    }
}