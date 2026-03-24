package org.jeecg.modules.airag.cs.websocket;

import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * WebSocket会话管理器
 * 
 * 支持同一用户多设备/多浏览器同时在线：
 * - 同一个userId可以有多个WebSocketSession
 * - 发消息时向该userId的所有session广播
 * - 只有当最后一个session断开时，才判定用户离线
 * 
 * @author jeecg
 * @date 2026-01-07
 */
@Slf4j
@Component
public class CsWebSocketSessionManager {

    /**
     * 用户会话映射 (userId -> sessions)  支持多设备同时在线
     */
    private final Map<String, Set<WebSocketSession>> userSessions = new ConcurrentHashMap<>();

    /**
     * 客服会话映射 (agentId -> session)  客服端通常只有一个浏览器
     */
    private final Map<String, WebSocketSession> agentSessions = new ConcurrentHashMap<>();

    /**
     * 客服最后一次 ping 时间 (agentId -> timestamp)  用于服务端空闲检测
     */
    private final Map<String, Long> agentLastPingTime = new ConcurrentHashMap<>();

    /**
     * 会话到用户ID的映射 (sessionId -> userId)
     */
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();
    
    /**
     * 会话ID到WebSocket会话的映射 (conversationId -> sessions)
     * 支持多设备同时在线
     */
    private final Map<String, Set<WebSocketSession>> conversationSessions = new ConcurrentHashMap<>();

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
            WebSocketSession oldSession = agentSessions.put(userId, session);
            agentLastPingTime.put(userId, System.currentTimeMillis());
            // 关闭被覆盖的旧僵尸 session
            if (oldSession != null && oldSession.isOpen() && !oldSession.getId().equals(session.getId())) {
                try {
                    oldSession.close(new CloseStatus(4002, "replaced_by_new_session"));
                } catch (Exception e) {
                    log.warn("[CS-WebSocket] 关闭旧客服会话失败: agentId={}, error={}", userId, e.getMessage());
                }
            }
            log.info("[CS-WebSocket] 客服上线: agentId={}, 当前在线客服IDs={}", userId, agentSessions.keySet());
        } else {
            // 添加到用户会话集合（支持多设备）
            userSessions.computeIfAbsent(userId, k -> new CopyOnWriteArraySet<>()).add(session);
            // 同时按conversationId存储
            if (oConvertUtils.isNotEmpty(conversationId)) {
                conversationSessions.computeIfAbsent(conversationId, k -> new CopyOnWriteArraySet<>()).add(session);
            }
            int sessionCount = userSessions.getOrDefault(userId, Set.of()).size();
            log.info("[CS-WebSocket] 用户上线: userId={}, conversationId={}, 该用户会话数={}, 在线用户数={}", 
                    userId, conversationId, sessionCount, userSessions.size());
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
            // 客服只移除匹配的session（防止新session被错误移除）
            boolean removed = agentSessions.remove(userId, session);
            if (removed) {
                agentLastPingTime.remove(userId);
            }
            log.info("[CS-WebSocket] 客服下线: agentId={}, removed={}, 当前在线客服数={}", userId, removed, agentSessions.size());
        } else {
            // 从用户会话集合中移除当前session
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null) {
                sessions.remove(session);
                // 如果该用户没有任何活跃session了，移除整个entry
                if (sessions.isEmpty()) {
                    userSessions.remove(userId);
                }
            }
            // 同时从conversationId映射中移除
            if (oConvertUtils.isNotEmpty(conversationId)) {
                Set<WebSocketSession> convSessions = conversationSessions.get(conversationId);
                if (convSessions != null) {
                    convSessions.remove(session);
                    if (convSessions.isEmpty()) {
                        conversationSessions.remove(conversationId);
                    }
                }
            }
            int remaining = userSessions.containsKey(userId) ? userSessions.get(userId).size() : 0;
            log.info("[CS-WebSocket] 用户会话断开: userId={}, conversationId={}, 剩余会话数={}, 在线用户数={}", 
                    userId, conversationId, remaining, userSessions.size());
        }
    }

    /**
     * 发送消息给用户（向该用户的所有session广播）
     */
    public void sendToUser(String userId, Object message) {
        if (oConvertUtils.isEmpty(userId)) {
            log.debug("[CS-WebSocket] 用户ID为空，跳过发送");
            return;
        }
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return;
        }
        String json = toJson(message);
        for (WebSocketSession session : sessions) {
            if (session != null && session.isOpen()) {
                if (isSessionExpired(session)) {
                    closeExpiredSession(session, "token expired");
                    continue;
                }
                sendRawMessage(session, json);
            }
        }
    }
    
    /**
     * 通过conversationId发送消息给用户（向所有匹配的session广播）
     */
    public boolean sendToUserByConversation(String conversationId, String userId, Object message) {
        String json = toJson(message);
        boolean sent = false;
        
        // 优先通过conversationId查找并发送
        if (oConvertUtils.isNotEmpty(conversationId)) {
            Set<WebSocketSession> convSessions = conversationSessions.get(conversationId);
            if (convSessions != null && !convSessions.isEmpty()) {
                for (WebSocketSession session : convSessions) {
                    if (session != null && session.isOpen()) {
                        if (isSessionExpired(session)) {
                            closeExpiredSession(session, "token expired");
                            continue;
                        }
                        sendRawMessage(session, json);
                        sent = true;
                    }
                }
                if (sent) {
                    log.info("[CS-WebSocket] 通过conversationId发送消息成功: conversationId={}, 目标会话数={}", 
                            conversationId, convSessions.size());
                    return true;
                }
            }
        }
        
        // 如果conversationId找不到，尝试通过userId发送
        if (!sent && oConvertUtils.isNotEmpty(userId)) {
            Set<WebSocketSession> sessions = userSessions.get(userId);
            if (sessions != null && !sessions.isEmpty()) {
                for (WebSocketSession session : sessions) {
                    if (session != null && session.isOpen()) {
                        if (isSessionExpired(session)) {
                            closeExpiredSession(session, "token expired");
                            continue;
                        }
                        sendRawMessage(session, json);
                        sent = true;
                    }
                }
                if (sent) {
                    log.info("[CS-WebSocket] 通过userId发送消息成功: userId={}", userId);
                    return true;
                }
            }
        }
        
        log.warn("[CS-WebSocket] 用户会话不存在，无法发送消息: conversationId={}, userId={}", conversationId, userId);
        return false;
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
            sendRawMessage(session, json);
        }
    }

    /**
     * 发送消息（单个session）
     */
    private void sendMessage(WebSocketSession session, Object message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        sendRawMessage(session, toJson(message));
    }

    /**
     * 发送已序列化的JSON消息（避免重复序列化）
     * WebSocketSession.sendMessage 不是线程安全的，需要对同一 session 加锁
     */
    private void sendRawMessage(WebSocketSession session, String json) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(json));
                }
            }
        } catch (IOException e) {
            log.error("[CS-WebSocket] 发送消息失败: {}", e.getMessage());
        }
    }

    public boolean isSessionExpired(WebSocketSession session) {
        Object expireAtObj = session.getAttributes().get(CsWebSocketInterceptor.ATTR_TOKEN_EXPIRE_AT);
        if (expireAtObj == null) {
            return false;
        }
        long expireAt;
        if (expireAtObj instanceof Number) {
            expireAt = ((Number) expireAtObj).longValue();
        } else {
            try {
                expireAt = Long.parseLong(String.valueOf(expireAtObj));
            } catch (Exception e) {
                return false;
            }
        }
        return expireAt > 0 && expireAt < System.currentTimeMillis();
    }

    private void closeExpiredSession(WebSocketSession session, String reason) {
        try {
            removeSession(session);
            session.close(new CloseStatus(4001, reason));
        } catch (Exception e) {
            log.warn("[CS-WebSocket] 关闭过期会话失败: {}", e.getMessage());
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
        Set<WebSocketSession> sessions = userSessions.get(userId);
        if (sessions == null || sessions.isEmpty()) {
            return false;
        }
        // 检查是否有至少一个open的session
        for (WebSocketSession session : sessions) {
            if (session != null && session.isOpen()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 通过conversationId检查用户是否在线
     */
    public boolean isUserOnlineByConversation(String conversationId, String userId) {
        // 优先通过conversationId查找
        if (oConvertUtils.isNotEmpty(conversationId)) {
            Set<WebSocketSession> sessions = conversationSessions.get(conversationId);
            if (sessions != null) {
                for (WebSocketSession session : sessions) {
                    if (session != null && session.isOpen()) {
                        return true;
                    }
                }
            }
        }
        // 其次通过userId查找
        return isUserOnline(userId);
    }
    
    /**
     * 获取所有在线用户的conversationId列表
     */
    public java.util.Set<String> getOnlineConversationIds() {
        return new java.util.HashSet<>(conversationSessions.keySet());
    }
    
    /**
     * 获取所有在线用户ID列表
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
     * 获取客服 WebSocket session
     */
    public WebSocketSession getAgentSession(String agentId) {
        return agentSessions.get(agentId);
    }

    /**
     * 更新客服 ping 时间
     */
    public void updateAgentPingTime(String agentId) {
        agentLastPingTime.put(agentId, System.currentTimeMillis());
    }

    /**
     * 获取客服最后 ping 时间快照（用于空闲检测扫描）
     */
    public Map<String, Long> getAgentLastPingTimeSnapshot() {
        return new ConcurrentHashMap<>(agentLastPingTime);
    }

    /**
     * 获取在线用户数
     */
    public int getOnlineUserCount() {
        return userSessions.size();
    }
}
