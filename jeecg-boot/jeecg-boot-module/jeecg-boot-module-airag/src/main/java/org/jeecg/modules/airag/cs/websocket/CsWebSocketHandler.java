package org.jeecg.modules.airag.cs.websocket;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

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

    private final CsWebSocketSessionManager sessionManager;
    private final ICsMessageService messageService;
    private final ICsConversationService conversationService;
    private final ICsAgentService agentService;

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
        sessionManager.addSession(session);
        
        String userId = sessionManager.getUserId(session);
        String userType = sessionManager.getUserType(session);
        String conversationId = sessionManager.getConversationId(session);
        
        // 构建extra信息
        java.util.Map<String, Object> extra = new java.util.HashMap<>();
        extra.put("userType", userType);
        
        // 如果是用户连接，获取会话的replyMode
        if (CsWebSocketInterceptor.USER_TYPE_USER.equals(userType) && oConvertUtils.isNotEmpty(conversationId)) {
            CsConversation conversation = conversationService.getById(conversationId);
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
        if (CsWebSocketInterceptor.USER_TYPE_USER.equals(userType)) {
            notifyAgentsNewConversation(conversationId, userId);
            // 通知客服用户上线
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
            // 通知所有在线客服用户已上线
            sessionManager.sendToAllAgents(CsWebSocketMessage.builder()
                    .type("user_online")
                    .conversationId(conversationId)
                    .senderId(userId)
                    .content("用户已上线")
                    .timestamp(new java.util.Date())
                    .build());
            
            log.info("[CS-WebSocket] 通知客服用户上线: conversationId={}, userId={}", conversationId, userId);
        } catch (Exception e) {
            log.error("[CS-WebSocket] 通知客服用户上线失败: {}", e.getMessage());
        }
    }

    /**
     * 通知客服有新会话
     */
    private void notifyAgentsNewConversation(String conversationId, String userId) {
        try {
            CsWebSocketMessage notification = CsWebSocketMessage.builder()
                    .type("new_conversation")
                    .conversationId(conversationId)
                    .senderId(userId)
                    .content("有新的用户上线")
                    .timestamp(new java.util.Date())
                    .build();
            
            sessionManager.sendToAllAgents(notification);
            log.info("[CS-WebSocket] 通知客服新会话: conversationId={}", conversationId);
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
            messageService.sendUserMessage(conversationId, userId, userName, content);
        } else {
            // 客服发送消息
            var agent = agentService.getById(userId);
            String agentName = agent != null ? agent.getNickname() : "客服";
            messageService.sendAgentMessage(conversationId, userId, agentName, content, msgType, extra);
        }
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
                // 客服断开 - 延迟检查是否真的离线
                handleAgentDisconnect(userId);
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
     */
    private void handleAgentDisconnect(String agentId) {
        // 延迟5秒检查，避免页面刷新导致的误判
        new Thread(() -> {
            try {
                Thread.sleep(5000);
                if (!sessionManager.isAgentOnline(agentId)) {
                    log.info("[CS-WebSocket] 客服确认离线: agentId={}", agentId);
                    agentService.goOffline(agentId);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                if (!isShutdownError(e)) {
                    log.error("[CS-WebSocket] 处理客服离线失败: {}", e.getMessage());
                }
            }
        }).start();
    }

    /**
     * 处理用户断开连接
     */
    private void handleUserDisconnect(String conversationId, String userId) {
        if (oConvertUtils.isEmpty(conversationId)) {
            return;
        }
        
        try {
            CsConversation conversation = conversationService.getById(conversationId);
            if (conversation != null) {
                // 通知所有在线客服用户已离线（不仅限于已分配的会话）
                sessionManager.sendToAllAgents(CsWebSocketMessage.builder()
                        .type("user_offline")
                        .conversationId(conversationId)
                        .senderId(userId)
                        .content("用户已离线")
                        .timestamp(new java.util.Date())
                        .build());
                
                log.info("[CS-WebSocket] 通知客服用户离线: conversationId={}, userId={}", conversationId, userId);
                
                // 只有已分配的会话才发送系统消息
                if (conversation.getStatus() == CsConversation.STATUS_ASSIGNED) {
                    // 发送系统消息（不持久化到数据库，只推送通知）
                    messageService.sendSystemMessage(conversationId, "用户已离开会话", false);
                }
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

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }
}
