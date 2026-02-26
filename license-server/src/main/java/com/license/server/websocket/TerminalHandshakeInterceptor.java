package com.license.server.websocket;

import com.license.server.security.JwtProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class TerminalHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtProvider jwtProvider;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        HttpServletRequest req = servletRequest.getServletRequest();
        String uri = req.getRequestURI();
        String prefix = "/ws/terminal/";
        if (!uri.startsWith(prefix)) {
            return false;
        }
        String idStr = uri.substring(prefix.length());
        try {
            String token = resolveToken(req);
            if (token == null || token.isBlank()) {
                return false;
            }
            Claims claims = jwtProvider.parseToken(token);
            String type = String.valueOf(claims.get("type"));
            if (!"access".equals(type)) {
                return false;
            }
            attributes.put("serverId", Long.valueOf(idStr));
            attributes.put("userId", Long.valueOf(claims.getSubject()));
            attributes.put("username", String.valueOf(claims.get("username")));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String resolveToken(HttpServletRequest req) {
        String protocolHeader = req.getHeader("Sec-WebSocket-Protocol");
        if (protocolHeader != null && !protocolHeader.isBlank()) {
            // Browser may send comma-separated subprotocols.
            String[] parts = protocolHeader.split(",");
            for (String part : parts) {
                String value = part == null ? "" : part.trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
        }
        String token = req.getParameter("accessToken");
        return token == null ? null : token.trim();
    }
}
