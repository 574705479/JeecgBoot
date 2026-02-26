package com.license.server.config;

import com.license.server.websocket.TerminalWebSocketHandler;
import com.license.server.websocket.TerminalHandshakeInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.util.List;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketTerminalConfig implements WebSocketConfigurer {

    private final TerminalWebSocketHandler terminalWebSocketHandler;
    private final TerminalHandshakeInterceptor terminalHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(terminalWebSocketHandler, "/ws/terminal/{serverId}")
                .addInterceptors(terminalHandshakeInterceptor)
                .setHandshakeHandler(new DefaultHandshakeHandler() {
                    @Override
                    protected String selectProtocol(List<String> requestedProtocols, WebSocketHandler webSocketHandler) {
                        // Echo the first requested subprotocol so browser accepts handshake.
                        if (requestedProtocols != null && !requestedProtocols.isEmpty()) {
                            return requestedProtocols.get(0);
                        }
                        return super.selectProtocol(requestedProtocols, webSocketHandler);
                    }
                })
                .setAllowedOriginPatterns("*");
    }
}
