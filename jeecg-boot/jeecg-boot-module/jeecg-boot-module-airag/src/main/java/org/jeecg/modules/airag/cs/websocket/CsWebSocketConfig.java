package org.jeecg.modules.airag.cs.websocket;

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

    public CsWebSocketConfig(CsWebSocketHandler csWebSocketHandler, 
                             CsWebSocketInterceptor csWebSocketInterceptor) {
        this.csWebSocketHandler = csWebSocketHandler;
        this.csWebSocketInterceptor = csWebSocketInterceptor;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 用户端WebSocket
        registry.addHandler(csWebSocketHandler, "/ws/cs/user")
                .addInterceptors(csWebSocketInterceptor)
                .setAllowedOrigins("*");
        
        // 客服端WebSocket
        registry.addHandler(csWebSocketHandler, "/ws/cs/agent")
                .addInterceptors(csWebSocketInterceptor)
                .setAllowedOrigins("*");
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
