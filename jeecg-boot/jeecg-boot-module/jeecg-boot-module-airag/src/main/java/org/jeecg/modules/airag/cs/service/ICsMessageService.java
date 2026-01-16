package org.jeecg.modules.airag.cs.service;

import org.jeecg.modules.airag.cs.entity.CsMessage;

import java.util.List;

/**
 * 消息服务接口 (重构版)
 * 
 * 核心功能:
 * 1. 消息发送与存储（MongoDB）
 * 2. AI回复生成与确认
 * 3. 消息推送给相关方
 * 
 * @author jeecg
 * @date 2026-01-12
 */
public interface ICsMessageService {

    // ==================== 消息发送 ====================

    /**
     * 用户发送消息
     * 根据会话的回复模式自动处理:
     * - AI自动: 调用AI生成回复并发送
     * - AI辅助: 调用AI生成建议推送给客服
     * - 手动: 只保存消息等待客服回复
     * 
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @param userName       用户名称
     * @param content        消息内容
     * @return 用户消息
     */
    CsMessage sendUserMessage(String conversationId, String userId, String userName, String content);

    /**
     * 客服发送消息
     * 
     * @param conversationId 会话ID
     * @param agentId        客服ID
     * @param agentName      客服名称
     * @param content        消息内容
     * @return 客服消息
     */
    CsMessage sendAgentMessage(String conversationId, String agentId, String agentName, String content,
                               Integer msgType, String extra);

    /**
     * 发送系统消息
     * 
     * @param conversationId 会话ID
     * @param content        消息内容
     * @return 系统消息
     */
    CsMessage sendSystemMessage(String conversationId, String content);

    /**
     * 发送系统消息（可控制是否持久化）
     * 
     * @param conversationId 会话ID
     * @param content        消息内容
     * @param persist        是否持久化到数据库
     * @return 系统消息
     */
    CsMessage sendSystemMessage(String conversationId, String content, boolean persist);

    /**
     * 发送消息（通用）
     * 
     * @param message 消息实体
     * @return 发送后的消息
     */
    CsMessage sendMessage(CsMessage message);

    // ==================== AI相关 ====================

    /**
     * 生成AI建议回复 (AI辅助模式)
     * 
     * @param conversationId 会话ID
     * @param userMessage    用户消息
     * @return AI建议内容
     */
    String generateAiSuggestion(String conversationId, String userMessage);

    /**
     * 确认并发送AI建议
     * 
     * @param conversationId 会话ID
     * @param suggestionId   建议ID
     * @param agentId        客服ID
     * @param agentName      客服名称
     * @param editedContent  编辑后的内容（如果客服修改了）
     * @return 发送的消息
     */
    CsMessage confirmAiSuggestion(String conversationId, String suggestionId, 
                                   String agentId, String agentName, String editedContent);

    /**
     * 获取当前的AI建议
     * 
     * @param conversationId 会话ID
     * @return AI建议内容
     */
    String getCurrentAiSuggestion(String conversationId);

    // ==================== 消息查询 ====================

    /**
     * 获取会话消息列表
     * 
     * @param conversationId 会话ID
     * @param limit          限制数量
     * @return 消息列表
     */
    List<CsMessage> getMessages(String conversationId, int limit);

    /**
     * 获取会话消息（分页）
     * 
     * @param conversationId 会话ID
     * @param beforeId       在此消息ID之前
     * @param limit          限制数量
     * @return 消息列表
     */
    List<CsMessage> getMessages(String conversationId, String beforeId, int limit);

    /**
     * 获取最近消息
     * 
     * @param conversationId 会话ID
     * @param limit          限制数量
     * @return 消息列表
     */
    List<CsMessage> getRecentMessages(String conversationId, int limit);

    // ==================== 已读状态 ====================

    /**
     * 标记消息已读
     * 
     * @param conversationId 会话ID
     * @param userId         用户ID
     */
    void markAsRead(String conversationId, String userId);

    /**
     * 获取未读消息数
     * 
     * @param conversationId 会话ID
     * @return 未读数
     */
    int getUnreadCount(String conversationId);
}
