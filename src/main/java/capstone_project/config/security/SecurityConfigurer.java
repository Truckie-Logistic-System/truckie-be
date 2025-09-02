package capstone_project.config.security;


import capstone_project.common.enums.RoleTypeEnum;
import capstone_project.service.auth.AuthUserService;
import capstone_project.service.auth.JwtRequestFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Arrays;
import java.util.stream.Stream;

@EnableWebSecurity
@Configuration
@RequiredArgsConstructor
public class SecurityConfigurer {

    private final AuthUserService authUserService;

    @Value("${auth.api.base-path}")
    private String authApiBasePath;

    @Value("${manager.api.base-path}")
    private String managerApiBasePath;

    @Value("${email.api.base-path}")
    private String emailApiBasePath;

    @Value("${role.api.base-path}")
    private String roleApiBasePath;

    @Value("${vehicle-type.api.base-path}")
    private String vehicleTypeApiBasePath;

    @Value("${category.api.base-path}")
    private String categoryApiBasePath;

    @Value("${category-pricing-detail.api.base-path}")
    private String categoryPricingDetailApiBasePath;

    @Value("${vehicle-rule.api.base-path}")
    private String vehicleRuleApiBasePath;

    @Value("${distance-rule.api.base-path}")
    private String distanceRuleApiBasePath;

    @Value("${basing-price.api.base-path}")
    private String basingPriceApiBasePath;

    @Value("${contract.api.base-path}")
    private String contractApiBasePath;

    @Value("${contract-rule.api.base-path}")
    private String contractRuleApiBasePath;

    @Value("${order.api.base-path}")
    private String orderApiBasePath;

    @Value("${penalty.api.base-path}")
    private String penaltyApiBasePath;

    @Value("${order.api.base-path}")
    private String orderBasePath;

    @Value("${order-detail.api.base-path}")
    private String orderDetailApiBasePath;

    @Value("${order-size.api.base-path}")
    private String orderSizeBasePath;

    @Value("${issue-type.api.base-path}")
    private String issueTypeBasePath;

    @Value("${issue.api.base-path}")
    private String issueBasePath;

    @Value("${issue-image.api.base-path}")
    private String issueImageBasePath;

    @Value("${distance.api.base-path}")
    private String distanceBasePath;

    @Value("${photo-completion.api.base-path}}")
    private String photoCompletionBasePath;

    @Value("${device-type.api.base-path}")
    private String deviceTypeBasePath;

    @Value("${device.api.base-path}")
    private String deviceBasePath;

    public static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    public static final String[] PUBLIC_ENDPOINTS = Stream.concat(
            Stream.of(
                    "/api/v1/auths/**",
                    "/api/loading/**",
                    "/api/v1/address/**",
                    "/api/v1/emails/**",
                    "/app/**",
                    "/topic/**",
                    "/actuator/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/error"
            ),
            Arrays.stream(SWAGGER_ENDPOINTS)
    ).toArray(String[]::new);

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtRequestFilter jwtRequestFilter() {
        return new JwtRequestFilter(authUserService);
    }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()
                        .requestMatchers(HttpMethod.GET, vehicleTypeApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, categoryApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, categoryPricingDetailApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, distanceRuleApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, vehicleRuleApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, basingPriceApiBasePath + "/**").authenticated()
//                        .requestMatchers(HttpMethod.GET, orderApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, contractApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, contractRuleApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, penaltyApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, deviceTypeBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, deviceBasePath + "/**").authenticated()


                        .requestMatchers(managerApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(roleApiBasePath + "/**").hasAuthority("ADMIN")
                        .requestMatchers(vehicleTypeApiBasePath + "/**").hasAuthority("ADMIN")
                        .requestMatchers(categoryApiBasePath + "/**").hasAuthority("ADMIN")
                        .requestMatchers(categoryPricingDetailApiBasePath + "/**").hasAuthority("ADMIN")
                        .requestMatchers(distanceRuleApiBasePath + "/**").hasAuthority("ADMIN")
                        .requestMatchers(vehicleRuleApiBasePath + "/**").hasAuthority("ADMIN")
                        .requestMatchers(basingPriceApiBasePath + "/**").hasAuthority("ADMIN")
                        .requestMatchers(orderApiBasePath + "/**").hasAnyAuthority("ADMIN","STAFF","DRIVER")
                        .requestMatchers(contractApiBasePath + "/**").hasAuthority("ADMIN")
                        .requestMatchers(contractRuleApiBasePath + "/**").hasAuthority("ADMIN")
                        .requestMatchers(penaltyApiBasePath + "/**").hasAuthority("ADMIN")
                        .requestMatchers(orderDetailApiBasePath + "/**").hasAnyAuthority("ADMIN","STAFF","DRIVER")
                        .requestMatchers(orderBasePath + "/**").hasAnyAuthority("ADMIN","STAFF","DRIVER")
                        .requestMatchers(issueTypeBasePath + "/**").hasAnyAuthority("ADMIN","STAFF","DRIVER")
                        .requestMatchers(issueBasePath + "/**").hasAnyAuthority("ADMIN","STAFF","DRIVER")
                        .requestMatchers(issueImageBasePath + "/**").hasAnyAuthority("ADMIN","STAFF","DRIVER")
                        .requestMatchers(orderSizeBasePath + "/**").hasAnyAuthority("ADMIN","STAFF")
                        .requestMatchers(distanceBasePath + "/**").hasAnyAuthority("CUSTOMER","DRIVER","ADMIN","STAFF")
                        .requestMatchers(photoCompletionBasePath + "/**").hasAnyAuthority("ADMIN","STAFF","DRIVER")
                        .requestMatchers(deviceTypeBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(deviceBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name())
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .addFilterBefore(jwtRequestFilter(), UsernamePasswordAuthenticationFilter.class)
                // oauth2
                .oauth2Login(oauth2Login -> oauth2Login
                        .successHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Login successful!\"}");
                        })
                )
                // logout
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auths/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID", "JWT_TOKEN")
                        .logoutSuccessUrl("/api/v1/auths/login")
                );
        return http.build();
    }

}
