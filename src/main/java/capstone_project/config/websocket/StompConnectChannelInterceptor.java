package capstone_project.config.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Collections;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompConnectChannelInterceptor implements ChannelInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            
            // Try to get token from headers
            String authHeader = accessor.getFirstNativeHeader("Authorization");

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                try {
                    String token = authHeader.substring(7);
                    if (jwtTokenProvider.validateToken(token)) {
                        // Get the username from token using the available method
                        String username = jwtTokenProvider.getUsernameFromToken(token);
                        String role = jwtTokenProvider.getRoleFromToken(token);

                        if (username != null) {
                            // Check if connecting to issue WebSocket endpoints
                            String destination = accessor.getDestination();
                            if (destination != null && 
                                (destination.contains("/topic/issues/") || destination.contains("/app/issue/"))) {
                                
                                // Only allow STAFF role for issue endpoints
                                if (!"STAFF".equals(role)) {
                                    log.warn("❌ User {} with role {} attempted to connect to issue WebSocket endpoint: {}", 
                                            username, role, destination);
                                    throw new IllegalArgumentException("Only STAFF role can access issue WebSocket endpoints");
                                }
                                
                            }

                            // Create a simple Authentication object with the username and role
                            Authentication auth = new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + (role != null ? role : "USER")))
                            );
                            accessor.setUser(auth);
                            
                        }
                    }
                } catch (IllegalArgumentException e) {
                    // Re-throw authorization errors
                    throw e;
                } catch (Exception e) {
                    log.warn("Failed to process Authentication from STOMP header", e);
                }
            } else {
                
            }
        }

        return message;
    }
}
