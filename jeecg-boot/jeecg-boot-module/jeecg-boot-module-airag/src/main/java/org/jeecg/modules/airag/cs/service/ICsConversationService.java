package org.jeecg.modules.airag.cs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.cs.entity.CsConversation;

import java.util.List;

/**
 * 会话管理服务接口 (重构版)
 * 
 * 核心功能:
 * 1. 会话生命周期管理 (创建/接入/结束)
 * 2. 回复模式切换 (AI自动/手动/AI辅助)
 * 3. 会话移交
 * 
 * @author jeecg
 * @date 2026-01-12
 */
public interface ICsConversationService extends IService<CsConversation> {

    // ==================== 会话生命周期 ====================

    /**
     * 创建会话 (用户发起对话时调用)
     * 
     * @param appId    应用ID
     * @param userId   用户ID
     * @param userName 用户名称
     * @param source   来源渠道
     * @return 会话
     */
    CsConversation createConversation(String appId, String userId, String userName, String source);

    /**
     * 获取或创建会话 (用户发消息时调用)
     * 
     * @param conversationId 会话ID (可以是用户指定的，如uuid)
     * @param appId          应用ID
     * @param userId         用户ID
     * @param userName       用户名称
     * @return 会话
     */
    CsConversation getOrCreateConversation(String conversationId, String appId, String userId, String userName);

    /**
     * 获取会话
     * 
     * @param conversationId 会话ID
     * @return 会话
     */
    CsConversation getConversation(String conversationId);

    /**
     * 客服接入会话
     * 
     * @param conversationId 会话ID
     * @param agentId        客服ID
     * @return 是否成功
     */
    boolean assignToAgent(String conversationId, String agentId);

    /**
     * 结束会话
     * 
     * @param conversationId 会话ID
     */
    void closeConversation(String conversationId);

    // ==================== 回复模式管理 ====================

    /**
     * 切换回复模式
     * 
     * @param conversationId 会话ID
     * @param replyMode      回复模式: 0-AI自动 1-手动 2-AI辅助
     * @return 是否成功
     */
    boolean changeReplyMode(String conversationId, int replyMode);

    /**
     * 获取当前回复模式
     * 
     * @param conversationId 会话ID
     * @return 回复模式
     */
    int getReplyMode(String conversationId);

    // ==================== 会话移交 ====================

    /**
     * 移交会话给其他客服
     * 
     * @param conversationId 会话ID
     * @param toAgentId      目标客服ID
     * @param fromAgentId    原客服ID
     * @return 是否成功
     */
    boolean transferTo(String conversationId, String toAgentId, String fromAgentId);

    // ==================== 查询接口 ====================

    /**
     * 分页查询会话列表
     * 
     * @param page    分页参数
     * @param agentId 客服ID
     * @param status  状态 (可选)
     * @param filter  筛选类型: mine-我负责的, collab-协作中, unassigned-未分配, all-全部
     * @return 会话列表
     */
    IPage<CsConversation> getConversationList(Page<CsConversation> page, String agentId, 
                                               Integer status, String filter);

    /**
     * 获取客服负责的会话列表
     * 
     * @param agentId 客服ID
     * @return 会话列表
     */
    List<CsConversation> getMyConversations(String agentId);

    /**
     * 获取未分配的会话列表
     * 
     * @param limit 限制数量
     * @return 会话列表
     */
    List<CsConversation> getUnassignedConversations(int limit);

    /**
     * 获取用户的活跃会话
     * 
     * @param userId 用户ID
     * @param appId  应用ID
     * @return 活跃会话
     */
    CsConversation getActiveConversation(String userId, String appId);

    // ==================== 消息相关 ====================

    /**
     * 更新最后消息
     * 
     * @param conversationId 会话ID
     * @param message        消息内容
     */
    void updateLastMessage(String conversationId, String message);

    /**
     * 增加未读消息数
     * 
     * @param conversationId 会话ID
     */
    void incrementUnread(String conversationId);

    /**
     * 清除未读消息数
     * 
     * @param conversationId 会话ID
     */
    void clearUnread(String conversationId);

    // ==================== 评价 ====================

    /**
     * 评价会话
     * 
     * @param conversationId 会话ID
     * @param satisfaction   满意度 (1-5)
     * @param comment        评价内容
     */
    void rateConversation(String conversationId, Integer satisfaction, String comment);

    // ==================== 通知 ====================

    /**
     * 通知用户
     * 
     * @param conversationId 会话ID
     * @param type           消息类型
     * @param content        消息内容
     */
    void notifyUser(String conversationId, String type, String content);

    /**
     * 通知所有相关客服 (主负责人 + 协作者)
     * 
     * @param conversationId 会话ID
     * @param type           消息类型
     * @param content        消息内容
     */
    void notifyAgents(String conversationId, String type, String content);
}
