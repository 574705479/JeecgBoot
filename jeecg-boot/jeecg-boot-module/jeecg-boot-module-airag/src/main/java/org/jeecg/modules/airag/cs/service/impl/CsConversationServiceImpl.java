package org.jeecg.modules.airag.cs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.common.util.oConvertUtils;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsCollaborator;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.mapper.CsCollaboratorMapper;
import org.jeecg.modules.airag.cs.mapper.CsConversationMapper;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsConversationService;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketMessage;
import org.jeecg.modules.airag.cs.websocket.CsWebSocketSessionManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * 会话管理服务实现 (重构版)
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Service
public class CsConversationServiceImpl extends ServiceImpl<CsConversationMapper, CsConversation> 
        implements ICsConversationService {

    @Autowired
    @Lazy
    private ICsAgentService agentService;

    @Autowired
    private CsCollaboratorMapper collaboratorMapper;

    @Autowired
    private CsWebSocketSessionManager sessionManager;

    // ==================== 会话生命周期 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsConversation createConversation(String appId, String userId, String userName, String source) {
        CsConversation conversation = new CsConversation();
        conversation.setAppId(appId);
        conversation.setUserId(userId);
        conversation.setUserName(oConvertUtils.isNotEmpty(userName) ? userName : "访客");
        conversation.setSource(source);
        conversation.setStatus(CsConversation.STATUS_UNASSIGNED);
        conversation.setReplyMode(CsConversation.REPLY_MODE_AI_AUTO);
        conversation.setUnreadCount(0);
        conversation.setMessageCount(0);
        conversation.setCreateTime(new Date());
        conversation.setLastMessageTime(new Date());
        
        save(conversation);
        log.info("[CS-Conversation] 创建会话: id={}, userId={}", conversation.getId(), userId);
        
        // ★ 广播新会话给所有在线客服
        broadcastNewConversation(conversation);
        
        return conversation;
    }
    
    /**
     * 广播新会话给所有在线客服
     */
    private void broadcastNewConversation(CsConversation conversation) {
        try {
            java.util.Map<String, Object> extra = new java.util.HashMap<>();
            extra.put("appId", conversation.getAppId());
            extra.put("userName", conversation.getUserName());
            extra.put("createTime", conversation.getCreateTime());
            extra.put("status", conversation.getStatus());
            extra.put("replyMode", conversation.getReplyMode());
            
            CsWebSocketMessage notification = CsWebSocketMessage.builder()
                    .type("new_conversation")
                    .conversationId(conversation.getId())
                    .senderId(conversation.getUserId())
                    .senderName(conversation.getUserName())
                    .content("新会话")
                    .extra(extra)
                    .build();
            sessionManager.sendToAllAgents(notification);
            log.info("[CS-Conversation] 广播新会话给所有客服: conversationId={}", conversation.getId());
        } catch (Exception e) {
            log.warn("[CS-Conversation] 广播新会话失败: {}", e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CsConversation getOrCreateConversation(String conversationId, String appId, String userId, String userName) {
        // 如果指定了conversationId，先尝试查找
        if (oConvertUtils.isNotEmpty(conversationId)) {
            CsConversation existing = getById(conversationId);
            if (existing != null) {
                return existing;
            }
            
            // 不存在则创建（使用指定的ID）
            CsConversation conversation = new CsConversation();
            conversation.setId(conversationId);
            conversation.setAppId(appId);
            conversation.setUserId(userId);
            conversation.setUserName(oConvertUtils.isNotEmpty(userName) ? userName : "访客");
            conversation.setStatus(CsConversation.STATUS_UNASSIGNED);
            conversation.setReplyMode(CsConversation.REPLY_MODE_AI_AUTO);
            conversation.setUnreadCount(0);
            conversation.setMessageCount(0);
            conversation.setCreateTime(new Date());
            conversation.setLastMessageTime(new Date());
            
            save(conversation);
            log.info("[CS-Conversation] 创建会话(指定ID): id={}, userId={}", conversationId, userId);
            
            // ★ 广播新会话给所有在线客服
            broadcastNewConversation(conversation);
            
            return conversation;
        }
        
        // 没有指定ID，查找用户的活跃会话
        CsConversation active = getActiveConversation(userId, appId);
        if (active != null) {
            return active;
        }
        
        // 创建新会话 (createConversation内部会广播)
        return createConversation(appId, userId, userName, null);
    }

    @Override
    public CsConversation getConversation(String conversationId) {
        if (oConvertUtils.isEmpty(conversationId)) {
            return null;
        }
        
        CsConversation conversation = getById(conversationId);
        if (conversation != null) {
            // 加载协作者列表
            List<CsCollaborator> collaborators = collaboratorMapper.selectActiveCollaborators(conversationId);
            conversation.setCollaborators(collaborators);
            
            // 加载主负责客服信息
            if (oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())) {
                CsAgent agent = agentService.getById(conversation.getOwnerAgentId());
                if (agent != null) {
                    conversation.setOwnerAgentName(agent.getNickname());
                    conversation.setOwnerAgentAvatar(agent.getAvatar());
                }
            }
        }
        
        return conversation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean assignToAgent(String conversationId, String agentId) {
        log.info("[CS-Conversation] 客服接入会话: conversationId={}, agentId={}", conversationId, agentId);
        
        CsConversation conversation = getById(conversationId);
        if (conversation == null) {
            // 会话不存在，创建新会话
            conversation = new CsConversation();
            conversation.setId(conversationId);
            conversation.setUserId(conversationId);
            conversation.setUserName("访客");
            conversation.setStatus(CsConversation.STATUS_UNASSIGNED);
            conversation.setReplyMode(CsConversation.REPLY_MODE_AI_AUTO);
            conversation.setUnreadCount(0);
            conversation.setMessageCount(0);
            conversation.setCreateTime(new Date());
            save(conversation);
        }
        
        // 检查客服状态
        CsAgent agent = agentService.getById(agentId);
        if (agent == null) {
            log.warn("[CS-Conversation] 客服不存在: agentId={}", agentId);
            return false;
        }
        
        // 如果客服离线，自动上线
        if (agent.getStatus() != CsAgent.STATUS_ONLINE) {
            agentService.goOnline(agentId);
        }
        
        // 检查是否已被其他客服接入
        if (conversation.getStatus() == CsConversation.STATUS_ASSIGNED 
                && oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())
                && !conversation.getOwnerAgentId().equals(agentId)) {
            log.warn("[CS-Conversation] 会话已被其他客服接入: conversationId={}", conversationId);
            return false;
        }
        
        // 更新会话状态
        conversation.setOwnerAgentId(agentId);
        conversation.setStatus(CsConversation.STATUS_ASSIGNED);
        conversation.setAssignTime(new Date());
        conversation.setUpdateTime(new Date());
        // 默认切换为AI辅助模式（客服可以看到AI建议）
        conversation.setReplyMode(CsConversation.REPLY_MODE_AI_ASSIST);
        updateById(conversation);
        
        // 创建协作者记录（主负责人）
        CsCollaborator collaborator = new CsCollaborator();
        collaborator.setConversationId(conversationId);
        collaborator.setAgentId(agentId);
        collaborator.setRole(CsCollaborator.ROLE_OWNER);
        collaborator.setJoinTime(new Date());
        collaboratorMapper.insert(collaborator);
        
        // 更新客服会话数
        agentService.incrementSessions(agentId);
        
        // 通知用户客服已接入
        notifyUser(conversationId, "agent_connected", "客服 " + agent.getNickname() + " 为您服务");
        
        log.info("[CS-Conversation] 客服接入成功: conversationId={}, agentId={}", conversationId, agentId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeConversation(String conversationId) {
        log.info("[CS-Conversation] 结束会话: conversationId={}", conversationId);
        
        CsConversation conversation = getById(conversationId);
        if (conversation == null) {
            return;
        }
        
        // 更新会话状态
        conversation.setStatus(CsConversation.STATUS_CLOSED);
        conversation.setEndTime(new Date());
        conversation.setUpdateTime(new Date());
        updateById(conversation);
        
        // 更新所有协作者的离开时间
        LambdaUpdateWrapper<CsCollaborator> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsCollaborator::getConversationId, conversationId)
                .isNull(CsCollaborator::getLeaveTime)
                .set(CsCollaborator::getLeaveTime, new Date());
        collaboratorMapper.update(null, updateWrapper);
        
        // 减少客服会话数
        if (oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())) {
            agentService.decrementSessions(conversation.getOwnerAgentId());
        }
        
        // 通知用户会话已结束
        notifyUser(conversationId, "conversation_closed", "会话已结束");
        
        // 通知所有相关客服
        notifyAgents(conversationId, "conversation_closed", "会话已结束");
    }

    // ==================== 回复模式管理 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean changeReplyMode(String conversationId, int replyMode) {
        log.info("[CS-Conversation] 切换回复模式: conversationId={}, replyMode={}", conversationId, replyMode);
        
        if (replyMode < 0 || replyMode > 2) {
            return false;
        }
        
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getReplyMode, replyMode)
                .set(CsConversation::getUpdateTime, new Date());
        
        boolean success = update(updateWrapper);
        
        if (success) {
            // 通知所有相关客服模式已切换
            String modeName = replyMode == 0 ? "AI自动" : (replyMode == 1 ? "手动" : "AI辅助");
            notifyAgents(conversationId, "mode_changed", "回复模式已切换为: " + modeName);
        }
        
        return success;
    }

    @Override
    public int getReplyMode(String conversationId) {
        CsConversation conversation = getById(conversationId);
        if (conversation == null) {
            return CsConversation.REPLY_MODE_AI_AUTO;
        }
        return conversation.getReplyMode() != null ? conversation.getReplyMode() : CsConversation.REPLY_MODE_AI_AUTO;
    }

    // ==================== 会话移交 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean transferTo(String conversationId, String toAgentId, String fromAgentId) {
        log.info("[CS-Conversation] 移交会话: conversationId={}, from={}, to={}", 
                conversationId, fromAgentId, toAgentId);
        
        CsConversation conversation = getById(conversationId);
        if (conversation == null) {
            return false;
        }
        
        // 检查目标客服
        CsAgent toAgent = agentService.getById(toAgentId);
        if (toAgent == null) {
            log.warn("[CS-Conversation] 目标客服不存在: agentId={}", toAgentId);
            return false;
        }
        
        // 如果目标客服离线，自动上线
        if (toAgent.getStatus() != CsAgent.STATUS_ONLINE) {
            agentService.goOnline(toAgentId);
        }
        
        // 更新原负责人的协作记录
        if (oConvertUtils.isNotEmpty(fromAgentId)) {
            LambdaUpdateWrapper<CsCollaborator> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(CsCollaborator::getConversationId, conversationId)
                    .eq(CsCollaborator::getAgentId, fromAgentId)
                    .isNull(CsCollaborator::getLeaveTime)
                    .set(CsCollaborator::getLeaveTime, new Date())
                    .set(CsCollaborator::getRole, CsCollaborator.ROLE_COLLABORATOR);
            collaboratorMapper.update(null, updateWrapper);
            
            // 减少原客服会话数
            agentService.decrementSessions(fromAgentId);
        }
        
        // 更新会话
        conversation.setOwnerAgentId(toAgentId);
        conversation.setUpdateTime(new Date());
        updateById(conversation);
        
        // 创建新负责人的协作记录
        CsCollaborator collaborator = new CsCollaborator();
        collaborator.setConversationId(conversationId);
        collaborator.setAgentId(toAgentId);
        collaborator.setRole(CsCollaborator.ROLE_OWNER);
        collaborator.setJoinTime(new Date());
        collaborator.setInviteBy(fromAgentId);
        collaboratorMapper.insert(collaborator);
        
        // 增加新客服会话数
        agentService.incrementSessions(toAgentId);
        
        // 通知相关人员
        CsAgent fromAgent = fromAgentId != null ? agentService.getById(fromAgentId) : null;
        String fromName = fromAgent != null ? fromAgent.getNickname() : "系统";
        
        notifyAgents(conversationId, "transfer", 
                "会话已从 " + fromName + " 移交给 " + toAgent.getNickname());
        notifyUser(conversationId, "agent_changed", 
                "客服 " + toAgent.getNickname() + " 继续为您服务");
        
        return true;
    }

    // ==================== 查询接口 ====================

    @Override
    public IPage<CsConversation> getConversationList(Page<CsConversation> page, String agentId, 
                                                      Integer status, String filter) {
        IPage<CsConversation> result = baseMapper.selectConversationPage(page, agentId, status, filter);
        
        // 补充协作者信息
        for (CsConversation conv : result.getRecords()) {
            List<CsCollaborator> collaborators = collaboratorMapper.selectActiveCollaborators(conv.getId());
            conv.setCollaborators(collaborators);
        }
        
        return result;
    }

    @Override
    public List<CsConversation> getMyConversations(String agentId) {
        return baseMapper.selectByOwnerAgent(agentId);
    }

    @Override
    public List<CsConversation> getUnassignedConversations(int limit) {
        return baseMapper.selectUnassigned(limit);
    }

    @Override
    public CsConversation getActiveConversation(String userId, String appId) {
        LambdaQueryWrapper<CsConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CsConversation::getUserId, userId)
                .ne(CsConversation::getStatus, CsConversation.STATUS_CLOSED)
                .orderByDesc(CsConversation::getCreateTime)
                .last("LIMIT 1");
        
        if (oConvertUtils.isNotEmpty(appId)) {
            wrapper.eq(CsConversation::getAppId, appId);
        }
        
        return getOne(wrapper);
    }

    // ==================== 消息相关 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLastMessage(String conversationId, String message) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getLastMessage, 
                        message != null && message.length() > 100 ? message.substring(0, 100) + "..." : message)
                .set(CsConversation::getLastMessageTime, new Date())
                .setSql("message_count = IFNULL(message_count, 0) + 1");
        update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUnread(String conversationId) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .setSql("unread_count = IFNULL(unread_count, 0) + 1");
        update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void clearUnread(String conversationId) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getUnreadCount, 0);
        update(updateWrapper);
    }

    // ==================== 评价 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rateConversation(String conversationId, Integer satisfaction, String comment) {
        LambdaUpdateWrapper<CsConversation> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsConversation::getId, conversationId)
                .set(CsConversation::getSatisfaction, satisfaction)
                .set(CsConversation::getSatisfactionComment, comment)
                .set(CsConversation::getUpdateTime, new Date());
        update(updateWrapper);
        
        log.info("[CS-Conversation] 会话评价: conversationId={}, satisfaction={}", conversationId, satisfaction);
    }

    // ==================== 通知 ====================

    @Override
    public void notifyUser(String conversationId, String type, String content) {
        CsConversation conversation = getById(conversationId);
        String userId = conversation != null ? conversation.getUserId() : conversationId;
        
        CsWebSocketMessage message = CsWebSocketMessage.builder()
                .type(type)
                .conversationId(conversationId)
                .content(content)
                .build();
        
        sessionManager.sendToUserByConversation(conversationId, userId, message);
    }

    @Override
    public void notifyAgents(String conversationId, String type, String content) {
        CsWebSocketMessage message = CsWebSocketMessage.builder()
                .type(type)
                .conversationId(conversationId)
                .content(content)
                .build();
        
        // 获取所有相关客服ID
        CsConversation conversation = getById(conversationId);
        if (conversation != null && oConvertUtils.isNotEmpty(conversation.getOwnerAgentId())) {
            sessionManager.sendToAgent(conversation.getOwnerAgentId(), message);
        }
        
        // 通知所有协作者
        List<CsCollaborator> collaborators = collaboratorMapper.selectActiveCollaborators(conversationId);
        for (CsCollaborator collab : collaborators) {
            if (!collab.getAgentId().equals(conversation != null ? conversation.getOwnerAgentId() : null)) {
                sessionManager.sendToAgent(collab.getAgentId(), message);
            }
        }
    }
}
