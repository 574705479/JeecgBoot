package org.jeecg.modules.airag.cs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.jeecg.modules.airag.cs.entity.CsAgent;
import org.jeecg.modules.airag.cs.entity.CsCollaborator;
import org.jeecg.modules.airag.cs.entity.CsConversation;
import org.jeecg.modules.airag.cs.mapper.CsCollaboratorMapper;
import org.jeecg.modules.airag.cs.service.ICsAgentService;
import org.jeecg.modules.airag.cs.service.ICsCollaboratorService;
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
 * 会话协作者服务实现
 * 
 * @author jeecg
 * @date 2026-01-12
 */
@Slf4j
@Service
public class CsCollaboratorServiceImpl extends ServiceImpl<CsCollaboratorMapper, CsCollaborator> 
        implements ICsCollaboratorService {

    @Autowired
    @Lazy
    private ICsConversationService conversationService;

    @Autowired
    @Lazy
    private ICsAgentService agentService;

    @Autowired
    private CsWebSocketSessionManager sessionManager;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean inviteCollaborator(String conversationId, String agentId, String inviteBy) {
        log.info("[CS-Collaborator] 邀请协作: conversationId={}, agentId={}, inviteBy={}", 
                conversationId, agentId, inviteBy);
        
        // 检查是否已在协作中
        if (isInConversation(conversationId, agentId)) {
            log.warn("[CS-Collaborator] 客服已在协作中: conversationId={}, agentId={}", conversationId, agentId);
            return false;
        }
        
        // 检查客服是否存在
        CsAgent agent = agentService.getById(agentId);
        if (agent == null) {
            log.warn("[CS-Collaborator] 客服不存在: agentId={}", agentId);
            return false;
        }
        
        // 创建协作记录
        CsCollaborator collaborator = new CsCollaborator();
        collaborator.setConversationId(conversationId);
        collaborator.setAgentId(agentId);
        collaborator.setRole(CsCollaborator.ROLE_COLLABORATOR);
        collaborator.setJoinTime(new Date());
        collaborator.setInviteBy(inviteBy);
        save(collaborator);
        
        // 通知被邀请的客服
        CsAgent inviter = inviteBy != null ? agentService.getById(inviteBy) : null;
        String inviterName = inviter != null ? inviter.getNickname() : "系统";
        
        CsWebSocketMessage notification = CsWebSocketMessage.builder()
                .type("invite_collab")
                .conversationId(conversationId)
                .content(inviterName + " 邀请您协作处理会话")
                .senderId(inviteBy)
                .senderName(inviterName)
                .build();
        sessionManager.sendToAgent(agentId, notification);
        
        // 通知会话中的其他客服
        conversationService.notifyAgents(conversationId, "collab_joined", 
                agent.getNickname() + " 加入协作");
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean joinConversation(String conversationId, String agentId, boolean asOwner) {
        log.info("[CS-Collaborator] 加入会话: conversationId={}, agentId={}, asOwner={}", 
                conversationId, agentId, asOwner);
        
        // 检查是否已在协作中
        if (isInConversation(conversationId, agentId)) {
            log.warn("[CS-Collaborator] 客服已在协作中: conversationId={}, agentId={}", conversationId, agentId);
            return false;
        }
        
        // 检查客服
        CsAgent agent = agentService.getById(agentId);
        if (agent == null) {
            log.warn("[CS-Collaborator] 客服不存在: agentId={}", agentId);
            return false;
        }
        
        // 如果客服离线，自动上线
        if (agent.getStatus() != CsAgent.STATUS_ONLINE) {
            agentService.goOnline(agentId);
        }
        
        CsConversation conversation = conversationService.getConversation(conversationId);
        
        // 如果会话未分配且希望作为负责人加入
        if (asOwner && (conversation == null || conversation.getStatus() == CsConversation.STATUS_UNASSIGNED)) {
            return conversationService.assignToAgent(conversationId, agentId);
        }
        
        // 作为协作者加入
        CsCollaborator collaborator = new CsCollaborator();
        collaborator.setConversationId(conversationId);
        collaborator.setAgentId(agentId);
        collaborator.setRole(CsCollaborator.ROLE_COLLABORATOR);
        collaborator.setJoinTime(new Date());
        save(collaborator);
        
        // 通知会话中的其他客服
        conversationService.notifyAgents(conversationId, "collab_joined", 
                agent.getNickname() + " 加入协作");
        
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean leaveConversation(String conversationId, String agentId) {
        log.info("[CS-Collaborator] 退出协作: conversationId={}, agentId={}", conversationId, agentId);
        
        // 检查角色
        Integer role = getRole(conversationId, agentId);
        if (role == null) {
            log.warn("[CS-Collaborator] 客服不在会话中: conversationId={}, agentId={}", conversationId, agentId);
            return false;
        }
        
        // 如果是主负责人，不能直接退出，需要移交
        if (role == CsCollaborator.ROLE_OWNER) {
            log.warn("[CS-Collaborator] 主负责人不能直接退出，请先移交会话: conversationId={}, agentId={}", 
                    conversationId, agentId);
            return false;
        }
        
        // 更新离开时间
        LambdaUpdateWrapper<CsCollaborator> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(CsCollaborator::getConversationId, conversationId)
                .eq(CsCollaborator::getAgentId, agentId)
                .isNull(CsCollaborator::getLeaveTime)
                .set(CsCollaborator::getLeaveTime, new Date());
        update(updateWrapper);
        
        // 通知其他客服
        CsAgent agent = agentService.getById(agentId);
        String agentName = agent != null ? agent.getNickname() : "客服";
        conversationService.notifyAgents(conversationId, "collab_left", agentName + " 退出协作");
        
        return true;
    }

    @Override
    public List<CsCollaborator> getCollaborators(String conversationId) {
        return baseMapper.selectActiveCollaborators(conversationId);
    }

    @Override
    public List<String> getConversationIdsByAgent(String agentId) {
        return baseMapper.selectConversationIdsByAgent(agentId);
    }

    @Override
    public boolean isInConversation(String conversationId, String agentId) {
        // 检查是否是主负责人
        CsConversation conversation = conversationService.getById(conversationId);
        if (conversation != null && agentId.equals(conversation.getOwnerAgentId())) {
            return true;
        }
        
        // 检查协作者表
        return baseMapper.countActiveByConvAndAgent(conversationId, agentId) > 0;
    }

    @Override
    public Integer getRole(String conversationId, String agentId) {
        // 检查是否是主负责人
        CsConversation conversation = conversationService.getById(conversationId);
        if (conversation != null && agentId.equals(conversation.getOwnerAgentId())) {
            return CsCollaborator.ROLE_OWNER;
        }
        
        // 查询协作者表
        LambdaQueryWrapper<CsCollaborator> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(CsCollaborator::getConversationId, conversationId)
                .eq(CsCollaborator::getAgentId, agentId)
                .isNull(CsCollaborator::getLeaveTime);
        CsCollaborator collaborator = getOne(queryWrapper);
        
        return collaborator != null ? collaborator.getRole() : null;
    }
}
