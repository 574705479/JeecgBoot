package org.jeecg.modules.airag.chat.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.chat.entity.ChatMessage;
import org.jeecg.modules.airag.chat.repository.ChatMessageRepository;
import org.jeecg.modules.airag.chat.service.IChatMessageService;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一聊天消息服务实现（MongoDB存储）
 *
 * @author jeecg
 * @date 2026-01-08
 */
@Slf4j
@Service
public class ChatMessageServiceImpl implements IChatMessageService {

    @Autowired
    private ChatMessageRepository messageRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    @Lazy
    private ICsConversationService csConversationService;

    // ==================== 消息保存 ====================

    @Override
    public ChatMessage saveMessage(ChatMessage message) {
        if (message.getId() == null) {
            message.setId(UUID.randomUUID().toString().replace("-", ""));
        }
        if (message.getCreateTime() == null) {
            message.setCreateTime(new Date());
        }
        message.setDeleted(false);
        
        log.debug("[ChatMessage] 保存消息: conversationId={}, senderType={}, content={}",
                message.getConversationId(), message.getSenderType(), 
                message.getContent() != null && message.getContent().length() > 50 
                    ? message.getContent().substring(0, 50) + "..." : message.getContent());
        
        return messageRepository.save(message);
    }

    @Override
    public ChatMessage saveAiUserMessage(String conversationId, String appId, String userId,
                                          String externalUserId, String externalUserName,
                                          String content, List<String> images) {
        ChatMessage message = new ChatMessage()
                .setConversationId(conversationId)
                .setAppId(appId)
                .setConversationType(ChatMessage.CONV_TYPE_AI)
                .setUserId(userId)
                .setExternalUserId(externalUserId)
                .setExternalUserName(externalUserName)
                .setSenderId(oConvertUtils.isNotEmpty(externalUserId) ? externalUserId : userId)
                .setSenderType(ChatMessage.SENDER_USER)
                .setSenderName(oConvertUtils.isNotEmpty(externalUserName) ? externalUserName : "用户")
                .setContent(content)
                .setMsgType(ChatMessage.MSG_TYPE_TEXT);
        
        // 处理图片
        if (images != null && !images.isEmpty()) {
            Map<String, Object> extra = new HashMap<>();
            extra.put("images", images);
            message.setExtra(extra);
        }
        
        ChatMessage savedMessage = saveMessage(message);
        
        // ★ 自动同步到客服会话系统
        syncToCsConversation(conversationId, appId, 
                oConvertUtils.isNotEmpty(externalUserId) ? externalUserId : userId,
                oConvertUtils.isNotEmpty(externalUserName) ? externalUserName : "访客",
                content);
        
        return savedMessage;
    }
    
    /**
     * 同步到客服会话系统
     * 用于在新架构中自动创建/更新客服会话记录
     */
    private void syncToCsConversation(String conversationId, String appId, 
                                       String visitorId, String visitorName, String content) {
        log.info("[ChatMessage] 开始同步客服会话: conversationId={}, appId={}, visitorId={}", 
                conversationId, appId, visitorId);
        try {
            if (csConversationService == null) {
                log.warn("[ChatMessage] csConversationService 为空，无法同步");
                return;
            }
            if (oConvertUtils.isEmpty(conversationId)) {
                log.warn("[ChatMessage] conversationId 为空，无法同步");
                return;
            }
            // 确保会话存在 (如果不存在则创建)
            csConversationService.getOrCreateConversation(conversationId, appId, visitorId, visitorName);
            // 更新最后消息
            csConversationService.updateLastMessage(conversationId, content);
            log.info("[ChatMessage] ★ 同步客服会话成功: conversationId={}", conversationId);
        } catch (Exception e) {
            // 同步失败不影响主流程
            log.warn("[ChatMessage] 同步客服会话失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public ChatMessage saveAiAssistantMessage(String conversationId, String appId,
                                               String content, String modelName,
                                               Integer tokenUsage, String referenceKnowledge) {
        ChatMessage message = new ChatMessage()
                .setConversationId(conversationId)
                .setAppId(appId)
                .setConversationType(ChatMessage.CONV_TYPE_AI)
                .setSenderId("ai")
                .setSenderType(ChatMessage.SENDER_AI)
                .setSenderName("AI助手")
                .setContent(content)
                .setMsgType(ChatMessage.MSG_TYPE_TEXT)
                .setModelName(modelName)
                .setTokenUsage(tokenUsage)
                .setReferenceKnowledge(referenceKnowledge);
        
        return saveMessage(message);
    }

    @Override
    public ChatMessage saveAgentMessage(String conversationId, String agentId,
                                         String agentName, String agentAvatar, String content) {
        ChatMessage message = new ChatMessage()
                .setConversationId(conversationId)
                .setConversationType(ChatMessage.CONV_TYPE_AGENT)
                .setSenderId(agentId)
                .setSenderType(ChatMessage.SENDER_AGENT)
                .setSenderName(agentName)
                .setSenderAvatar(agentAvatar)
                .setAgentId(agentId)
                .setContent(content)
                .setMsgType(ChatMessage.MSG_TYPE_TEXT);
        
        return saveMessage(message);
    }

    @Override
    public ChatMessage saveUserToAgentMessage(String conversationId, String userId,
                                               String userName, String content) {
        ChatMessage message = new ChatMessage()
                .setConversationId(conversationId)
                .setConversationType(ChatMessage.CONV_TYPE_AGENT)
                .setSenderId(userId)
                .setSenderType(ChatMessage.SENDER_USER)
                .setSenderName(oConvertUtils.isNotEmpty(userName) ? userName : "访客")
                .setContent(content)
                .setMsgType(ChatMessage.MSG_TYPE_TEXT)
                .setIsRead(false);
        
        return saveMessage(message);
    }

    @Override
    public ChatMessage saveSystemMessage(String conversationId, String content) {
        ChatMessage message = new ChatMessage()
                .setConversationId(conversationId)
                .setSenderId("system")
                .setSenderType(ChatMessage.SENDER_SYSTEM)
                .setSenderName("系统")
                .setContent(content)
                .setMsgType(ChatMessage.MSG_TYPE_TEXT);
        
        return saveMessage(message);
    }

    // ==================== 消息查询 ====================

    @Override
    public List<ChatMessage> getMessages(String conversationId) {
        return messageRepository.findByConversationIdAndDeletedIsFalseOrderByCreateTimeAsc(conversationId);
    }

    @Override
    public List<ChatMessage> getRecentMessages(String conversationId, int limit) {
        List<ChatMessage> messages = messageRepository.findTop100ByConversationIdAndDeletedIsFalseOrderByCreateTimeDesc(conversationId);
        
        // 限制数量并反转顺序（变为时间升序）
        if (messages.size() > limit) {
            messages = messages.subList(0, limit);
        }
        Collections.reverse(messages);
        return messages;
    }

    @Override
    public ChatMessage getLastMessage(String conversationId) {
        return messageRepository.findTopByConversationIdAndDeletedIsFalseOrderByCreateTimeDesc(conversationId);
    }

    @Override
    public long getMessageCount(String conversationId) {
        return messageRepository.countByConversationIdAndDeletedIsFalse(conversationId);
    }

    // ==================== 会话管理 ====================

    @Override
    public List<Map<String, Object>> getUserConversations(String appId, String userId, String externalUserId) {
        // 使用聚合查询获取用户的会话列表
        Criteria criteria = new Criteria();
        criteria.and("appId").is(appId);
        criteria.and("deleted").ne(true);
        
        if (oConvertUtils.isNotEmpty(externalUserId)) {
            criteria.and("externalUserId").is(externalUserId);
        } else if (oConvertUtils.isNotEmpty(userId)) {
            criteria.and("userId").is(userId);
        } else {
            return new ArrayList<>();
        }
        
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(criteria),
                Aggregation.group("conversationId")
                        .first("conversationId").as("conversationId")
                        .first("appId").as("appId")
                        .first("externalUserName").as("userName")
                        .max("createTime").as("lastMessageTime")
                        .last("content").as("lastMessage")
                        .count().as("messageCount"),
                Aggregation.sort(Sort.Direction.DESC, "lastMessageTime"),
                Aggregation.limit(50)
        );
        
        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "chat_messages", Map.class);
        
        return results.getMappedResults().stream()
                .map(m -> {
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("conversationId", m.get("conversationId"));
                    conv.put("appId", m.get("appId"));
                    conv.put("userName", m.get("userName"));
                    conv.put("lastMessage", m.get("lastMessage"));
                    conv.put("lastMessageTime", m.get("lastMessageTime"));
                    conv.put("messageCount", m.get("messageCount"));
                    return conv;
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getActiveAiConversations(int limit) {
        // 查询最近1小时内有消息的AI会话
        Date since = new Date(System.currentTimeMillis() - 3600 * 1000);
        
        Aggregation aggregation = Aggregation.newAggregation(
                Aggregation.match(Criteria.where("conversationType").is(ChatMessage.CONV_TYPE_AI)
                        .and("deleted").ne(true)
                        .and("createTime").gte(since)),
                Aggregation.group("conversationId")
                        .first("conversationId").as("conversationId")
                        .first("appId").as("appId")
                        .first("userId").as("userId")
                        .first("externalUserId").as("externalUserId")
                        .first("externalUserName").as("userName")
                        .max("createTime").as("updateTime")
                        .last("content").as("lastMessage")
                        .count().as("messageCount"),
                Aggregation.sort(Sort.Direction.DESC, "updateTime"),
                Aggregation.limit(limit)
        );
        
        AggregationResults<Map> results = mongoTemplate.aggregate(aggregation, "chat_messages", Map.class);
        
        return results.getMappedResults().stream()
                .map(m -> {
                    Map<String, Object> conv = new HashMap<>();
                    conv.put("conversationId", m.get("conversationId"));
                    conv.put("appId", m.get("appId"));
                    conv.put("userId", m.get("userId") != null ? m.get("userId") : m.get("externalUserId"));
                    conv.put("userName", m.get("userName") != null ? m.get("userName") : "访客");
                    conv.put("lastMessage", truncateContent(String.valueOf(m.get("lastMessage")), 50));
                    conv.put("updateTime", m.get("updateTime"));
                    conv.put("messageCount", m.get("messageCount"));
                    return conv;
                })
                .collect(Collectors.toList());
    }

    @Override
    public Date getLastMessageTime(String conversationId) {
        // 查询会话中最后一条消息的时间
        Query query = new Query(Criteria.where("conversationId").is(conversationId)
                .and("deleted").ne(true))
                .with(Sort.by(Sort.Direction.DESC, "createTime"))
                .limit(1);
        ChatMessage lastMsg = mongoTemplate.findOne(query, ChatMessage.class);
        return lastMsg != null ? lastMsg.getCreateTime() : null;
    }

    @Override
    public void deleteConversation(String conversationId) {
        // 软删除
        Query query = new Query(Criteria.where("conversationId").is(conversationId));
        Update update = new Update()
                .set("deleted", true)
                .set("deleteTime", new Date());
        mongoTemplate.updateMulti(query, update, ChatMessage.class);
        
        log.info("[ChatMessage] 删除会话: conversationId={}", conversationId);
    }

    @Override
    public void clearMessages(String conversationId) {
        deleteConversation(conversationId);
    }

    // ==================== 消息状态 ====================

    @Override
    public void markAsRead(String conversationId, Integer senderType) {
        Query query = new Query(Criteria.where("conversationId").is(conversationId)
                .and("senderType").is(senderType)
                .and("isRead").is(false));
        Update update = new Update()
                .set("isRead", true)
                .set("readTime", new Date());
        mongoTemplate.updateMulti(query, update, ChatMessage.class);
    }

    @Override
    public long getUnreadCount(String conversationId, Integer senderType) {
        return messageRepository.countByConversationIdAndIsReadFalseAndSenderTypeAndDeletedIsFalse(
                conversationId, senderType);
    }

    // ==================== 数据迁移 ====================

    @Override
    public void migrateFromRedis(String conversationId, String appId, String userId,
                                  String externalUserId, List<Map<String, Object>> messages) {
        log.info("[ChatMessage] 从Redis迁移消息: conversationId={}, messageCount={}", 
                conversationId, messages != null ? messages.size() : 0);
        
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            ChatMessage message = new ChatMessage()
                    .setConversationId(conversationId)
                    .setAppId(appId)
                    .setConversationType(ChatMessage.CONV_TYPE_AI)
                    .setUserId(userId)
                    .setExternalUserId(externalUserId)
                    .setDeleted(false);
            
            String role = String.valueOf(msg.get("role"));
            String content = String.valueOf(msg.get("content"));
            
            if ("user".equals(role)) {
                message.setSenderId(oConvertUtils.isNotEmpty(externalUserId) ? externalUserId : userId)
                        .setSenderType(ChatMessage.SENDER_USER)
                        .setSenderName("用户");
            } else {
                message.setSenderId("ai")
                        .setSenderType(ChatMessage.SENDER_AI)
                        .setSenderName("AI助手");
            }
            
            message.setContent(content)
                    .setMsgType(ChatMessage.MSG_TYPE_TEXT)
                    .setCreateTime(msg.get("dateTime") != null ? 
                            parseDate(msg.get("dateTime")) : new Date());
            
            chatMessages.add(message);
        }
        
        if (!chatMessages.isEmpty()) {
            messageRepository.saveAll(chatMessages);
        }
    }

    @Override
    public void migrateFromMysql(String conversationId, List<Map<String, Object>> messages) {
        log.info("[ChatMessage] 从MySQL迁移消息: conversationId={}, messageCount={}", 
                conversationId, messages != null ? messages.size() : 0);
        
        if (messages == null || messages.isEmpty()) {
            return;
        }
        
        List<ChatMessage> chatMessages = new ArrayList<>();
        for (Map<String, Object> msg : messages) {
            ChatMessage message = new ChatMessage()
                    .setId(String.valueOf(msg.get("id")))
                    .setConversationId(conversationId)
                    .setConversationType(ChatMessage.CONV_TYPE_AGENT)
                    .setSenderId(String.valueOf(msg.get("senderId")))
                    .setSenderType(((Number) msg.get("senderType")).intValue())
                    .setSenderName(String.valueOf(msg.get("senderName")))
                    .setContent(String.valueOf(msg.get("content")))
                    .setMsgType(msg.get("msgType") != null ? ((Number) msg.get("msgType")).intValue() : 0)
                    .setCreateTime(parseDate(msg.get("createTime")))
                    .setDeleted(false);
            
            chatMessages.add(message);
        }
        
        if (!chatMessages.isEmpty()) {
            messageRepository.saveAll(chatMessages);
        }
    }

    // ==================== 私有方法 ====================

    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        if (content.length() <= maxLength) return content;
        return content.substring(0, maxLength) + "...";
    }

    private Date parseDate(Object dateObj) {
        if (dateObj == null) return new Date();
        if (dateObj instanceof Date) return (Date) dateObj;
        if (dateObj instanceof Long) return new Date((Long) dateObj);
        if (dateObj instanceof String) {
            try {
                return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse((String) dateObj);
            } catch (Exception e) {
                return new Date();
            }
        }
        return new Date();
    }
}
