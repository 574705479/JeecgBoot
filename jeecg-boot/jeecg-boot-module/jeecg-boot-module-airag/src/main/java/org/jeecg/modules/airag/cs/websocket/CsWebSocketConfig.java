package org.jeecg.modules.airag.cs.websocket;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket配置
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Configuration
@EnableWebSocket
public class CsWebSocketConfig implements WebSocketConfigurer {

    private final CsWebSocketHandler csWebSocketHandler;
    private final CsWebSocketInterceptor csWebSocketInterceptor;

    /**
     * 允许的握手 Origin 列表，逗号分隔；默认 "*" 与历史行为一致。
     * 生产环境建议在 application.yml 中配置：
     *   jeecg.cs.ws.allowed-origins: https://your.domain.com,https://other.domain.com
     */
    @Value("${jeecg.cs.ws.allowed-origins:*}")
    private String allowedOrigins;

    public CsWebSocketConfig(CsWebSocketHandler csWebSocketHandler, 
                             CsWebSocketInterceptor csWebSocketInterceptor) {
        this.csWebSocketHandler = csWebSocketHandler;
        this.csWebSocketInterceptor = csWebSocketInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] origins = parseOrigins(allowedOrigins);

        // 用户端WebSocket
        registry.addHandler(csWebSocketHandler, "/ws/cs/user")
                .addInterceptors(csWebSocketInterceptor)
                .setAllowedOrigins(origins);
        
        // 客服端WebSocket
        registry.addHandler(csWebSocketHandler, "/ws/cs/agent")
                .addInterceptors(csWebSocketInterceptor)
                .setAllowedOrigins(origins);
    }

    private String[] parseOrigins(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new String[]{"*"};
        }
        String[] parts = raw.split(",");
        java.util.List<String> list = new java.util.ArrayList<>(parts.length);
        for (String p : parts) {
            String t = p.trim();
            if (!t.isEmpty()) {
                list.add(t);
            }
        }
        return list.isEmpty() ? new String[]{"*"} : list.toArray(new String[0]);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 设置消息缓冲区大小
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        // 设置会话空闲超时时间（30分钟）
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L);
        return container;
    }
}
