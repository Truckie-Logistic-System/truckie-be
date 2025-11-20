package capstone_project.config.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {

        String rawToken = null;

        // Try Authorization header first (preferred)
        if (request.getHeaders() != null) {
            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.toLowerCase().startsWith("bearer ")) {
                rawToken = authHeader.substring(7).trim();
            }
        }

        // Fallback to query param "token"
        if (rawToken == null && request instanceof ServletServerHttpRequest) {
            HttpServletRequest servletRequest = ((ServletServerHttpRequest) request).getServletRequest();
            String q = servletRequest.getParameter("token");
            if (q != null) rawToken = q.trim();
        }

        if (rawToken == null || rawToken.isEmpty()) {
            log.warn("WebSocket handshake rejected: no token provided");
            return false; // reject handshake when no token
        }

        // Clean token (remove any fragments)
        rawToken = rawToken.replaceAll("#", "").trim();

        // Validate token
        if (!jwtTokenProvider.validateToken(rawToken)) {
            log.warn("WebSocket handshake rejected: invalid token");
            return false;
        }

        String username = jwtTokenProvider.getUsernameFromToken(rawToken);
        attributes.put("principal", new StompPrincipal(username));

        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }
}
