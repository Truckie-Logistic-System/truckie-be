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
            allowed.forEach(origin -> config.addAllowedOriginPattern(origin));
        } else {
            // IMPORTANT: Use origin patterns to support wildcards with allowCredentials=true
            config.addAllowedOriginPattern("http://localhost:*");
            config.addAllowedOriginPattern("https://*.vercel.app");
            config.addAllowedOriginPattern("https://truckie.vercel.app");
            config.addAllowedOriginPattern("https://truckie-fe.vercel.app");
            config.addAllowedOriginPattern("https://*.azurewebsites.net");
            config.addAllowedOriginPattern("http://14.225.253.8");
            config.addAllowedOriginPattern("http://14.225.253.8:*");
            config.addAllowedOriginPattern("https://truckie.io.vn");
        }

        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Disposition", "Set-Cookie"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE);
        bean.setName("corsFilter");
        return bean;
    }
}