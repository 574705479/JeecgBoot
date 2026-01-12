package org.jeecg.modules.airag.cs.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.chat.entity.ChatMessage;
import org.jeecg.modules.airag.chat.service.IChatMessageService;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.entity.CsMessage;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.service.ICsMessageService;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 消息服务实现 (重构版)
 * 
 * 核心功能:
 * 1. 根据回复模式处理消息 (AI自动/AI辅助/手动)
 * 2. 消息存储到MongoDB
 * 3. 实时推送给相关方
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Service
public class CsMessageServiceImpl implements ICsMessageService {

    @Autowired
    private IChatMessageService chatMessageService;

    @Autowired
    @Lazy
    private ICsConversationService conversationService;

    @Autowired
    private CsWebSocketSessionManager sessionManager;

    // AI建议缓存 (conversationId -> suggestion)
    private final Map<String, String> aiSuggestionCache = new ConcurrentHashMap<>();

    // ==================== 消息发送 ====================

    @Override
    public CsMessage sendUserMessage(String conversationId, String userId, String userName, String content) {
        log.info("[CS-Message] 用户发送消息: conversationId={}, userId={}", conversationId, userId);
        
        // 确保会话存在
        CsConversation conversation = conversationService.getOrCreateConversation(
                conversationId, null, userId, userName);
        
        // 创建用户消息
        CsMessage userMessage = CsMessage.createUserMessage(conversationId, userId, userName, content);
        
        // 保存到MongoDB
        saveToMongo(userMessage);
        
        // 更新会话最后消息
        conversationService.updateLastMessage(conversationId, content);
        
        // 推送给所有相关客服
        pushToAgents(conversation, userMessage);
        
        // 增加客服未读数
        conversationService.incrementUnread(conversationId);
        
        // 根据回复模式处理
        int replyMode = conversation.getReplyMode() != null ? 
                conversation.getReplyMode() : CsConversation.REPLY_MODE_AI_AUTO;
        
        switch (replyMode) {
            case CsConversation.REPLY_MODE_AI_AUTO:
                // AI自动模式：生成并发送AI回复
                generateAndSendAiReply(conversation, content);
                break;
                
            case CsConversation.REPLY_MODE_AI_ASSIST:
                // AI辅助模式：生成建议推送给客服
                generateAiSuggestionAsync(conversationId, content);
                break;
                
            case CsConversation.REPLY_MODE_MANUAL:
                // 手动模式：不做任何处理，等待客服回复
                break;
        }
        
        return userMessage;
    }

    @Override
    public CsMessage sendAgentMessage(String conversationId, String agentId, String agentName, String content) {
        log.info("[CS-Message] 客服发送消息: conversationId={}, agentId={}", conversationId, agentId);
        
        // 创建客服消息（用户看到的显示为"客服"）
        CsMessage agentMessage = CsMessage.createAgentMessage(conversationId, agentId, agentName, content);
        agentMessage.setSenderName(agentName); // 显示实际客服名称
        
        // 保存到MongoDB
        saveToMongo(agentMessage);
        
        // 更新会话最后消息
        conversationService.updateLastMessage(conversationId, content);
        
        // 推送给用户
        pushToUser(conversationId, agentMessage);
        
        // 推送给其他协作客服（同步）
        pushToOtherAgents(conversationId, agentId, agentMessage);
        
        // 清除AI建议缓存
        aiSuggestionCache.remove(conversationId);
        
        return agentMessage;
    }

    @Override
    public CsMessage sendSystemMessage(String conversationId, String content) {
        log.info("[CS-Message] 系统消息: conversationId={}, content={}", conversationId, content);
        
        CsMessage systemMessage = CsMessage.createSystemMessage(conversationId, content);
        
        // 保存到MongoDB
        saveToMongo(systemMessage);
        
        // 推送给用户
        pushToUser(conversationId, systemMessage);
        
        // 推送给所有客服
        CsConversation conversation = conversationService.getConversation(conversationId);
        if (conversation != null) {
            pushToAgents(conversation, systemMessage);
        }
        
        return systemMessage;
    }

    @Override
    public CsMessage sendMessage(CsMessage message) {
        // 保存到MongoDB
        saveToMongo(message);
        
        // 更新会话
        conversationService.updateLastMessage(message.getConversationId(), message.getContent());
        
        return message;
    }

    // ==================== AI相关 ====================

    @Override
    public String generateAiSuggestion(String conversationId, String userMessage) {
        try {
            // 调用AI服务生成回复
            String suggestion = callAiService(conversationId, userMessage);
            
            // 缓存建议
            aiSuggestionCache.put(conversationId, suggestion);
            
            return suggestion;
        } catch (Exception e) {
            log.error("[CS-Message] 生成AI建议失败: conversationId={}", conversationId, e);
            return null;
        }
    }

    @Override
    public CsMessage confirmAiSuggestion(String conversationId, String suggestionId, 
                                          String agentId, String agentName, String editedContent) {
        log.info("[CS-Message] 确认AI建议: conversationId={}, agentId={}", conversationId, agentId);
        
        // 获取原始建议或使用编辑后的内容
        String content = oConvertUtils.isNotEmpty(editedContent) ? 
                editedContent : aiSuggestionCache.get(conversationId);
        
        if (oConvertUtils.isEmpty(content)) {
            log.warn("[CS-Message] AI建议不存在或已过期: conversationId={}", conversationId);
            return null;
        }
        
        // 创建消息
        CsMessage message = CsMessage.createAgentMessage(conversationId, agentId, agentName, content);
        message.setIsAiGenerated(true);
        message.setAiConfirmed(true);
        message.setAiSuggestionId(suggestionId);
        
        // 保存到MongoDB
        saveToMongo(message);
        
        // 更新会话
        conversationService.updateLastMessage(conversationId, content);
        
        // 推送给用户
        pushToUser(conversationId, message);
        
        // 推送给其他客服
        pushToOtherAgents(conversationId, agentId, message);
        
        // 清除缓存
        aiSuggestionCache.remove(conversationId);
        
        return message;
    }

    @Override
    public String getCurrentAiSuggestion(String conversationId) {
        return aiSuggestionCache.get(conversationId);
    }

    // ==================== 消息查询 ====================

    @Override
    public List<CsMessage> getMessages(String conversationId, int limit) {
        try {
            // 从MongoDB获取消息
            List<ChatMessage> chatMessages = chatMessageService.getRecentMessages(conversationId, limit);
            if (chatMessages == null) {
                return new ArrayList<>();
            }
            
            List<CsMessage> messages = new ArrayList<>();
            for (ChatMessage msg : chatMessages) {
                CsMessage csMsg = new CsMessage();
                csMsg.setId(msg.getId());
                csMsg.setConversationId(conversationId);
                csMsg.setContent(msg.getContent());
                csMsg.setSenderType(msg.getSenderType() != null ? msg.getSenderType() : CsMessage.SENDER_USER);
                csMsg.setSenderId(msg.getSenderId());
                csMsg.setSenderName(msg.getSenderName());
                csMsg.setCreateTime(msg.getCreateTime());
                messages.add(csMsg);
            }
            
            return messages;
        } catch (Exception e) {
            log.error("[CS-Message] 获取消息失败: conversationId={}", conversationId, e);
            return new ArrayList<>();
        }
    }

    @Override
    public List<CsMessage> getMessages(String conversationId, String beforeId, int limit) {
        // 简化实现，暂时不支持分页
        return getMessages(conversationId, limit);
    }

    @Override
    public List<CsMessage> getRecentMessages(String conversationId, int limit) {
        return getMessages(conversationId, limit);
    }

    // ==================== 已读状态 ====================

    @Override
    public void markAsRead(String conversationId, String userId) {
        // 清除未读数
        conversationService.clearUnread(conversationId);
    }

    @Override
    public int getUnreadCount(String conversationId) {
        CsConversation conversation = conversationService.getById(conversationId);
        return conversation != null && conversation.getUnreadCount() != null ? 
                conversation.getUnreadCount() : 0;
    }

    // ==================== 内部方法 ====================

    /**
     * 保存消息到MongoDB
     */
    private void saveToMongo(CsMessage message) {
        try {
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setConversationId(message.getConversationId());
            chatMessage.setContent(message.getContent());
            chatMessage.setSenderId(message.getSenderId());
            chatMessage.setSenderName(message.getSenderName());
            chatMessage.setCreateTime(message.getCreateTime() != null ? message.getCreateTime() : new Date());
            chatMessage.setSenderType(message.getSenderType());
            chatMessage.setMsgType(message.getMsgType() != null ? message.getMsgType() : ChatMessage.MSG_TYPE_TEXT);
            
            chatMessageService.saveMessage(chatMessage);
        } catch (Exception e) {
            log.error("[CS-Message] 保存消息到MongoDB失败", e);
        }
    }

    /**
     * 推送消息给用户
     */
    private void pushToUser(String conversationId, CsMessage message) {
        CsWebSocketMessage wsMessage = CsWebSocketMessage.builder()
                .type("message")
                .conversationId(conversationId)
                .content(message.getContent())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .senderType(message.getSenderType())
                .build();
        
        CsConversation conversation = conversationService.getById(conversationId);
        String userId = conversation != null ? conversation.getUserId() : conversationId;
        
        sessionManager.sendToUserByConversation(conversationId, userId, wsMessage);
    }

    /**
     * 推送消息给所有相关客服
     */
    private void pushToAgents(CsConversation conversation, CsMessage message) {
        CsWebSocketMessage wsMessage = CsWebSocketMessage.builder()
                .type("message")
                .conversationId(conversation.getId())
                .content(message.getContent())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .senderType(message.getSenderType())
                .build();
        
        // 推送给主负责人
        if (oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())) {
            sessionManager.sendToAgent(conversation.getOwnerAgentId(), wsMessage);
        }
        
        // 推送给协作者
        if (conversation.getCollaborators() != null) {
            for (var collab : conversation.getCollaborators()) {
                if (!collab.getAgentId().equals(conversation.getOwnerAgentId())) {
                    sessionManager.sendToAgent(collab.getAgentId(), wsMessage);
                }
            }
        }
    }

    /**
     * 推送消息给其他客服（排除发送者）
     */
    private void pushToOtherAgents(String conversationId, String excludeAgentId, CsMessage message) {
        CsConversation conversation = conversationService.getConversation(conversationId);
        if (conversation == null) {
            return;
        }
        
        CsWebSocketMessage wsMessage = CsWebSocketMessage.builder()
                .type("message")
                .conversationId(conversationId)
                .content(message.getContent())
                .senderId(message.getSenderId())
                .senderName(message.getSenderName())
                .senderType(message.getSenderType())
                .build();
        
        // 推送给主负责人
        if (oConvertUtils.isNotEmpty(conversation.getOwnerAgentId()) 
                && !conversation.getOwnerAgentId().equals(excludeAgentId)) {
            sessionManager.sendToAgent(conversation.getOwnerAgentId(), wsMessage);
        }
        
        // 推送给协作者
        if (conversation.getCollaborators() != null) {
            for (var collab : conversation.getCollaborators()) {
                if (!collab.getAgentId().equals(excludeAgentId)) {
                    sessionManager.sendToAgent(collab.getAgentId(), wsMessage);
                }
            }
        }
    }

    /**
     * 生成并发送AI回复 (AI自动模式)
     */
    @Async
    public void generateAndSendAiReply(CsConversation conversation, String userMessage) {
        try {
            String aiReply = callAiService(conversation.getId(), userMessage);
            
            if (oConvertUtils.isNotEmpty(aiReply)) {
                // 创建AI消息
                CsMessage aiMessage = CsMessage.createAiMessage(
                        conversation.getId(), "智能客服", aiReply);
                
                // 保存到MongoDB
                saveToMongo(aiMessage);
                
                // 更新会话
                conversationService.updateLastMessage(conversation.getId(), aiReply);
                
                // 推送给用户
                pushToUser(conversation.getId(), aiMessage);
                
                // 通知客服AI已回复
                if (conversation.getOwnerAgentId() != null) {
                    pushToAgents(conversation, aiMessage);
                }
            }
        } catch (Exception e) {
            log.error("[CS-Message] AI自动回复失败: conversationId={}", conversation.getId(), e);
        }
    }

    /**
     * 异步生成AI建议 (AI辅助模式)
     */
    @Async
    public void generateAiSuggestionAsync(String conversationId, String userMessage) {
        try {
            String suggestion = callAiService(conversationId, userMessage);
            
            if (oConvertUtils.isNotEmpty(suggestion)) {
                // 缓存建议
                aiSuggestionCache.put(conversationId, suggestion);
                
                // 推送给客服
                CsConversation conversation = conversationService.getConversation(conversationId);
                if (conversation != null) {
                    CsWebSocketMessage wsMessage = CsWebSocketMessage.builder()
                            .type("ai_suggestion")
                            .conversationId(conversationId)
                            .content(suggestion)
                            .build();
                    
                    if (conversation.getOwnerAgentId() != null) {
                        sessionManager.sendToAgent(conversation.getOwnerAgentId(), wsMessage);
                    }
                    
                    if (conversation.getCollaborators() != null) {
                        for (var collab : conversation.getCollaborators()) {
                            sessionManager.sendToAgent(collab.getAgentId(), wsMessage);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("[CS-Message] 生成AI建议失败: conversationId={}", conversationId, e);
        }
    }

    /**
     * 调用AI服务
     */
    private String callAiService(String conversationId, String userMessage) {
        try {
            // 这里调用现有的AI聊天服务
            // 简化实现，直接返回模拟回复
            // 实际应该调用 airagChatService.chat()
            
            log.info("[CS-Message] 调用AI服务: conversationId={}", conversationId);
            
            // TODO: 集成实际的AI服务调用
            // 目前返回简单回复
            return "感谢您的咨询，请问还有什么可以帮助您的吗？";
            
        } catch (Exception e) {
            log.error("[CS-Message] 调用AI服务失败", e);
            return null;
        }
    }

}
