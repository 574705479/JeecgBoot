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
     * 用户发送消息（带附件支持）
     */
    CsMessage sendUserMessage(String conversationId, String userId, String userName, String content,
                              Integer msgType, String extra);

    /**
     * 用户发送消息（仅保存+推送，不触发AI回复）
     * 用于FAQ等场景，由预设答案代替AI回复
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @param userName       用户名称
     * @param content        消息内容
     * @return 用户消息
     */
    CsMessage sendUserMessageRaw(String conversationId, String userId, String userName, String content);

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
     * 发送访客端开场白（AI消息，来自全局访客AI应用）
     *
     * @param conversationId 会话ID
     * @return 开场白消息
     */
    CsMessage sendVisitorPrologue(String conversationId);

    /**
     * 发送自动消息（AI关闭时客服自动欢迎语）
     *
     * @param conversationId 会话ID
     * @param agentId        客服ID
     * @param agentName      客服名称
     * @param userLang       用户浏览器语言
     */
    void sendAutoMessages(String conversationId, String agentId, String agentName, String userLang);

    /**
     * 发送自动消息作为会话欢迎消息（客服身份，但不切换会话回复模式）
     *
     * @param conversationId 会话ID
     * @param agentId        客服ID
     * @param agentName      客服名称
     * @param userLang       用户浏览器语言
     */
    void sendVisitorAutoMessagesAsAgent(String conversationId, String agentId, String agentName, String userLang);

    /**
     * 以系统身份("智能助手")发送自动消息，用于无客服分配时
     * @param conversationId 会话ID
     * @param userLang       用户浏览器语言
     */
    void sendAutoMessagesAsSystem(String conversationId, String userLang);

    /**
     * 发送智能助手消息 (senderType=4)
     * @param conversationId 会话ID
     * @param content        消息文本
     * @param faqExtraJson   FAQ扩展JSON（可为null）
     * @return 智能助手消息
     */
    CsMessage sendSmartAssistantMessage(String conversationId, String content, String faqExtraJson);

    /**
     * 发送初始FAQ列表消息（会话创建后的欢迎FAQ）
     * @param conversationId 会话ID
     */
    void sendInitialFaqMessage(String conversationId);

    /**
     * 处理FAQ交互（点击/返回第一层/返回上一层）
     * @param conversationId 会话ID
     * @param action         操作类型: click/top/back
     * @param faqIndex       FAQ索引（action=click时使用）
     * @param parentPath     路径数组，定位当前展示的FAQ层级位置
     */
    void handleFaqInteract(String conversationId, String action, Integer faqIndex, java.util.List<Integer> parentPath);

    /**
     * 发送消息（通用）
     * 
     * @param message 消息实体
     * @return 发送后的消息
     */
    CsMessage sendMessage(CsMessage message);

    // ==================== AI相关 ====================

    /**
     * 取消正在进行的AI流式回复
     * 设置取消标记，后续token将不再推送，已生成的部分内容会被保存
     *
     * @param conversationId 会话ID
     */
    void cancelAiStream(String conversationId);

    /**
     * 取消正在进行的AI建议流式推送
     *
     * @param conversationId 会话ID
     */
    void cancelAiSuggestionStream(String conversationId);

    /**
     * 生成AI建议回复 (AI辅助模式)
     * 
     * @param conversationId 会话ID
     * @param userMessage    用户消息
     * @return AI建议内容
     */
    String generateAiSuggestion(String conversationId, String userMessage);

    /**
     * 生成AI建议回复 (AI辅助模式) - 指定请求客服
     *
     * @param conversationId 会话ID
     * @param userMessage    用户消息
     * @param agentId        请求客服ID（会话未分配时用于推送）
     * @return AI建议内容
     */
    String generateAiSuggestion(String conversationId, String userMessage, String agentId);

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

    // ==================== 消息撤回 ====================

    /**
     * 撤回消息
     *
     * @param messageId 消息ID
     * @param agentId   操作客服ID
     * @return 是否成功
     */
    boolean recallMessage(String messageId, String agentId);

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
