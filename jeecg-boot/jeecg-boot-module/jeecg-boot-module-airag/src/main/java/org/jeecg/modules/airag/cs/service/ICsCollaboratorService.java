package org.jeecg.modules.airag.cs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import org.jeecg.modules.airag.cs.entity.CsCollaborator;

import java.util.List;

/**
 * 会话协作者服务接口
 * 
 * 支持多客服协作同一会话:
 * - 邀请协作: 主负责人邀请其他客服加入
 * - 主动加入: 客服主动加入某个会话
 * - 退出协作: 协作者离开会话
 * 
 * @author jeecg
 * @date 2026-01-12
 */
public interface ICsCollaboratorService extends IService<CsCollaborator> {

    /**
     * 邀请客服协作
     * 
     * @param conversationId 会话ID
     * @param agentId        被邀请的客服ID
     * @param inviteBy       邀请人ID
     * @return 是否成功
     */
    boolean inviteCollaborator(String conversationId, String agentId, String inviteBy);

    /**
     * 客服主动加入会话
     * 
     * @param conversationId 会话ID
     * @param agentId        客服ID
     * @param asOwner        是否作为主负责人加入（仅当会话未分配时有效）
     * @return 是否成功
     */
    boolean joinConversation(String conversationId, String agentId, boolean asOwner);

    /**
     * 退出协作
     * 
     * @param conversationId 会话ID
     * @param agentId        客服ID
     * @return 是否成功
     */
    boolean leaveConversation(String conversationId, String agentId);

    /**
     * 获取会话的活跃协作者列表
     * 
     * @param conversationId 会话ID
     * @return 协作者列表
     */
    List<CsCollaborator> getCollaborators(String conversationId);

    /**
     * 获取客服参与的会话ID列表
     * 
     * @param agentId 客服ID
     * @return 会话ID列表
     */
    List<String> getConversationIdsByAgent(String agentId);

    /**
     * 检查客服是否在会话中
     * 
     * @param conversationId 会话ID
     * @param agentId        客服ID
     * @return 是否在协作中
     */
    boolean isInConversation(String conversationId, String agentId);

    /**
     * 获取客服在会话中的角色
     * 
     * @param conversationId 会话ID
     * @param agentId        客服ID
     * @return 角色 (null表示不在会话中)
     */
    Integer getRole(String conversationId, String agentId);
}
