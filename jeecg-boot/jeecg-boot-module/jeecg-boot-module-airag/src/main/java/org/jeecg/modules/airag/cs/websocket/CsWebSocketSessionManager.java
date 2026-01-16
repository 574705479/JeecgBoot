package org.jeecg.modules.airag.cs.websocket;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket会话管理器
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Slf4j
@Component
public class CsWebSocketSessionManager {

    /**
     * 用户会话映射 (userId -> session)
     */
    private final Map<String, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    /**
     * 客服会话映射 (agentId -> session)
     */
    private final Map<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();

    /**
     * 会话到用户ID的映射 (sessionId -> userId)
     */
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    /**
     * 会话ID到WebSocket会话的映射 (conversationId -> session)
     * 用于通过conversationId直接定位用户WebSocket会话，解决无痕浏览器刷新后userId变化的问题
     */
    private final Map<String, WebSocketSession> conversationSessions = new ConcurrentHashMap<>();

    /**
     * 添加会话
     */
    public void addSession(WebSocketSession session) {
        String userId = getUserId(session);
        String userType = getUserType(session);
        String conversationId = getConversationId(session);

        if (oConvertUtils.isEmpty(userId) || oConvertUtils.isEmpty(userType)) {
            return;
        }

        sessionUserMap.put(session.getId(), userId);

        if (CsWebSocketInterceptor.USER_TYPE_AGENT.equals(userType)) {
            agentSessions.put(userId, session);
            log.info("[CS-WebSocket] 客服上线: agentId={}, 当前在线客服IDs={}", userId, agentSessions.keySet());
        } else {
            userSessions.put(userId, session);
            // 同时按conversationId存储，支持通过conversationId查找用户会话
            if (oConvertUtils.isNotEmpty(conversationId)) {
                conversationSessions.put(conversationId, session);
            }
            log.info("[CS-WebSocket] 用户上线: userId={}, conversationId={}, 当前在线用户数={}", 
                    userId, conversationId, userSessions.size());
        }
    }

    /**
     * 移除会话
     */
    public void removeSession(WebSocketSession session) {
        String userId = sessionUserMap.remove(session.getId());
        if (oConvertUtils.isEmpty(userId)) {
            return;
        }

        String userType = getUserType(session);
        String conversationId = getConversationId(session);
        
        if (CsWebSocketInterceptor.USER_TYPE_AGENT.equals(userType)) {
            agentSessions.remove(userId);
            log.info("[CS-WebSocket] 客服下线: agentId={}, 当前在线客服数={}", userId, agentSessions.size());
        } else {
            userSessions.remove(userId);
            // 同时移除conversationId映射
            if (oConvertUtils.isNotEmpty(conversationId)) {
                conversationSessions.remove(conversationId);
            }
            log.info("[CS-WebSocket] 用户下线: userId={}, conversationId={}, 当前在线用户数={}", 
                    userId, conversationId, userSessions.size());
        }
    }

    /**
     * 发送消息给用户
     */
    public void sendToUser(String userId, Object message) {
        if (oConvertUtils.isEmpty(userId)) {
            log.debug("[CS-WebSocket] 用户ID为空，跳过发送");
            return;
        }
        WebSocketSession session = userSessions.get(userId);
        sendMessage(session, message);
    }
    
    /**
     * 通过conversationId发送消息给用户
     * 优先使用conversationId查找会话，支持无痕浏览器刷新后仍能收到消息
     */
    public boolean sendToUserByConversation(String conversationId, String userId, Object message) {
        WebSocketSession session = null;
        
        // ★ 增强日志：显示当前所有在线用户
        log.info("[CS-WebSocket] 尝试发送消息给用户: conversationId={}, userId={}, " +
                "当前conversationSessions={}, 当前userSessions={}", 
                conversationId, userId, conversationSessions.keySet(), userSessions.keySet());
        
        // 优先通过conversationId查找
        if (oConvertUtils.isNotEmpty(conversationId)) {
            session = conversationSessions.get(conversationId);
            if (session != null) {
                log.info("[CS-WebSocket] 通过conversationId找到用户会话: conversationId={}", conversationId);
            }
        }
        
        // 如果conversationId找不到，尝试通过userId查找
        if (session == null && oConvertUtils.isNotEmpty(userId)) {
            session = userSessions.get(userId);
            if (session != null) {
                log.info("[CS-WebSocket] 通过userId找到用户会话: userId={}", userId);
            }
        }
        
        if (session == null) {
            log.warn("[CS-WebSocket] ★★★ 用户会话不存在，无法发送消息: conversationId={}, userId={}, " +
                    "当前conversationSessions={}, 当前userSessions={}", 
                    conversationId, userId, conversationSessions.keySet(), userSessions.keySet());
            return false;
        }
        
        if (!session.isOpen()) {
            log.warn("[CS-WebSocket] 用户会话已关闭: conversationId={}, userId={}", conversationId, userId);
            return false;
        }
        
        log.info("[CS-WebSocket] 正在发送消息给用户: conversationId={}, userId={}", conversationId, userId);
        sendMessage(session, message);
        return true;
    }

    /**
     * 发送消息给客服
     */
    public void sendToAgent(String agentId, Object message) {
        if (oConvertUtils.isEmpty(agentId)) {
            log.debug("[CS-WebSocket] 客服ID为空，跳过发送");
            return;
        }
        WebSocketSession session = agentSessions.get(agentId);
        if (session == null) {
            log.warn("[CS-WebSocket] 客服会话不存在，无法发送消息: agentId={}, 当前在线客服={}", 
                    agentId, agentSessions.keySet());
            return;
        }
        if (!session.isOpen()) {
            log.warn("[CS-WebSocket] 客服会话已关闭: agentId={}", agentId);
            return;
        }
        log.info("[CS-WebSocket] 发送消息给客服: agentId={}", agentId);
        sendMessage(session, message);
    }

    /**
     * 发送消息给所有在线客服
     */
    public void sendToAllAgents(Object message) {
        String json = toJson(message);
        for (WebSocketSession session : agentSessions.values()) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            } catch (IOException e) {
                log.error("[CS-WebSocket] 发送消息失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 发送消息
     */
    private void sendMessage(WebSocketSession session, Object message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = toJson(message);
            session.sendMessage(new TextMessage(json));
        } catch (IOException e) {
            log.error("[CS-WebSocket] 发送消息失败: {}", e.getMessage());
        }
    }

    /**
     * 转换为JSON
     */
    private String toJson(Object message) {
        if (message instanceof String) {
            return (String) message;
        }
        return JSON.toJSONString(message);
    }

    /**
     * 获取用户ID
     */
    public String getUserId(WebSocketSession session) {
        return (String) session.getAttributes().get(CsWebSocketInterceptor.ATTR_USER_ID);
    }

    /**
     * 获取用户类型
     */
    public String getUserType(WebSocketSession session) {
        return (String) session.getAttributes().get(CsWebSocketInterceptor.ATTR_USER_TYPE);
    }

    /**
     * 获取应用ID
     */
    public String getAppId(WebSocketSession session) {
        return (String) session.getAttributes().get(CsWebSocketInterceptor.ATTR_APP_ID);
    }
    
    /**
     * 获取会话ID (conversationId)
     */
    public String getConversationId(WebSocketSession session) {
        return (String) session.getAttributes().get(CsWebSocketInterceptor.ATTR_CONVERSATION_ID);
    }

    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String userId) {
        if (oConvertUtils.isEmpty(userId)) {
            return false;
        }
        WebSocketSession session = userSessions.get(userId);
        return session != null && session.isOpen();
    }
    
    /**
     * 通过conversationId检查用户是否在线
     */
    public boolean isUserOnlineByConversation(String conversationId, String userId) {
        // 优先通过conversationId查找
        if (oConvertUtils.isNotEmpty(conversationId)) {
            WebSocketSession session = conversationSessions.get(conversationId);
            if (session != null && session.isOpen()) {
                log.debug("[CS-WebSocket] 用户在线(conversationId匹配): conversationId={}", conversationId);
                return true;
            }
        }
        // 其次通过userId查找
        boolean online = isUserOnline(userId);
        log.debug("[CS-WebSocket] 用户在线状态: conversationId={}, userId={}, online={}, " +
                "conversationSessions.keys={}, userSessions.keys={}", 
                conversationId, userId, online, conversationSessions.keySet(), userSessions.keySet());
        return online;
    }
    
    /**
     * 获取所有在线用户的conversationId列表（用于调试）
     */
    public java.util.Set<String> getOnlineConversationIds() {
        return new java.util.HashSet<>(conversationSessions.keySet());
    }
    
    /**
     * 获取所有在线用户ID列表（用于调试）
     */
    public java.util.Set<String> getOnlineUserIds() {
        return new java.util.HashSet<>(userSessions.keySet());
    }

    /**
     * 检查客服是否在线
     */
    public boolean isAgentOnline(String agentId) {
        WebSocketSession session = agentSessions.get(agentId);
        return session != null && session.isOpen();
    }

    /**
     * 获取在线客服数
     */
    public int getOnlineAgentCount() {
        return agentSessions.size();
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }
}
