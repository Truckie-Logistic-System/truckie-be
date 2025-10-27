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

    @Value("${vehicle-type-rule.api.base-path}")
    private String vehicleTypeRuleApiBasePath;

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

    @Value("${room.api.base-path}")
    private String roomApiBasePath;

    @Value("${chat.api.base-path}")
    private String chatApiBasePath;

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
                    "/api/v1/auths/**",
                    "/api/loading/**",
                    "/api/v1/address/**",
                    "/api/v1/emails/**",
                    "/api/v1/notifications/**",
                    "/api/v1/transactions/stripe/webhook",
                    "/api/v1/transactions/stripe/webhook/**",
                    "/api/v1/transactions/pay-os/webhook",
                    "/api/v1/transactions/pay-os/webhook/**",
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
                    "/chat/**",
                    "/vehicle-tracking-browser/**" // Adding SockJS endpoint for browser connections
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

                        // ================= USER =================
                        .requestMatchers(userApiBasePath + "/**").authenticated()

                        // ================= VEHICLE =================
                        .requestMatchers(HttpMethod.GET, vehicleTypeApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, vehicleTypeRuleApiBasePath + "/**").authenticated()
                        .requestMatchers(vehicleTypeApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(vehicleTypeRuleApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())

                        // ================= CATEGORY =================
                        .requestMatchers(HttpMethod.GET, categoryApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, categoryPricingDetailApiBasePath + "/**").authenticated()
                        .requestMatchers(categoryApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(categoryPricingDetailApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())

                        // ================= DISTANCE & BASING PRICE =================
                        .requestMatchers(HttpMethod.GET, distanceRuleApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, basingPriceApiBasePath + "/**").authenticated()
                        .requestMatchers(distanceRuleApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(basingPriceApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(distanceBasePath + "/**").hasAnyAuthority("CUSTOMER","DRIVER","ADMIN","STAFF")

                        // ================= CONTRACT =================
                        .requestMatchers(HttpMethod.GET, contractApiBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, contractRuleApiBasePath + "/**").authenticated()
                        .requestMatchers(contractRuleApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(contractApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.CUSTOMER.name(), RoleTypeEnum.STAFF.name())

                        // ================= PENALTY =================
                        .requestMatchers(HttpMethod.GET, penaltyApiBasePath + "/**").authenticated()
                        .requestMatchers(penaltyApiBasePath + "/**").hasAuthority("ADMIN")

                        // ================= DEVICE =================
                        .requestMatchers(HttpMethod.GET, deviceTypeBasePath + "/**").authenticated()
                        .requestMatchers(HttpMethod.GET, deviceBasePath + "/**").authenticated()
                        .requestMatchers(deviceTypeBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(deviceBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name())

                        // ================= CUSTOMER =================
                        .requestMatchers(HttpMethod.GET, customerApiBasePath + "/**").authenticated()
                        .requestMatchers(customerApiBasePath + "/**")
                        .hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.CUSTOMER.name())

                        // ================= DRIVER =================
                        .requestMatchers(HttpMethod.GET, driverApiBasePath + "/**").authenticated()
                        .requestMatchers(driverApiBasePath + "/**")
                        .hasAnyAuthority(RoleTypeEnum.ADMIN.name(), RoleTypeEnum.DRIVER.name())

                        // ================= ORDER =================
                        .requestMatchers(orderDetailApiBasePath + "/**").hasAnyAuthority("CUSTOMER","ADMIN","STAFF","DRIVER")
                        .requestMatchers(orderBasePath + "/**").hasAnyAuthority("CUSTOMER","ADMIN","STAFF","DRIVER")
                        .requestMatchers(orderSizeBasePath + "/**").hasAnyAuthority("CUSTOMER","ADMIN","STAFF")

                        // ================= ISSUE =================
                        .requestMatchers(issueTypeBasePath + "/**").hasAnyAuthority("ADMIN","STAFF","DRIVER")
                        .requestMatchers(issueBasePath + "/**").hasAnyAuthority("ADMIN","STAFF","DRIVER")
                        .requestMatchers(issueImageBasePath + "/**").hasAnyAuthority("CUSTOMER","ADMIN","STAFF","DRIVER")

                        // ================= PHOTO COMPLETION =================
                        .requestMatchers(photoCompletionBasePath + "/**").hasAnyAuthority("CUSTOMER","ADMIN","STAFF","DRIVER")

                        // ================= MANAGER & ROLE =================
                        .requestMatchers(managerApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(roleApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())

                        // ================= ROOM & CHAT =================
                        .requestMatchers(roomApiBasePath + "/**").hasAnyAuthority("CUSTOMER","ADMIN","STAFF","DRIVER")
                        .requestMatchers(chatApiBasePath + "/**").hasAnyAuthority("CUSTOMER","ADMIN","STAFF","DRIVER")
                        // ================= SETTING =================
                        .requestMatchers(contractSettingApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(weightUnitSettingApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())
                        .requestMatchers(stipulationSettingApiBasePath + "/**").hasAuthority(RoleTypeEnum.ADMIN.name())

                        // ================= SEAL =================
                        .requestMatchers(sealApiBasePath + "/**").hasAnyAuthority(RoleTypeEnum.ADMIN.name(),RoleTypeEnum.DRIVER.name(),RoleTypeEnum.STAFF.name())

                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

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

