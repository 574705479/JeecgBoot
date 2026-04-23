package org.jeecg.modules.airag.chat.service;

import org.jeecg.modules.airag.chat.entity.ChatMessage;

import java.util.List;
import java.util.Map;

/**
 * 统一聊天消息服务接口
 *
 * @author jeecg
 * @date 2026-01-08
 */
public interface IChatMessageService {

    // ==================== 消息保存 ====================

    /**
     * 保存消息
     */
    ChatMessage saveMessage(ChatMessage message);

    /**
     * 保存AI用户消息
     */
    ChatMessage saveAiUserMessage(String conversationId, String appId, String userId, 
                                   String externalUserId, String externalUserName,
                                   String content, List<String> images);

    /**
     * 保存AI用户消息（带访客上下文）— 用于在 RAG / SSE 异步链路中透传 IP/UA/deviceId/userLang，
     * 避免 cs_conversation 因「未知访客」而展示空白。
     */
    ChatMessage saveAiUserMessage(String conversationId, String appId, String userId,
                                   String externalUserId, String externalUserName,
                                   String content, List<String> images,
                                   String userIp, String userAgent, String deviceId, String userLang);

    /**
     * 保存AI回复消息
     */
    ChatMessage saveAiAssistantMessage(String conversationId, String appId, 
                                        String content, String modelName, 
                                        Integer tokenUsage, String referenceKnowledge);

    /**
     * 保存客服消息
     */
    ChatMessage saveAgentMessage(String conversationId, String agentId, 
                                  String agentName, String agentAvatar, String content);

    /**
     * 保存用户给客服的消息
     */
    ChatMessage saveUserToAgentMessage(String conversationId, String userId,
                                        String userName, String content);

    /**
     * 保存系统消息
     */
    ChatMessage saveSystemMessage(String conversationId, String content);

    // ==================== 消息查询 ====================

    /**
     * 获取会话的所有消息
     */
    List<ChatMessage> getMessages(String conversationId);

    /**
     * 获取会话的最近N条消息
     */
    List<ChatMessage> getRecentMessages(String conversationId, int limit);

    /**
     * 获取某条消息之前的N条消息
     */
    List<ChatMessage> getMessagesBefore(String conversationId, String beforeId, int limit);

    /**
     * 获取会话的最后一条消息
     */
    ChatMessage getLastMessage(String conversationId);

    /**
     * 获取会话消息数量
     */
    long getMessageCount(String conversationId);

    // ==================== 会话管理 ====================

    /**
     * 获取用户的会话列表
     */
    List<Map<String, Object>> getUserConversations(String appId, String userId, String externalUserId);

    /**
     * 获取活跃的AI会话列表（供客服接入）
     */
    List<Map<String, Object>> getActiveAiConversations(int limit);

    /**
     * 获取会话的最后消息时间
     */
    java.util.Date getLastMessageTime(String conversationId);

    /**
     * 删除会话的所有消息
     */
    void deleteConversation(String conversationId);

    /**
     * 清空会话消息
     */
    void clearMessages(String conversationId);

    // ==================== 消息状态 ====================

    /**
     * 根据ID查找消息
     */
    ChatMessage findById(String messageId);

    /**
     * 更新消息状态（用于撤回等）
     */
    boolean updateMessageStatus(String messageId, int status);

    /**
     * 标记消息已读
     */
    void markAsRead(String conversationId, Integer senderType);

    /**
     * 获取未读消息数量
     */
    long getUnreadCount(String conversationId, Integer senderType);

    // ==================== 数据迁移 ====================

    /**
     * 从Redis迁移AI聊天记录
     */
    void migrateFromRedis(String conversationId, String appId, String userId, 
                          String externalUserId, List<Map<String, Object>> messages);

    /**
     * 从MySQL迁移客服聊天记录
     */
    void migrateFromMysql(String conversationId, List<Map<String, Object>> messages);

    // ==================== 数据清理 ====================

    /**
     * 按会话ID列表物理删除消息
     */
    long physicalDeleteByConversationIds(List<String> conversationIds, int conversationType);

    /**
     * 物理删除已软删除且超期的消息
     */
    long physicalDeleteSoftDeleted(java.util.Date deadline);
}
