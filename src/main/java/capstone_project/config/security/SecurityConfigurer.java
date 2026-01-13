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
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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

    @Value("${size-rule.api.base-path}")
    private String sizeRuleApiBasePath;

    @Value("${distance-rule.api.base-path}")
    private String distanceRuleApiBasePath;

    @Value("${basing-price.api.base-path}")
    private String basingPriceApiBasePath;

    @Value("${contract.api.base-path}")
    private String contractApiBasePath;

    @Value("${contract-rule.api.base-path}")
    private String contractRuleApiBasePath;


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

    @Value("${damage-resolution.api.base-path}")
    private String damageResolutionBasePath;

    @Value("${distance.api.base-path}")
    private String distanceBasePath;

    @Value("${photo-completion.api.base-path}")
    private String photoCompletionBasePath;

    @Value("${device-type.api.base-path}")
    private String deviceTypeBasePath;

    @Value("${device.api.base-path}")
    private String deviceBasePath;

    @Value("${user.api.base-path}")
    private String userApiBasePath;

    @Value("${customer.api.base-path}")
    private String customerApiBasePath;

    @Value("${driver.api.base-path}")
    private String driverApiBasePath;


    @Value("${user-chat.api.base-path}")
    private String userChatApiBasePath;

    @Value("${seal.api.base-path}")
    private String sealApiBasePath;

    @Value("${notification.api.base-path}")
    private String notificationApiBasePath;


//    @Value("${system-setting.api.base-path}")
//    private String systemSettingApiBasePath;

    @Value("${contract-setting.api.base-path}")
    private String contractSettingApiBasePath;

    @Value("${weight-unit-setting.api.base-path}")
    private String weightUnitSettingApiBasePath;

    @Value("${stipulation-setting.api.base-path}")
    private String stipulationSettingApiBasePath;

    public static final String[] SWAGGER_ENDPOINTS = {
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            "/webjars/**"
    };

    public static final String[] PUBLIC_ENDPOINTS = Stream.concat(
            Stream.of(
                    "/",           // Root health check for Azure
                    "/health",     // Health endpoint for Azure warmup
                    "/ping",       // Quick ping endpoint
                    "/api/v1/auths/**",
                    "/api/loading/**",
                    "/api/v1/address/**",
                    "/api/v1/emails/**",
                    "/api/v1/public/stipulations/**", // Public stipulation endpoint for customers
                    "/api/v1/public/pricing/**", // Public pricing endpoint for customers/guests
                    "/api/public/chat/**", // AI Chatbot public endpoint - no auth required
                    "/api/v1/public/recipient-tracking/**", // Recipient order tracking - no auth required
                    "/api/v1/vietmap/**", // Public VietMap endpoints for recipient tracking
                    "/api/v1/transactions/stripe/webhook",
                    "/api/v1/transactions/stripe/webhook/**",
                    "/api/v1/transactions/pay-os/webhook",
                    "/api/v1/transactions/pay-os/webhook/**",
                    "/api/payos-transaction/webhook", // Fix: Add actual PayOS webhook path
                    "/api/v1/transactions/pay-os/callback",
                    "/api/v1/transactions/pay-os/callback/**",
                    "/api/v1/transactions/pay-os/cancel",
                    "/api/v1/transactions/pay-os/cancel/**",
                    "/api/webhook/**",
                    "/app/**",
                    "/topic/**",
                    "/actuator/**",
                    "/actuator/health",
                    "/actuator/info",
                    "/error",
                    "/vehicle-tracking-browser/**", // Adding SockJS endpoint for browser connections
                    "/ws/**" // Issue tracking WebSocket endpoint
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

    // JwtRequestFilter is now auto-configured via @Component and constructor injection
    // No need for explicit @Bean creation

    // CORS configuration moved to application-azure.properties
    // Using Spring Web native CORS support instead of custom bean
    // @Bean
    // public CorsConfigurationSource corsConfigurationSource() {
    //     ... custom CORS config removed ...
    // }

    @Bean
    public SecurityFilterChain configure(HttpSecurity http, JwtRequestFilter jwtRequestFilter) throws Exception {
        http
                // CORS configuration - use default Spring Web CORS (from application.properties)
                .cors(Customizer.withDefaults())
                
                // CSRF disabled for REST API (using JWT instead)
                .csrf(AbstractHttpConfigurer::disable)
                
                // Security headers
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny()) // Prevent clickjacking
                        .xssProtection(xss -> xss.disable()) // Modern browsers handle this
                        .contentTypeOptions(contentType -> contentType.disable()) // Let Spring handle
                        .cacheControl(cache -> cache.disable()) // Allow caching
                )
                
                // Authorization rules
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // ================= USER =================
                        .requestMatchers(userApiBasePath + "/**").authenticated()

                        // ================= VEHICLE =================
                        .requestMatchers(HttpMethod.GET, vehicleTypeApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, sizeRuleApiBasePath + "/**").authenticated()
                        .requestMatchers(vehicleTypeApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())
                        .requestMatchers(sizeRuleApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())

                        // ================= CATEGORY =================
                        .requestMatchers(HttpMethod.GET, categoryApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, categoryPricingDetailApiBasePath + "/**").authenticated()
                        .requestMatchers(categoryApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())
                        .requestMatchers(categoryPricingDetailApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())

                        // ================= DISTANCE & BASING PRICE =================
                        .requestMatchers(HttpMethod.GET, distanceRuleApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, basingPriceApiBasePath + "/**").authenticated()
                        .requestMatchers(distanceRuleApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())
                        .requestMatchers(basingPriceApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())
                        .requestMatchers(distanceBasePath + "/**").hasAnyAuthority(RoleTypeEnum.CUSTOMER.name(), RoleTypeEnum.DRIVER.name(), RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())

                        // ================= CONTRACT =================
                        .requestMatchers(HttpMethod.GET, contractApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, contractRuleApiBasePath + "/**").authenticated()
                        .requestMatchers(contractRuleApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(contractApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.CUSTOMER.name(), RoleTypeEnum.STAFF.name())

                        // ================= PENALTY =================
                        .requestMatchers(HttpMethod.GET, penaltyApiBasePath + "/**").authenticated()
                        .requestMatchers(penaltyApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())

                        // ================= DEVICE =================
                        .requestMatchers(HttpMethod.GET, deviceTypeBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, deviceBasePath + "/**").authenticated()
                        .requestMatchers(deviceTypeBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())
                        .requestMatchers(deviceBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())

                        // ================= CUSTOMER =================
                        .requestMatchers(HttpMethod.GET, customerApiBasePath + "/**").authenticated()
                        .requestMatchers(customerApiBasePath + "/**")
                        .hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.CUSTOMER.name())

                        // ================= DRIVER =================
                        .requestMatchers(HttpMethod.GET, driverApiBasePath + "/**").authenticated()
                        .requestMatchers(driverApiBasePath + "/**")
                        .hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.DRIVER.name())

                        // ================= ORDER =================
                        .requestMatchers(orderDetailApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.CUSTOMER.name(), RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name(), RoleTypeEnum.DRIVER.name())
                        .requestMatchers(orderBasePath + "/**").hasAnyAuthority(RoleTypeEnum.CUSTOMER.name(), RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name(), RoleTypeEnum.DRIVER.name())
                        .requestMatchers(orderSizeBasePath + "/**").hasAnyAuthority(RoleTypeEnum.CUSTOMER.name(), RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())

                        // ================= ISSUE =================
                        .requestMatchers(issueTypeBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name(), RoleTypeEnum.DRIVER.name())
                        .requestMatchers(issueBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name(), RoleTypeEnum.DRIVER.name())
                        .requestMatchers(issueImageBasePath + "/**").hasAnyAuthority(RoleTypeEnum.CUSTOMER.name(), RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name(), RoleTypeEnum.DRIVER.name())
                        
                        // ================= DAMAGE RESOLUTION =================
                        .requestMatchers(damageResolutionBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())

                        // ================= PHOTO COMPLETION =================
                        .requestMatchers(photoCompletionBasePath + "/**").hasAnyAuthority(RoleTypeEnum.CUSTOMER.name(), RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name(), RoleTypeEnum.DRIVER.name())

                        // ================= MANAGER & ROLE =================
                        .requestMatchers(managerApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(roleApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())

                        // ================= USER CHAT =================
                        // Guest chat endpoints - allow public access
                        .requestMatchers(userChatApiBasePath + "/guest/**").permitAll()
                        // Guest message sending - allow public access
                        .requestMatchers(userChatApiBasePath + "/conversations/*/messages").permitAll()
                        .requestMatchers(userChatApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.CUSTOMER.name(), RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name(), RoleTypeEnum.DRIVER.name())

                        // ================= NOTIFICATION =================
                        .requestMatchers(HttpMethod.GET, contractSettingApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name(), RoleTypeEnum.CUSTOMER.name())
                        .requestMatchers(contractSettingApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(weightUnitSettingApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(stipulationSettingApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.STAFF.name())

                        // ================= SEAL =================
                        .requestMatchers(sealApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.DRIVER.name(), RoleTypeEnum.STAFF.name())

                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                        .anyRequest().authenticated())
                // httpBasic removed to prevent browser login popup for API calls
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                // oauth2 - Disabled for Railway deployment
//                .oauth2Login(oauth2Login -> oauth2Login
//                        .successHandler((request, response, authentication) -> {
//                            response.setStatus(HttpServletResponse.SC_OK);
//                            response.setContentType("application/json");
//                            response.getWriter().write("{\"message\":\"Login successful!\"}");
//                        })
//                )
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

