package capstone_project.config.websocket;

import capstone_project.config.app.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    private final CorsProperties corsProperties;
    private final JwtHandshakeInterceptor jwtHandshakeInterceptor;
    private final StompConnectChannelInterceptor stompConnectChannelInterceptor;

    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("websocket-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Get all allowed origins - fallback to "*" if empty
        String[] allowedOriginsArray = corsProperties.getAllowedOrigins() != null && !corsProperties.getAllowedOrigins().isEmpty()
                ? corsProperties.getAllowedOrigins().toArray(new String[0])
                : new String[]{"*"};

        // Chat endpoint (unchanged)
        registry.addEndpoint("/chat")
                .setAllowedOrigins(allowedOriginsArray);

        // Vehicle tracking endpoint with JWT handshake interceptor (for mobile clients)
        registry.addEndpoint("/vehicle-tracking")
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected Principal determineUser(
                            org.springframework.http.server.ServerHttpRequest request,
                            org.springframework.web.socket.WebSocketHandler wsHandler,
                            Map<String, Object> attributes) {
                        Object principal = attributes.get("principal");
                        if (principal instanceof Principal) {
                            return (Principal) principal;
                        }
                        return super.determineUser(request, wsHandler, attributes);
                    }
                })
                .addInterceptors(jwtHandshakeInterceptor)
                .setAllowedOrigins(allowedOriginsArray);

        // Additional vehicle tracking endpoint with SockJS support (for browser clients)
        // This endpoint doesn't require JWT during handshake, will authenticate via STOMP CONNECT instead
        registry.addEndpoint("/vehicle-tracking-browser")
                .setAllowedOrigins(allowedOriginsArray)
                .withSockJS();

        // Issue tracking endpoint with SockJS support (for staff browser clients)
        // This endpoint doesn't require JWT during handshake, will authenticate via STOMP CONNECT instead
        registry.addEndpoint("/ws")
                .setAllowedOrigins(allowedOriginsArray)
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // DEMO OPTIMIZATION: Configure for high-frequency updates
        registry.enableSimpleBroker("/topic", "/queue")
                .setHeartbeatValue(new long[]{5000, 5000}) // Reduce heartbeat for faster detection
                .setTaskScheduler(heartBeatScheduler()); // Provide TaskScheduler for heartbeat
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        // Register the STOMP channel interceptor to authenticate via STOMP CONNECT frame
        registration.interceptors(stompConnectChannelInterceptor);
    }
}