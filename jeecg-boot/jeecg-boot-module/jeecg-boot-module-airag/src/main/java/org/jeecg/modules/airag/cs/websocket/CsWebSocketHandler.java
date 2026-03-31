package org.jeecg.modules.airag.cs.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.common.license.core.LicenseClientService;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import org.springframework.scheduling.annotation.Scheduled;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WebSocket消息处理器 (重构版)
 * 
 * 统一处理用户和客服的WebSocket连接与消息
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Component
public class CsWebSocketHandler implements WebSocketHandler {

    /** 客服断连延迟检查调度器 */
    private static final ScheduledExecutorService disconnectScheduler =
        Executors.newScheduledThreadPool(2, r -> {
            Thread t = new Thread(r, "cs-disconnect-check");
            t.setDaemon(true);
            return t;
        });

    /** 页面刷新等正常关闭的宽限期（秒） */
    private static final int GRACE_PERIOD_NORMAL = 5;
    /** 异常断连（1006等）的宽限期（秒） */
    private static final int GRACE_PERIOD_ABNORMAL = 10;
    /** 客服 ping 超时阈值（毫秒），超过此时间未收到 ping 则主动断开 */
    private static final long AGENT_PING_TIMEOUT_MS = 60_000;

    private final CsWebSocketSessionManager sessionManager;
    private final ICsMessageService messageService;
    private final ICsConversationService conversationService;
    private final ICsAgentService agentService;

    @Autowired(required = false)
    private LicenseClientService licenseClientService;

    public CsWebSocketHandler(CsWebSocketSessionManager sessionManager,
                              @Lazy ICsMessageService messageService,
                              @Lazy ICsConversationService conversationService,
                              @Lazy ICsAgentService agentService) {
        this.sessionManager = sessionManager;
        this.messageService = messageService;
        this.conversationService = conversationService;
        this.agentService = agentService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String userId = sessionManager.getUserId(session);
        String userType = sessionManager.getUserType(session);

        // 客服连接时，在 addSession 之前检查坐席限额（避免先覆盖旧session再拒绝导致新旧都丢失）
        if (CsWebSocketInterceptor.USER_TYPE_AGENT.equals(userType)) {
            CsAgent agent = agentService.getById(userId);
            if (agent != null && agent.getStatus() != null
                && agent.getStatus() == CsAgent.STATUS_OFFLINE) {
                if (licenseClientService != null && licenseClientService.isLicensed()
                    && licenseClientService.isQuotaExceeded("max_cs_agents")) {
                    CsWebSocketMessage quotaMsg = CsWebSocketMessage.builder()
                            .type("quota_exceeded")
                            .content("客服坐席已满，无法上线")
                            .build();
                    session.sendMessage(new TextMessage(JSON.toJSONString(quotaMsg)));
                    session.close(new CloseStatus(4005, "quota_exceeded"));
                    log.warn("[CS-WebSocket] 坐席超限拒绝连接: agentId={}", userId);
                    return;
                }
            }
        }

        sessionManager.addSession(session);
        String conversationId = sessionManager.getConversationId(session);
        
        // 构建extra信息
        java.util.Map<String, Object> extra = new java.util.HashMap<>();
        extra.put("userType", userType);
        
        // 如果是用户连接，获取会话详情（复用于 welcome 消息和客服通知）
        CsConversation conversation = null;
        if (CsWebSocketInterceptor.USER_TYPE_USER.equals(userType) && oConvertUtils.isNotEmpty(conversationId)) {
            conversation = conversationService.getById(conversationId);
            if (conversation != null) {
                extra.put("replyMode", conversation.getReplyMode() != null ? conversation.getReplyMode() : 0);
                extra.put("hasAgent", oConvertUtils.isNotEmpty(conversation.getOwnerAgentId()));
                extra.put("status", conversation.getStatus());
            }
        }
        
        // 发送连接成功消息
        CsWebSocketMessage welcome = CsWebSocketMessage.builder()
                .type("connected")
                .senderId(userId)
                .conversationId(conversationId)
                .content("连接成功")
                .extra(extra)
                .build();
        session.sendMessage(new TextMessage(JSON.toJSONString(welcome)));
        
        log.info("[CS-WebSocket] 连接建立: userId={}, userType={}, conversationId={}", 
                userId, userType, conversationId);
        
        // 如果是用户连接，通知相关客服
        if (CsWebSocketInterceptor.USER_TYPE_USER.equals(userType) && conversation != null) {
            notifyAgentsNewConversation(conversation);
            notifyAgentsUserOnline(conversationId, userId);
        }
    }

    /**
     * 通知客服用户上线
     */
    private void notifyAgentsUserOnline(String conversationId, String userId) {
        if (oConvertUtils.isEmpty(conversationId)) {
            return;
        }
        
        try {
            // 通知相关客服用户已上线
            CsWebSocketMessage message = CsWebSocketMessage.builder()
                    .type("user_online")
                    .conversationId(conversationId)
                    .senderId(userId)
                    .content("用户已上线")
                    .timestamp(new java.util.Date())
                    .build();
            conversationService.sendToRelatedAgents(conversationId, message);
            
            log.info("[CS-WebSocket] 通知客服用户上线: conversationId={}, userId={}", conversationId, userId);
        } catch (Exception e) {
            log.error("[CS-WebSocket] 通知客服用户上线失败: {}", e.getMessage());
        }
    }

    /**
     * 通知客服有新会话（携带完整会话信息）
     */
    private void notifyAgentsNewConversation(CsConversation conversation) {
        try {
            java.util.Map<String, Object> extra = new java.util.HashMap<>();
            extra.put("appId", conversation.getAppId());
            extra.put("userName", conversation.getUserName());
            extra.put("createTime", conversation.getCreateTime());
            extra.put("status", conversation.getStatus());
            extra.put("replyMode", conversation.getReplyMode());
            extra.put("ownerAgentId", conversation.getOwnerAgentId());
            extra.put("userIp", conversation.getUserIp());
            extra.put("userOs", conversation.getUserOs());
            extra.put("userOsVersion", conversation.getUserOsVersion());
            extra.put("userBrowser", conversation.getUserBrowser());
            extra.put("userBrowserVersion", conversation.getUserBrowserVersion());
            extra.put("userDeviceId", conversation.getUserDeviceId());
            extra.put("userCountry", conversation.getUserCountry());
            extra.put("userProvince", conversation.getUserProvince());
            extra.put("userCity", conversation.getUserCity());
            extra.put("userLang", conversation.getUserLang());

            CsWebSocketMessage notification = CsWebSocketMessage.builder()
                    .type("new_conversation")
                    .conversationId(conversation.getId())
                    .senderId(conversation.getUserId())
                    .senderName(conversation.getUserName())
                    .content("有新的用户上线")
                    .timestamp(new java.util.Date())
                    .extra(extra)
                    .build();
            
            sessionManager.sendToAllAgents(notification);
            log.info("[CS-WebSocket] 通知客服新会话: conversationId={}", conversation.getId());
        } catch (Exception e) {
            log.error("[CS-WebSocket] 通知客服失败: {}", e.getMessage());
        }
    }

    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        if (message instanceof TextMessage) {
            String payload = ((TextMessage) message).getPayload();
            handleTextMessage(session, payload);
        }
    }

    /**
     * 处理文本消息
     */
    private void handleTextMessage(WebSocketSession session, String payload) {
        try {
            if (sessionManager.isSessionExpired(session)) {
                sendExpiredAndClose(session);
                return;
            }
            JSONObject json = JSON.parseObject(payload);
            String type = json.getString("type");
            
            if (oConvertUtils.isEmpty(type)) {
                return;
            }

            String userId = sessionManager.getUserId(session);
            String userType = sessionManager.getUserType(session);

            switch (type) {
                case "ping":
                    handlePing(session);
                    break;
                    
                case "message":
                    handleSendMessage(json, userId, userType);
                    break;
                    
                case "read":
                    handleRead(json, userId);
                    break;
                    
                case "typing":
                    handleTyping(json, userId, userType);
                    break;
                    
                case "mode_change":
                    handleModeChange(json, userId, userType);
                    break;
                    
                case "confirm_ai":
                    handleConfirmAi(json, userId, userType);
                    break;

                case "stop_ai":
                    handleStopAi(json, userId);
                    break;

                case "stop_ai_suggestion":
                    handleStopAiSuggestion(json, userId);
                    break;
                    
                default:
                    log.debug("[CS-WebSocket] 未处理的消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("[CS-WebSocket] 处理消息失败: {}", e.getMessage(), e);
        }
    }

    /**
     * 处理心跳
     */
    private void handlePing(WebSocketSession session) throws Exception {
        if (sessionManager.isSessionExpired(session)) {
            sendExpiredAndClose(session);
            return;
        }
        // 记录客服最后 ping 时间，用于服务端空闲检测
        String userType = sessionManager.getUserType(session);
        if (CsWebSocketInterceptor.USER_TYPE_AGENT.equals(userType)) {
            String agentId = sessionManager.getUserId(session);
            if (oConvertUtils.isNotEmpty(agentId)) {
                sessionManager.updateAgentPingTime(agentId);
            }
        }
        CsWebSocketMessage pong = CsWebSocketMessage.builder()
                .type("pong")
                .timestamp(new java.util.Date())
                .build();
        session.sendMessage(new TextMessage(JSON.toJSONString(pong)));
    }

    /**
     * 处理发送消息
     */
    private void handleSendMessage(JSONObject json, String userId, String userType) {
        String conversationId = json.getString("conversationId");
        String content = json.getString("content");
        String userName = json.getString("userName");
        Integer msgType = json.getInteger("msgType");
        String extra = json.getString("extra");

        if (oConvertUtils.isEmpty(conversationId) || (oConvertUtils.isEmpty(content) && oConvertUtils.isEmpty(extra))) {
            return;
        }

        if (CsWebSocketInterceptor.USER_TYPE_USER.equals(userType)) {
            // 用户发送消息
            messageService.sendUserMessage(conversationId, userId, userName, content, msgType, extra);
        } else {
            // 客服发送消息
            var agent = agentService.getById(userId);
            String agentName = agent != null ? agent.getNickname() : "客服";
            messageService.sendAgentMessage(conversationId, userId, agentName, content, msgType, extra);
        }
    }

    /**
     * 处理终止AI回复请求
     */
    private void handleStopAi(JSONObject json, String userId) {
        String conversationId = json.getString("conversationId");
        if (oConvertUtils.isEmpty(conversationId)) {
            return;
        }
        log.info("[CS-WebSocket] 收到终止AI请求: conversationId={}, userId={}", conversationId, userId);
        messageService.cancelAiStream(conversationId);
    }

    /**
     * 处理终止AI建议请求（客服端忽略回复建议时调用）
     */
    private void handleStopAiSuggestion(JSONObject json, String userId) {
        String conversationId = json.getString("conversationId");
        if (oConvertUtils.isEmpty(conversationId)) {
            return;
        }
        log.info("[CS-WebSocket] 收到终止AI建议请求: conversationId={}, userId={}", conversationId, userId);
        messageService.cancelAiSuggestionStream(conversationId);
    }

    private void sendExpiredAndClose(WebSocketSession session) {
        try {
            CsWebSocketMessage message = CsWebSocketMessage.builder()
                    .type("system")
                    .content("访客凭证无效或已过期")
                    .timestamp(new java.util.Date())
                    .build();
            session.sendMessage(new TextMessage(JSON.toJSONString(message)));
        } catch (Exception ignored) {
        }
        try {
            session.close(new CloseStatus(4001, "token expired"));
        } catch (Exception ignored) {
        }
        sessionManager.removeSession(session);
    }

    /**
     * 处理已读
     */
    private void handleRead(JSONObject json, String userId) {
        String conversationId = json.getString("conversationId");
        if (oConvertUtils.isNotEmpty(conversationId)) {
            messageService.markAsRead(conversationId, userId);
        }
    }

    /**
     * 处理正在输入
     */
    private void handleTyping(JSONObject json, String userId, String userType) {
        String conversationId = json.getString("conversationId");
        if (oConvertUtils.isEmpty(conversationId)) {
            return;
        }

        CsConversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return;
        }

        if (CsWebSocketInterceptor.USER_TYPE_AGENT.equals(userType)) {
            // 客服正在输入，发给用户
            conversationService.notifyUser(conversationId, "typing", null);
        } else {
            // 用户正在输入，发给客服
            conversationService.notifyAgents(conversationId, "typing", null);
        }
    }

    /**
     * 处理回复模式切换
     */
    private void handleModeChange(JSONObject json, String userId, String userType) {
        if (!CsWebSocketInterceptor.USER_TYPE_AGENT.equals(userType)) {
            return;
        }
        
        String conversationId = json.getString("conversationId");
        Integer mode = json.getInteger("mode");
        
        if (oConvertUtils.isNotEmpty(conversationId) && mode != null) {
            conversationService.changeReplyMode(conversationId, mode);
            
            // 通知用户模式已切换
            String modeDesc = mode == CsConversation.REPLY_MODE_MANUAL ? "人工服务" : "AI自动回复";
            conversationService.notifyUser(conversationId, "mode_changed", modeDesc, java.util.Map.of("replyMode", mode));
        }
    }

    /**
     * 处理确认AI建议
     */
    private void handleConfirmAi(JSONObject json, String userId, String userType) {
        if (!CsWebSocketInterceptor.USER_TYPE_AGENT.equals(userType)) {
            return;
        }
        
        String conversationId = json.getString("conversationId");
        String suggestionId = json.getString("suggestionId");
        String editedContent = json.getString("editedContent");
        
        if (oConvertUtils.isEmpty(conversationId)) {
            return;
        }
        
        var agent = agentService.getById(userId);
        String agentName = agent != null ? agent.getNickname() : "客服";
        
        messageService.confirmAiSuggestion(conversationId, suggestionId, userId, agentName, editedContent);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.error("[CS-WebSocket] 传输错误: userId={}, error={}", 
                  sessionManager.getUserId(session), exception.getMessage());
        try {
            sessionManager.removeSession(session);
        } catch (Exception e) {
            log.warn("[CS-WebSocket] 传输错误后清理会话失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String userId = sessionManager.getUserId(session);
        String userType = sessionManager.getUserType(session);
        String conversationId = sessionManager.getConversationId(session);
        
        sessionManager.removeSession(session);
        
        log.info("[CS-WebSocket] 连接关闭: userId={}, userType={}, status={}", 
                userId, userType, closeStatus);
        
        try {
            if (CsWebSocketInterceptor.USER_TYPE_AGENT.equals(userType)) {
                // 客服断开 - 延迟检查是否真的离线（根据关闭状态决定宽限期）
                handleAgentDisconnect(userId, closeStatus);
            } else {
                // 用户断开 - 通知相关客服
                handleUserDisconnect(conversationId, userId);
            }
        } catch (Exception e) {
            if (!isShutdownError(e)) {
                log.error("[CS-WebSocket] 处理连接关闭失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 处理客服断开连接
     * 根据关闭状态区分宽限期：
     * - 正常关闭 (1000, page_refresh): 5秒宽限期（前端主动刷新）
     * - 异常断连 (1006等): 10秒宽限期（网络波动、意外断开）
     * - Token过期 (4001): 立即下线
     */
    private void handleAgentDisconnect(String agentId, CloseStatus closeStatus) {
        int code = closeStatus.getCode();
        
        // Token过期，立即下线
        if (code == 4001) {
            log.info("[CS-WebSocket] Token过期，客服立即下线: agentId={}", agentId);
            try {
                agentService.goOffline(agentId, "websocket_disconnect");
            } catch (Exception e) {
                if (!isShutdownError(e)) {
                    log.error("[CS-WebSocket] 处理客服离线失败: {}", e.getMessage());
                }
            }
            return;
        }
        
        // 根据关闭原因选择宽限期
        int gracePeriod;
        if (code == CloseStatus.NORMAL.getCode()) {
            // 正常关闭（如 page_refresh），较短宽限期
            gracePeriod = GRACE_PERIOD_NORMAL;
        } else {
            // 异常断连（1006等），较长宽限期
            gracePeriod = GRACE_PERIOD_ABNORMAL;
        }
        
        log.info("[CS-WebSocket] 客服断连，{}秒后检查是否真正离线: agentId={}, closeCode={}", 
                gracePeriod, agentId, code);
        
        disconnectScheduler.schedule(() -> {
            try {
                if (!sessionManager.isAgentOnline(agentId)) {
                    log.info("[CS-WebSocket] 客服确认离线: agentId={}", agentId);
                    agentService.goOffline(agentId, "websocket_disconnect");
                } else {
                    log.info("[CS-WebSocket] 客服已重连，取消离线: agentId={}", agentId);
                }
            } catch (Exception e) {
                if (!isShutdownError(e)) {
                    log.error("[CS-WebSocket] 处理客服离线失败: {}", e.getMessage());
                }
            }
        }, gracePeriod, TimeUnit.SECONDS);
    }

    /**
     * 处理用户断开连接
     */
    private void handleUserDisconnect(String conversationId, String userId) {
        if (oConvertUtils.isEmpty(conversationId)) {
            return;
        }
        
        // 多 Tab 场景：检查该用户是否还有该会话的其他活跃连接
        if (sessionManager.isUserOnlineByConversation(conversationId, userId)) {
            log.info("[CS-WebSocket] 用户仍有其他活跃连接，不发送离线通知: conversationId={}, userId={}", conversationId, userId);
            return;
        }
        
        try {
            CsConversation conversation = conversationService.getById(conversationId);
            if (conversation != null) {
                // 通知相关客服用户已离线
                CsWebSocketMessage message = CsWebSocketMessage.builder()
                        .type("user_offline")
                        .conversationId(conversationId)
                        .senderId(userId)
                        .content("用户已离线")
                        .timestamp(new java.util.Date())
                        .build();
                conversationService.sendToRelatedAgents(conversationId, message);
                
                log.info("[CS-WebSocket] 通知客服用户离线: conversationId={}, userId={}", conversationId, userId);
                
                // 用户离开不再发送系统消息提示，仅保留 user_offline 通知
            }
        } catch (Exception e) {
            log.error("[CS-WebSocket] 处理用户离线失败: {}", e.getMessage());
        }
    }

    /**
     * 判断是否为关闭时的错误
     */
    private boolean isShutdownError(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("dataSource already closed") 
                || msg.contains("DataSource")
                || msg.contains("closed"));
    }

    /**
     * 定时扫描客服 WebSocket 空闲状态
     * 客户端每 15s 发一次 ping，若 60s 内未收到任何 ping 则判定为僵尸连接并主动关闭
     */
    @Scheduled(fixedRate = 30000)
    public void checkStaleAgentSessions() {
        try {
            Map<String, Long> pingSnapshot = sessionManager.getAgentLastPingTimeSnapshot();
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : pingSnapshot.entrySet()) {
                String agentId = entry.getKey();
                long lastPing = entry.getValue();
                if (now - lastPing > AGENT_PING_TIMEOUT_MS) {
                    WebSocketSession session = sessionManager.getAgentSession(agentId);
                    if (session != null && session.isOpen()) {
                        log.warn("[CS-WebSocket] 客服 ping 超时({}ms)，主动断开: agentId={}", now - lastPing, agentId);
                        try {
                            session.close(new CloseStatus(4003, "ping_timeout"));
                        } catch (Exception e) {
                            log.warn("[CS-WebSocket] 关闭超时会话失败: agentId={}, error={}", agentId, e.getMessage());
                        }
                    }
                }
            }
        } catch (Exception e) {
            if (!isShutdownError(e)) {
                log.error("[CS-WebSocket] 空闲检测扫描异常: {}", e.getMessage());
            }
        }
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
